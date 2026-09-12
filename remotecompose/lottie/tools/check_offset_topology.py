#!/usr/bin/env python3
"""Audit OffsetTopologyTest results and reference/RC captures; never accept stale failing tests."""

import argparse
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageStat


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet", type=Path)
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    xml = module / "build/test-results/testDebugUnitTest/TEST-com.google.android.horologist.remotecompose.lottie.OffsetTopologyTest.xml"
    methods = {
        "polygon": "polygonPointCount",
        "rounded-polygon": "roundedPolygonPointCount",
        "rounded-polygon-static-10": "roundedPolygonSevenPointControl",
        "rounded-star": "roundedStarPointCount",
        "rounded-corners": "roundedCornerActivation",
        "rounded-star-static": "roundedStarStaticControl",
        "rounded-star-static-10": "roundedStarSevenPointControl",
        "rounded-star-static-1": "roundedStarFractionalControl",
        "rounded-star-static-6": "roundedStarFivePointFourControl",
        "split-prune-static": "splitGroupStaticCrossing",
        "split-prune": "splitGroupLiveCrossing",
        "split-prune-padded-static": "paddedSplitGroupStaticCrossing",
        "split-prune-padded": "paddedSplitGroupLiveCrossing",
        "split-prune-amount": "splitGroupAnimatedAmount",
        "split-prune-amount-padded": "paddedSplitGroupAnimatedAmount",
        "split-prune-signed": "splitGroupSignedAmount",
        "split-prune-signed-padded": "paddedSplitGroupSignedAmount",
    }
    tests = {test.get("name"): test for test in ET.parse(xml).getroot().findall("testcase")} if xml.is_file() else {}
    errors, tiles = [], []
    for name, method in methods.items():
        result = tests.get(method)
        if result is None or any(result.find(tag) is not None for tag in ("failure", "error", "skipped")):
            errors.append(f"{name}: current JUnit result is missing, failed or skipped; captures are not accepted")
            continue
        folder = module / "build/outputs/lottie-motion" / ("offset-topology-" + name)
        frames = ([0] if name.endswith("-static") else
                  [int(name.rsplit("-", 1)[1])] if "-static-" in name else list(range(11)))
        worst_mean = worst_peak = 0
        for frame in frames:
            paths = [folder / f"frame{frame}-{kind}.png" for kind in ("reference", "rc")]
            if not all(path.is_file() for path in paths):
                errors.append(f"{name}/{frame}: missing capture")
                continue
            with Image.open(paths[0]) as expected, Image.open(paths[1]) as actual:
                if expected.size != actual.size:
                    errors.append(f"{name}/{frame}: dimensions differ")
                    continue
                a, b = expected.convert("RGB"), actual.convert("RGB")
                delta = ImageChops.difference(a, b).getchannel("R")
                mean = ImageStat.Stat(delta).mean[0] / 255
                peak = delta.getextrema()[1] / 255
                worst_mean, worst_peak = max(worst_mean, mean), max(worst_peak, peak)
                if sum(a.getchannel("R").histogram()[128:]) <= 20:
                    errors.append(f"{name}/{frame}: empty reference")
                if mean > .0002 or peak > .3:
                    errors.append(f"{name}/{frame}: mean={mean:.6f}, peak={peak:.4f}")
        print(f"{name}: {len(frames)} frames; worst red MAE={worst_mean:.6f}, peak={worst_peak:.4f}")
        if args.contact_sheet:
            tile = Image.new("RGB", (800, 166), "#ddd")
            draw = ImageDraw.Draw(tile)
            draw.text((4, 4), name + " (reference | RC)", fill="black")
            for column, frame in enumerate(frames if len(frames) == 1 else [0, 5, 10]):
                draw.text((column * 264 + 4, 20), f"Frame {frame}", fill="black")
                for side, kind in enumerate(("reference", "rc")):
                    path = folder / f"frame{frame}-{kind}.png"
                    if path.is_file():
                        with Image.open(path) as image:
                            thumbnail = image.convert("RGB")
                            thumbnail.thumbnail((128, 128))
                            tile.paste(thumbnail, (column * 264 + side * 132, 36))
            tiles.append(tile)
    if args.contact_sheet and tiles:
        sheet = Image.new("RGB", (800, len(tiles) * 166), "white")
        for row, tile in enumerate(tiles):
            sheet.paste(tile, (0, row * 166))
        sheet.save(args.contact_sheet)
        print(args.contact_sheet)
    if errors:
        raise SystemExit("\n".join(errors))
    print("PASS: 17 offset-topology scenarios, 117 unique frame pairs")


if __name__ == "__main__":
    main()
