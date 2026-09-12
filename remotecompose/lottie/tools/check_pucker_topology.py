#!/usr/bin/env python3
"""Audit every PuckerTopologyTest frame and optionally make reference/RC review sheets."""
import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageStat


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    cases = ["polygon", "polygon-rounded", "star", "star-rounded", "star-reversed",
             "chain", "transform", "twist", "repeater", "round-star-count",
             "round-polygon-count", "round-uneven", "round-controls"]
    errors, tiles = [], []
    for case in cases:
        folder = module / "build/outputs/lottie-motion" / ("pucker-topology-" + case)
        worst_mean = worst_peak = 0
        tile = Image.new("RGB", (800, 166), "#ddd")
        draw = ImageDraw.Draw(tile)
        draw.text((4, 4), case + " (geometry oracle | RC)", fill="black")
        for frame in range(11):
            paths = [folder / f"frame{frame}-{kind}.png" for kind in ("reference", "rc")]
            if not all(path.is_file() for path in paths):
                errors.append(f"{case}/{frame}: missing capture")
                continue
            with Image.open(paths[0]) as ref, Image.open(paths[1]) as rc:
                if ref.size != rc.size:
                    errors.append(f"{case}/{frame}: size mismatch")
                    continue
                a, b = ref.convert("RGB"), rc.convert("RGB")
                difference = ImageChops.difference(a, b)
                mean = ImageStat.Stat(difference).mean[0] / 255
                peak = difference.getextrema()[0][1] / 255
                worst_mean, worst_peak = max(worst_mean, mean), max(worst_peak, peak)
                if mean > 0.0001 or peak > 0.25:
                    errors.append(f"{case}/{frame}: mean={mean:.6f}, peak={peak:.4f}")
                for label, image in (("reference", a), ("RC", b)):
                    if sum(image.getchannel("R").histogram()[128:]) <= 20:
                        errors.append(f"{case}/{frame}: empty {label}")
                    if image.getextrema()[1][1] or image.getextrema()[2][1]:
                        errors.append(f"{case}/{frame}: unexpected non-red {label}")
                if frame in (0, 4, 10):
                    column = (0, 4, 10).index(frame)
                    draw.text((column * 264 + 4, 20), f"Frame {frame}", fill="black")
                    for side, image in enumerate((a, b)):
                        image.thumbnail((128, 128))
                        tile.paste(image, (column * 264 + side * 132, 36))
        tiles.append(tile)
        print(f"{case}: 11 frames; worst red MAE={worst_mean:.6f}, peak={worst_peak:.4f}")
    if args.contact_sheet_prefix:
        for page in range((len(tiles) + 5) // 6):
            batch = tiles[page * 6:(page + 1) * 6]
            sheet = Image.new("RGB", (800, 166 * len(batch)), "white")
            for row, tile in enumerate(batch):
                sheet.paste(tile, (0, row * 166))
            path = Path(f"{args.contact_sheet_prefix}-{page + 1}.png")
            sheet.save(path)
            print(path)
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"PASS: {len(cases)} topology scenarios; {len(cases) * 11} frame pairs")


if __name__ == "__main__":
    main()
