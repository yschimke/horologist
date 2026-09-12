#!/usr/bin/env python3
"""Check curved-offset playback captures against independent geometry references."""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageChops, ImageStat, ImageFilter


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    cases = [f"curve-{join}-{kind}" for join in (1, 3) for kind in ("amount", "geometry", "limit")]
    cases += [f"inflection-{join}-{kind}" for join in (1, 3) for kind in ("amount", "geometry")]
    cases += [f"{shape}-3-{kind}" for shape in ("split", "double") for kind in ("amount", "geometry")]
    cases += ["curve-1-amount-static", "quarter-2-false-false", "quarter-2-false-true",
              "quarter-2-true-false", "quarter-3-false-false", "quarter-1-false-false", "circle"]
    cases += ["loop-1-false-false", "loop-3-false-false", "loop-1-false-true", "loop-3-false-true",
              "loop-1-true-false", "loop-3-true-true", "loop-2-false-false", "loop-2-false-true",
              "loop-2-true-false"]
    total_frames = 0
    errors, tiles = [], []
    for name in cases:
        folder = module / "build/outputs/lottie-motion" / ("curveoffset-" + name)
        worst_mean = worst_peak = 0
        frames = [0] if name.endswith("-static") else list(range(11))
        total_frames += len(frames)
        for frame in frames:
            paths = [folder / f"frame{frame}-{kind}.png" for kind in ("reference", "rc")]
            if not all(path.is_file() for path in paths):
                errors.append(f"{name}/{frame}: missing capture")
                continue
            with Image.open(paths[0]) as ref, Image.open(paths[1]) as rc:
                if ref.size != rc.size:
                    errors.append(f"{name}/{frame}: dimensions differ")
                    continue
                a, b = ref.convert("RGB"), rc.convert("RGB")
                delta = ImageChops.difference(a, b).getchannel("R")
                mean, peak = ImageStat.Stat(delta).mean[0] / 255, delta.getextrema()[1] / 255
                if sum(a.getchannel("R").histogram()[128:]) <= 20:
                    errors.append(f"{name}/{frame}: empty reference")
                worst_mean, worst_peak = max(worst_mean, mean), max(worst_peak, peak)
                analytic = name.startswith(("quarter-", "loop-")) or name == "circle"
                if name.startswith("loop-") and name.endswith("-true"):
                    red = a.getchannel("R")
                    edges = [hi != lo for hi, lo in zip(red.filter(ImageFilter.MaxFilter(3)).tobytes(),
                                                      red.filter(ImageFilter.MinFilter(3)).tobytes())]
                    differences = delta.tobytes()
                    off_edge = sum(d > 0 and not edge for d, edge in zip(differences, edges))
                    edge_mean = sum(differences) / (255 * max(1, sum(edges)))
                    if off_edge or edge_mean > .02 or peak > .25:
                        errors.append(f"{name}/{frame}: off-edge={off_edge}, edge MAE={edge_mean:.6f}, peak={peak:.4f}")
                elif mean > (0.0001 if analytic else 0.0002) or peak > (0.25 if analytic else 0.3):
                    errors.append(f"{name}/{frame}: mean={mean:.6f}, peak={peak:.4f}")
        print(f"{name}: {len(frames)} frames; worst red MAE={worst_mean:.6f}, peak={worst_peak:.4f}")
        if args.contact_sheet_prefix:
            tile = Image.new("RGB", (800, 166), "#ddd")
            draw = ImageDraw.Draw(tile)
            draw.text((4, 4), name + " (independent reference | RC)", fill="black")
            for column, frame in enumerate((0,) if name.endswith("-static") else (0, 4, 10)):
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
    print(f"PASS: {len(cases)} curved-offset cases, {total_frames} frame pairs")


if __name__ == "__main__":
    main()
