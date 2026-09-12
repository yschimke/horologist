#!/usr/bin/env python3
"""Check complete bundled-fixture parity results and render reference/RC review sheets.

Run Cc0FixtureParityTest and BundledFixtureParityTest first. Each fixture is
compared at 13 shared authored-frame coordinates; this is not exhaustive parity.
"""

import argparse
import csv
import math
from pathlib import Path

from PIL import Image, ImageDraw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contact-sheet-prefix", type=Path)
    parser.add_argument("--include-merge-regressions", action="store_true",
                        help="Also require all 10 MergePathsRegressionTest reference comparisons")
    parser.add_argument("--include-scope-regressions", action="store_true",
                        help="Also require all 13 TrimScopeRegressionTest reference comparisons")
    parser.add_argument("--include-miter-regressions", action="store_true",
                        help="Also require all four StrokeMiterRegressionTest reference comparisons")
    parser.add_argument("--include-topology-regressions", action="store_true",
                        help="Also require all polystar topology reference comparisons")
    parser.add_argument("--include-compound-fill-regressions", action="store_true",
                        help="Also require all compound fill reference comparisons")
    parser.add_argument("--include-rounding-regressions", action="store_true",
                        help="Also require all Android rounded path reference comparisons")
    parser.add_argument("--include-group-modifier-regressions", action="store_true",
                        help="Also require all transformed group modifier reference comparisons")
    parser.add_argument("--include-repeater-scope-regressions", action="store_true",
                        help="Also require repeater group/paint/visibility comparisons")
    parser.add_argument("--include-merge-scope-regressions", action="store_true",
                        help="Also require grouped and greedy merge operand comparisons")
    parser.add_argument("--include-layer-stretch-regressions", action="store_true",
                        help="Also require ordinary-layer authored-time comparisons")
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    results = module / "build/outputs/lottie-parity"
    samples = sorted(p.stem for p in (module / "src/debug/res/raw").glob("*.json"))
    if args.include_merge_regressions:
        samples += [f"merge_{mode}_{motion}" for mode in range(2, 6)
                    for motion in ("static", "moving")]
        samples += ["merge_group_transform", "merge_repeater"]
    scope_samples = {
        "trim_before_geometry", "trim_between_geometry", "trim_scope_moving",
        "trim_before_group", "trim_group_inherited_stroke", "trim_compound",
        "trim_compound_moving", "trim_compound_parent", "trim_compound_animated",
        "trim_compound_offset", "round_before_geometry", "round_before_group",
        "round_group_inherited_stroke",
    }
    if args.include_scope_regressions:
        samples += sorted(scope_samples)
    if args.include_miter_regressions:
        samples += [f"miter_{style}_{limit}" for style in ("solid", "gradient")
                    for limit in (0, 4)]
    if args.include_topology_regressions:
        samples += ["topology_" + name for name in (
            "star-grow", "star-shrink", "star-rounded", "star-reverse", "star-hold",
            "star-trim", "polygon-grow", "polygon-shrink", "polygon-hold", "polygon-trim",
            "star-static-fraction", "polygon-static-fraction",
            "star-geometry", "polygon-geometry", "star-fill", "polygon-fill", "star-hole",
        )]
    if args.include_compound_fill_regressions:
        samples += ["compoundfill_" + name for name in (
            "nonzero", "evenodd", "gradient", "opacity", "nested", "inherited",
        )]
    if args.include_rounding_regressions:
        samples += ["roundpath_" + name for name in (
            "radius", "geometry", "trim", "intrinsic1", "intrinsic2",
        )]
    if args.include_group_modifier_regressions:
        samples += ["groupmod_" + name for name in (
            "uniform", "nonuniform", "live-scale", "mirrored", "nested",
            "paint-before", "styled", "compound-hole",
        )]
    if args.include_repeater_scope_regressions:
        samples += ["repeaterscope_" + name for name in (
            "owned-fill", "inherited-fill", "group-inherited", "nested-inherited",
            "styled-group", "scaled-stroke", "scaled-group-stroke", "live-copies",
            "group-live-copies", "consecutive", "open-live-copies", "rounded-live-copies",
            "gradient", "earlier-painted-path",
        )]
    if args.include_merge_scope_regressions:
        samples += ["mergescope_" + name for name in (
            "union", "subtract-group-last", "subtract-group-first", "subtract-interleaved",
            "intersect", "xor", "nested", "owned-group", "earlier-paint", "paint-before",
            "single-subtract", "hidden",
            "single-intersect", "empty-group", "static-subtract", "concatenate",
        )]
    if args.include_layer_stretch_regressions:
        samples += ["layerstretch_" + name for name in (
            "slow", "fast", "reverse", "solid", "parent", "mask", "mask-control", "precomp",
        )]
    errors = []
    tiles = []
    expected = {i / 10 for i in range(11)} | {0.25, 0.75}
    for sample in samples:
        folder = results / sample
        path = folder / "metrics.csv"
        if not path.is_file():
            errors.append(f"{sample}: missing metrics")
            continue
        with path.open() as stream:
            rows = list(csv.DictReader(stream))
        if {round(float(r["progress"]), 2) for r in rows} != expected or len(rows) != 13:
            errors.append(f"{sample}: incomplete or duplicated frame coverage")
        for row in rows:
            frame = round(float(row["progress"]) * 100)
            for kind in ("reference", "rc"):
                capture = folder / f"progress{frame}-{kind}.png"
                if not capture.is_file():
                    errors.append(f"{sample}: missing {frame}/{kind} capture")
                elif sample in ("layerstretch_mask", "layerstretch_mask-control"):
                    with Image.open(capture) as image:
                        if image.size != (64, 64):
                            errors.append(f"{sample}: mask timing requires native 64px capture")
                        if sample == "layerstretch_mask":
                            control = results / "layerstretch_mask-control" / capture.name
                            if not control.is_file():
                                errors.append(f"{sample}: missing unstretched mask control")
                            else:
                                with Image.open(control) as expected_image:
                                    actual_bytes = image.convert("RGBA").tobytes()
                                    expected_bytes = expected_image.convert("RGBA").tobytes()
                                    if actual_bytes != expected_bytes:
                                        errors.append(
                                            f"{sample}/{capture.name}: sr changed authored mask timing"
                                        )
            mean_error = float(row["mean_rgb_error"])
            foreground_error = float(row["foreground_rgb_error"])
            foreground_limit = 0.01 if sample == "cc0_precomp_stretch" or sample.startswith("merge_") else 0.15
            if sample in scope_samples:
                foreground_limit = 0.03
            if sample.startswith("miter_"):
                foreground_limit = 0.001
            if sample.startswith("topology_"):
                foreground_limit = 0.03
            if sample.startswith("compoundfill_"):
                foreground_limit = 0.01
            if sample.startswith("roundpath_"):
                foreground_limit = 0.03
            if sample.startswith("groupmod_"):
                foreground_limit = 0.02
            if sample.startswith("repeaterscope_"):
                foreground_limit = 0.02
            if sample.startswith("mergescope_"):
                foreground_limit = 0.02
            if sample.startswith("layerstretch_"):
                foreground_limit = 0.01
            if (not math.isfinite(mean_error) or not math.isfinite(foreground_error)
                    or mean_error >= 0.03
                    or foreground_error >= foreground_limit
                    or int(row["reference_pixels"]) <= 20):
                errors.append(f"{sample} progress={frame}: parity check failed: {row}")
        worst = max(float(r["foreground_rgb_error"]) for r in rows)
        print(f"{sample}: {len(rows)} frames; worst foreground RGB MAE {worst:.4f}")
        if args.contact_sheet_prefix:
            tile = Image.new("RGB", (1000, 136), "#ddd")
            draw = ImageDraw.Draw(tile)
            draw.text((4, 2), sample + "  (reference | RC in each pair)", fill="black")
            for column, frame in enumerate((0, 25, 50, 75, 100)):
                draw.text((column * 200 + 4, 18), f"{frame}%", fill="black")
                for side, kind in enumerate(("reference", "rc")):
                    image_path = folder / f"progress{frame}-{kind}.png"
                    if image_path.is_file():
                        with Image.open(image_path) as image:
                            thumb = image.convert("RGB")
                            thumb.thumbnail((96, 96))
                            tile.paste(thumb, (column * 200 + side * 100, 36))
            tiles.append(tile)
    if args.contact_sheet_prefix:
        for page in range((len(tiles) + 6) // 7):
            batch = tiles[page * 7:(page + 1) * 7]
            sheet = Image.new("RGB", (1000, len(batch) * 136), "white")
            for row, tile in enumerate(batch):
                sheet.paste(tile, (0, row * 136))
            path = Path(f"{args.contact_sheet_prefix}-{page + 1}.png")
            sheet.save(path)
            print(path)
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"PASS: {len(samples)} fixtures, {len(samples) * 13} reference frame pairs")


if __name__ == "__main__":
    main()
