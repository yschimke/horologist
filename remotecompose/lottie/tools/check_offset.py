#!/usr/bin/env python3
"""Check analytic polygon-offset playback captures after OffsetPathRegressionTest."""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    cases = [f"square-{join}-{kind}" for join in (1, 2, 3) for kind in ("amount", "geometry")]
    cases += ["square-1-limit", "line-1", "line-2", "line-3", "triangle-geometry", "reverse-amount"]
    errors, tiles = [], []
    for name in cases:
        folder = module / "build/outputs/lottie-motion" / ("offset-" + name)
        worst_mean = worst_peak = 0
        for frame in range(11):
            paths = [folder / f"frame{frame}-{kind}.png" for kind in ("reference", "rc")]
            if not all(path.is_file() for path in paths):
                errors.append(f"{name}/{frame}: missing capture")
                continue
            with Image.open(paths[0]) as ref, Image.open(paths[1]) as rc:
                if ref.size != rc.size:
                    errors.append(f"{name}/{frame}: dimensions differ")
                    continue
                a, b = ref.convert("RGB"), rc.convert("RGB")
                differences = [abs(a.getpixel((x, y))[0] - b.getpixel((x, y))[0]) / 255
                               for y in range(ref.height) for x in range(ref.width)]
                mean, peak = sum(differences) / len(differences), max(differences)
                worst_mean, worst_peak = max(worst_mean, mean), max(worst_peak, peak)
                if mean > 0.00005 or peak > 0.25:
                    errors.append(f"{name}/{frame}: mean={mean:.6f}, peak={peak:.4f}")
        print(f"{name}: 11 frames; worst red MAE={worst_mean:.6f}, peak={worst_peak:.4f}")
        if args.contact_sheet_prefix:
            tile = Image.new("RGB", (800, 166), "#ddd")
            draw = ImageDraw.Draw(tile)
            draw.text((4, 4), name + " (analytic reference | RC)", fill="black")
            for column, frame in enumerate((0, 4, 10)):
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
    print(f"PASS: {len(cases)} offset cases, {len(cases) * 11} frame pairs")


if __name__ == "__main__":
    main()
