#!/usr/bin/env python3
"""Audit the offline lottie-web rounding playback captures and make review sheets.

Run RoundedCornersRegressionTest first (ModifierOrderTest with --modifier-order).
Reference coordinates come from the pinned web
implementation; reference images are plain paths rendered without an RC modifier.
"""
import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    parser.add_argument("--modifier-order", action="store_true")
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    fixture_name = "modifier-order-reference.json" if args.modifier_order else "rounding-reference.json"
    prefix = "modifier-order-" if args.modifier_order else "rounding-"
    # Match each suite's existing full-image gate; do not loosen the rounding-only bound.
    mean_limit = 0.0001 if args.modifier_order else 0.00005
    fixtures = json.loads((module / "src/test/resources" / fixture_name).read_text())
    errors, tiles = [], []
    count = 0
    for case in fixtures["cases"]:
        folder = module / "build/outputs/lottie-motion" / (prefix + case["name"])
        worst_mean, worst_peak = 0, 0
        for frame in case["frames"]:
            count += 1
            index = frame["frame"]
            paths = [folder / f"frame{index}-{kind}.png" for kind in ("reference", "rc")]
            if not all(p.is_file() for p in paths):
                errors.append(f"{case['name']}/{index}: missing capture")
                continue
            with Image.open(paths[0]) as ref, Image.open(paths[1]) as rc:
                if ref.size != rc.size:
                    errors.append(f"{case['name']}/{index}: dimensions differ")
                    continue
                a_image, b_image = ref.convert("RGB"), rc.convert("RGB")
                a = [a_image.getpixel((x, y)) for y in range(ref.height) for x in range(ref.width)]
                b = [b_image.getpixel((x, y)) for y in range(rc.height) for x in range(rc.width)]
                differences = [abs(x[0] - y[0]) / 255 for x, y in zip(a, b)]
                mean, peak = sum(differences) / len(differences), max(differences)
                worst_mean, worst_peak = max(worst_mean, mean), max(worst_peak, peak)
                if mean > mean_limit or peak > 0.25 or sum(p[0] > 127 for p in a) <= 20:
                    errors.append(f"{case['name']}/{index}: mean={mean:.6f}, peak={peak:.4f}")
        print(f"{case['name']}: {len(case['frames'])} frames; worst red MAE={worst_mean:.6f}, peak={worst_peak:.4f}")
        if args.contact_sheet_prefix:
            tile = Image.new("RGB", (800, 166), "#ddd")
            draw = ImageDraw.Draw(tile)
            draw.text((4, 4), case["name"] + " (reference | RC)", fill="black")
            for column, frame in enumerate((0, 4, 10) if args.modifier_order else (0, 5, 10)):
                draw.text((column * 264 + 4, 20), f"Frame {frame}", fill="black")
                for side, kind in enumerate(("reference", "rc")):
                    path = folder / f"frame{frame}-{kind}.png"
                    if path.is_file():
                        with Image.open(path) as image:
                            thumb = image.convert("RGB")
                            thumb.thumbnail((128, 128))
                            tile.paste(thumb, (column * 264 + side * 132, 36))
            tiles.append(tile)
    if args.contact_sheet_prefix:
        for page in range((len(tiles) + 5) // 6):
            batch = tiles[page * 6:(page + 1) * 6]
            sheet = Image.new("RGB", (800, len(batch) * 166), "white")
            for row, tile in enumerate(batch):
                sheet.paste(tile, (0, row * 166))
            path = Path(f"{args.contact_sheet_prefix}-{page + 1}.png")
            sheet.save(path)
            print(path)
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"PASS: {len(fixtures['cases'])} rounding cases, {count} frame pairs")


if __name__ == "__main__":
    main()
