#!/usr/bin/env python3
"""Check independent geometry-oracle captures for paint scope, modifiers, and nested merges."""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    suite = parser.add_mutually_exclusive_group()
    suite.add_argument("--group-modifiers", action="store_true",
                        help="Check GroupModifierMotionTest local-space modifier captures instead")
    suite.add_argument("--nested-booleans", action="store_true",
                       help="Check NestedBooleanTest native path-algebra captures instead")
    suite.add_argument("--trim-timing", action="store_true",
                       help="Check TrimTimingTest native and independently integrated cubic captures instead")
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    cases = ["fill", "stroke", "gradient-fill", "gradient-stroke", "multiple-paints",
             "chain", "hidden", "later-sibling", "earlier-paints", "styled-group",
             "inherited-paint", "repeater-opacity", "repeater-inherited", "offset", "twist", "zigzag"]
    prefix, channels, mean_limit = "paintscope-", range(3), 0.00005
    label = "paint-scope"
    frames, review_frames = range(11), (0, 4, 10)
    if args.group_modifiers:
        cases = ["offset", "twist", "pucker", "zigzag", "repeater"]
        prefix, channels, mean_limit = "groupmod-motion-", (0,), 0.0001
        label = "group-modifier"
    if args.nested_booleans:
        cases = [f"left-{inner}-{outer}" for inner in range(2, 6) for outer in range(2, 6)]
        cases += [f"{side}-{inner}-{outer}" for side in ("right", "right-live")
                  for inner in (3, 5) for outer in range(2, 6)]
        cases += ["both-live", "passthrough", "transformed", "painted-repeater", "path-repeater"]
        prefix, channels, mean_limit = "nestedboolean-", (0,), 0.0001
        label = "nested-boolean"
    if args.trim_timing:
        cases = ["morph", "fast", "both", "wrapped", "hold", "compound", "rectangle",
                 "eased", "full", "pucker", "pucker-fixed-trim", "pucker-static-trim",
                 "child-pucker-trim", "paint-pucker-trim", "owned-pucker-trim",
                 "cut-pucker", "compound-cut-pucker", "child-cut-pucker", "local-cut-transform",
                 "live-cut-pucker", "curve-cut-pucker", "morph-cut-pucker", "both-cut-pucker",
                 "sliding-cut-pucker", "hold-cut-pucker", "empty-full-cut", "live-cut-transform",
                 "wrapped-cut-pucker", "wrapped-morph-pucker", "wrapped-round-caps",
                 "wrapped-negative", "wrapped-held", "wrapped-alpha",
                 "closed-moving-cut", "closed-live-full", "closed-wrapped", "closed-empty-full",
                 "closed-held-full", "closed-transform", "closed-fill", "closed-constant-cut",
                 "closed-polygon-full", "closed-fill-padding-control",
                 "closed-loop-constant", "closed-loop-full", "compound-static", "compound-live",
                 "compound-wrapped", "compound-retrim", "compound-closed", "compound-empty-alpha",
                 "individual-static", "individual-live", "individual-wrapped", "individual-paints",
                 "individual-groups", "individual-mode-control", "individual-coincident",
                 "individual-child", "individual-earlier-trim", "individual-merged",
                 "individual-repeater", "individual-curves"]
        prefix, channels, mean_limit = "trimtiming-", (0,), 0.0001
        label, frames, review_frames = "trim-timing", range(16), (0, 5, 12)
    errors, tiles = [], []
    for name in cases:
        folder = module / "build/outputs/lottie-motion" / (prefix + name)
        worst_mean = worst_peak = 0
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
                differences = [abs(a.getpixel((x, y))[channel] - b.getpixel((x, y))[channel]) / 255
                               for y in range(ref.height) for x in range(ref.width)
                               for channel in channels]
                visible = sum(max(a.getpixel((x, y))) > 127
                              for y in range(a.height) for x in range(a.width))
                intentional_empty = args.trim_timing and name in ("empty-full-cut", "closed-empty-full", "compound-empty-alpha") and frame == 0
                if intentional_empty and (a.getbbox() is not None or b.getbbox() is not None):
                    errors.append(f"{name}/{frame}: zero-length trim must be entirely empty")
                elif not intentional_empty and visible <= 20:
                    errors.append(f"{name}/{frame}: empty reference")
                mean, peak = sum(differences) / len(differences), max(differences)
                worst_mean, worst_peak = max(worst_mean, mean), max(worst_peak, peak)
                # These rotated offset strokes also have independent live-coordinate tests.
                limit = 0.0006 if args.group_modifiers and name in ("offset", "repeater") else mean_limit
                if args.trim_timing and name == "closed-fill":
                    # Native zero-area contour padding changes convex-fill edge AA.
                    # The independent padded-native control still uses the strict gate.
                    red = a.getchannel("R")
                    high = red.filter(ImageFilter.MaxFilter(3)).tobytes()
                    low = red.filter(ImageFilter.MinFilter(3)).tobytes()
                    edge = [d for d,h,l in zip(differences,high,low) if h != l]
                    off_edge = sum(d != 0 for d,h,l in zip(differences,high,low) if h == l)
                    edge_mean = sum(edge) / max(len(edge),1)
                    if off_edge or edge_mean > .1 or peak > .25:
                        errors.append(f"{name}/{frame}: edge MAE={edge_mean:.6f}, off-edge={off_edge}, peak={peak:.4f}")
                elif mean > limit or peak > 0.25:
                    errors.append(f"{name}/{frame}: mean={mean:.6f}, peak={peak:.4f}")
        channel_label = "red" if channels == (0,) else "RGB"
        print(f"{name}: {len(frames)} captures; worst {channel_label} MAE={worst_mean:.6f}, peak={worst_peak:.4f}")
        if args.contact_sheet_prefix:
            tile = Image.new("RGB", (800, 166), "#ddd")
            draw = ImageDraw.Draw(tile)
            draw.text((4, 4), name + " (geometry oracle | RC)", fill="black")
            for column, frame in enumerate(review_frames):
                display_frame = {0: "0", 5: "0.5", 12: "1"}[frame] if args.trim_timing else str(frame)
                draw.text((column * 264 + 4, 20), f"Frame {display_frame}", fill="black")
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
    print(f"PASS: {len(cases)} {label} cases, {len(cases) * len(frames)} frame pairs")


if __name__ == "__main__":
    main()
