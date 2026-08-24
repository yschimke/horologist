# Spike: Lottie 1.0.1 Format

> **Status:** concluded
> **Created:** 2026-08-21
> **Updated:** 2026-08-21
> **Author:** Jetski
> **Time-box:** 5 minutes
> **Scope:** external (https://lottie.github.io/lottie-spec/1.0.1/single-page/)
> **Mode:** single
>
> **Target concept:** [format.concept.md](./format.concept.md)
> **Serves:** -
> **Question(s):**
> 1. What does the Lottie 1.0.1 specification define in terms of animated primitives?
> 2. Are there specific constraints or features missing from our current implementation that the spec covers?

## Context

We are aligning the `format` serialization layer with the official Lottie 1.0.1 JSON specification. We need to verify if the official spec includes structures that we haven't ported or recognized in our current parser.

## Exploration Log

### Entry 1 — 2026-08-21 — Lottie Format Specifications

**What was tried / researched:**
Read through the Lottie single-page spec (https://lottie.github.io/lottie-spec/1.0.1/single-page/). 

**Findings:**
- **Values:** Defines Booleans (as 0/1 integers), Vectors (arrays of numbers), Colors (RGB normalized 0 to 1), Gradients (color stops and opacity).
- **Properties:** Differentiates animated (`a`: 0 or 1) vs static properties. When animated, `k` contains arrays of Keyframes.
- **Keyframes:** Follow a specific schema `t` (time), `h` (hold), `i` (in tangent), `o` (out tangent). The `i` and `o` objects have `x` and `y` axes where `x` is clamped to [0,1] (time interpolation) and `y` is the value interpolation factor (can overshoot for bounce effects).
- **Split Position:** Contains `s` (split flag), and separate `x` and `y` Scalars.
- **Bezier Shape:** Represented via `c` (closed), `v` (vertices), `i`/`o` (in/out tangents for spatial curves).

## Alternatives Considered

| # | Approach | Pros | Cons | Verdict |
|---|----------|------|------|---------|
| 1 | Standardize current parser to spec exactly | Future proof, compliant, prevents edge-case crashes | Requires strict parsing of `a` and `k` distinctions with union types (or serializers) | chosen |
| 2 | Keep ad-hoc parsing | Faster if only specific files are supported | Unreliable for general-purpose Lottie files exported from AE/Figma | rejected |

## Conclusion

**Verdict:** The official Lottie JSON schema is highly structured around distinguishing static vs animated values uniformly across property types (e.g. `VectorProperty`, `ScalarProperty`, `ColorProperty`). We should align `format` serializers strictly with the spec.

**Key constraints discovered:**
- Easing control points (`i` and `o`) are mandatory for interpolation unless it's a hold keyframe (`h=1`) or the last keyframe in the sequence. `y` values can overshoot [0, 1].
- Colors are normalized [0..1] vectors, but alpha handling is inconsistent (4th vector element is often ignored by players).
- Gradients pack color and opacity stops sequentially in a single flattened array `[offset, r, g, b, ...] + [offset, alpha, ...]`.

**Recommendations for the concept:**
- The `format` module must robustly handle `k` representing either a direct primitive OR an array of keyframe objects depending on `a`.
- Our parsers should anticipate `s: true` (Split Positions) and properly branch off to parse `x` and `y` independently.

**Artifacts to keep:**
- —

**Artifacts to discard:**
- `.system_generated/steps/50/content.md` (Raw HTTP fetch dump of the webpage).
