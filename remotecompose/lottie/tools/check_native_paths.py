#!/usr/bin/env python3
"""Audit native-path round-trip captures; geometric precision is checked separately in Kotlin."""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    args = parser.parse_args()
    cases = ["quadratic", "cubic", "closed-cubic", "mixed-contours", "conic-quarter",
             "conic-circle", "conic-double", "conic-zero", "conic-unit", "oval",
             "boolean-union", "boolean-intersect", "boolean-difference", "boolean-xor",
             "analytic-degree-elevation"]
    root = Path(__file__).resolve().parents[1] / "build/outputs/lottie-native-path"
    errors, tiles = [], []
    for name in cases:
        tile = Image.new("RGB", (264, 290), "#ddd")
        draw = ImageDraw.Draw(tile)
        draw.text((4, 4), name, fill="black")
        draw.text((4, 15), "native | converted; fill / stroke", fill="black")
        for row, style in enumerate(("fill", "stroke")):
            paths = [root / name / f"{style}-{kind}.png" for kind in ("reference", "rc")]
            if not all(p.is_file() for p in paths):
                errors.append(f"{name}/{style}: missing capture")
                continue
            with Image.open(paths[0]) as source, Image.open(paths[1]) as output:
                if source.size != (256, 256) or output.size != source.size:
                    errors.append(f"{name}/{style}: wrong capture dimensions")
                    continue
                ref, actual = source.convert("RGB"), output.convert("RGB")
                red = ref.getchannel("R")
                high = red.filter(ImageFilter.MaxFilter(3)).tobytes()
                low = red.filter(ImageFilter.MinFilter(3)).tobytes()
                edge = [hi > 0 and lo < 255 for hi, lo in zip(high, low)]
                delta = [abs(a-b)/255 for a, b in zip(red.tobytes(), actual.getchannel("R").tobytes())]
                off_edge = sum(d > 0 and not e for d, e in zip(delta, edge))
                edge_mean = sum(delta) / max(1, sum(edge))
                peak, mean = max(delta), sum(delta)/len(delta)
                if name == "conic-zero" and style == "fill":
                    if red.getbbox() is not None or actual.getbbox() is not None:
                        errors.append(f"{name}/{style}: zero-weight degenerate fill must be empty")
                elif sum(r > 127 for r in red.tobytes()) <= 20:
                    errors.append(f"{name}/{style}: empty reference")
                if off_edge or edge_mean > .1 or peak > .5:
                    errors.append(f"{name}/{style}: off-edge={off_edge}, edge MAE={edge_mean}, peak={peak}")
                print(f"{name}/{style}: full MAE={mean:.6f}, edge MAE={edge_mean:.6f}, peak={peak:.4f}, off-edge={off_edge}")
                for col, image in enumerate((ref, actual)):
                    image.thumbnail((128, 128))
                    tile.paste(image, (col*132, row*132+27))
        tiles.append(tile)
    if args.contact_sheet_prefix:
        for page in range((len(tiles)+3)//4):
            batch = tiles[page*4:(page+1)*4]
            sheet = Image.new("RGB", (528, 290*((len(batch)+1)//2)), "white")
            for row, tile in enumerate(batch):
                sheet.paste(tile, ((row%2)*264, (row//2)*290))
            path = Path(f"{args.contact_sheet_prefix}-{page+1}.png")
            sheet.save(path)
            print(path)
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"PASS: {len(cases)} native-path scenarios, {len(cases)*2} capture pairs")


if __name__ == "__main__":
    main()
