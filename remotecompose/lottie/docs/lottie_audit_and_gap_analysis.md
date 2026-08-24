# Lottie 1.0.1 Specification Audit & Commit Regression Analysis {#DOC_LOTTIE_AUDIT_2026}

> **Code:** DOC_LOTTIE_AUDIT_2026
> **Status:** active
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Scope:** Commit history analysis (last 35 commits) and full official Lottie 1.0.1 specification gap analysis for `:remotecompose:lottie`.
> **Auditors:** Multi-Agent Specialist Panel (Rendering & Geometry, Serialization & AST, Compositing & Lifecycle, Graphic Elements & Modifiers, Layers & Assets, Properties & Interpolation).
> **Specification Reference:** [Official Lottie 1.0.1 Specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/)

---

## 1. Executive Summary

This document synthesizes findings from an exhaustive 6-agent deep dive into `:remotecompose:lottie`. It covers two major areas:
1. **Commit History & Bug Analysis (Last 35 Commits):** Detailed review of recent refactoring, geometry decoupling, track matte masking, and dynamic layer timeline changes to detect subtle regressions, numerical singularities, and unhandled edge cases.
2. **Official Lottie 1.0.1 Specification Gap Analysis:** Comprehensive line-by-line audit comparing the official Lottie 1.0.1 specification against the format AST and rendering pipeline across Graphic Elements, Modifiers, Layers, Assets, Properties, and Interpolation.

---

## 2. Part 1: Commit History Analysis (Last 35 Commits)

### 2.1 Chronological Overview of Commits by Domain

| Commit | Scope | Summary of Changes | Impact & Status |
|---|---|---|---|
| `c84c72d71` | Layer Lifecycle | Dynamic timeline visibility $[ip, op)$ via `selectIfLt` expressions with terminal `+0.01f` boundary extension | Fixes timeline clipping at `progress = 1.0f` |
| `8f85b8f57` | Geometry | Dynamic Bézier rectangle geometry in `evaluateRectangle` using `clamp` / `min` on `RemoteFloat` | Fixes sharp corners in `m3Next` vertical bar |
| `524c90aa8` | Compositing | Reverse layer iteration (`indices.reversed()`) in `LottieAnimation.kt` for bottom-up RemoteCompose stacking | Fixes visual z-order and track matte pairing |
| `d54992eb9` | Verification | Updated Roborazzi screenshot baselines for sound wave geometry parity | Golden baselines updated |
| `bd84f9962` | Geometry | Bake group affine transformations directly into shape paths for container-level styles | Fixes stroke scaling on sound wave rings |
| `4da918747` | Geometry | Extracted `GeometryTransform.kt` affine transformation engine for `RemoteBezierValue` | Pure mathematical geometry transforms |
| `3ebeb4326` | Rendering | Parity improvements across media Lottie animations (`volume_up`, `volume_down`, `mute_to_unmute`) | Visual parity for media player icons |
| `08f55a04b` | Compositing | Alpha Track Matte canvas clipping (`RemoteCanvas.clipPath`) and canvas matrix inversion | Enables track matte masking |
| `ea08daa3a` | Styling | Container-level styles (`Fill`, `Stroke`) binding to geometries inside preceding sibling child groups | Enables outer styling on grouped paths |
| `36d878557` | Verification | Expanded `MediaLottieDiffScreenshotTest` with multi-frame milestones (0%, 25%, 50%, 100%) | Multi-progress regression safety |
| `e816da2b4` | Rendering | Grouped vector shape rendering in RemoteCompose Lottie | Multi-shape group styling |
| `9145aacf4` | Modifiers | Dynamic `TrimPath` evaluator for stroked vector lines and mute slash transitions | Dynamic stroke trimming |
| `983c2e7c0` | Geometry | Extracted de Casteljau cubic Bézier curve segment sampling utilities | Arc-length parameterization |
| `53c582fc0` | Transform | 2D skew matrix support ($R(-\theta) \cdot K(-\tan(\text{skew})) \cdot R(\theta)$) in `Transform.kt` | Skew and skewAxis evaluation |
| `9994016e2` | Format | Added data classes and serializers for Lottie 1.0.1 modifiers (`op`, `pb`, `tw`, `zz`) | Prevents modifier data loss in AST |
| `652b5f211` | Build | Cleaned up compiler and lint warnings across the Lottie module | Hygiene and build cleanliness |
| `74134e2ca` | Layer | Added layer timeline checks and `SolidColorLayer` rendering | Solid color background rendering |
| `e5e3b50e0` | Refactoring | Modularized layer renderers under `renderer/layers/` package | Decoupled layer dispatch |
| `9ca3f3bd6` | Layer | Modularized layer AST under `format/layer/` and preserved transform hierarchies with `UnknownLayer` | Ancestor transform preservation |
| `0c977936c` | Format | Removed deprecated compatibility typealiases from format layer | Clean AST interfaces |
| `0639f5f6d` | Styling | Added solid stroke rendering (`RemoteStroke`) with line caps and joins | Solid stroke rendering |
| `b79816737` | Geometry | Decoupled shape evaluators into `renderer/shapes/` (`Rectangle`, `Ellipse`, `PolyStar`, `Path`) | Decoupled shape rendering |
| `be997ade3` | Format | Modularized Graphic Elements into domain category packages (`geometry`, `styles`, `grouping`, `modifiers`) | Domain-driven AST structure |
| `1119a7fb2` | Format | Aligned Gradient property AST and animation renderer with Lottie 1.0.1 | Gradient stop data structures |
| `fadeecffb` | Verification | Added unit tests for gradient deserialization and interpolation | Test coverage for gradients |
| `e4a269240` | Refactoring | Decoupled scalar animation renderer and updated consumers | Modular scalar evaluation |
| `c9fbcb604` | Format | Aligned scalar property AST with Lottie 1.0.1 specification | Scalar property schema |
| `65cfa116f` | Verification | Added unit test assertions for scalar deserialization and animation | Test coverage for scalars |
| `94be0bc5d` | Refactoring | Decoupled position animation renderer and updated consumers | Modular position evaluation |
| `bdb786648` | Format | Aligned position property AST with Lottie 1.0.1 specification | Position property schema |
| `19c592e1a` | Verification | Added unit test assertions for position deserialization and animation | Test coverage for positions |
| `9831282cb` | Refactoring | Decoupled vector animation renderer and updated consumers | Modular vector evaluation |
| `bf4ec2d40` | Format | Supported flexible vector property deserialization (2D, 3D, and lists) | Vector property resilience |
| `2c744cc73` | Verification | Added unit test assertions for vector deserialization resilience | Test coverage for vectors |
| `2b35f088f` | Geometry | Render dynamic Bézier paths using `drawScope.remotePath { ... }` DSL | Dynamic Bézier expression evaluation |

---

### 2.2 Critical Bugs, Edge Cases & Regressions Identified

#### 1. Track Matte Clipping Reintroduces Static `constantValueOrNull` Collapsing
- **Location:** [`remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt#L438-L483)
- **Root Cause:** In `buildRemotePathFromBezier`, coordinates are extracted via `.constantValueOrNull ?: 0f`. If a track matte source layer contains animated keyframes, `constantValueOrNull` returns `null`, collapsing vertices to `(0, 0)` and incorrectly clipping out the target layer.
- **Fix:** Refactor `buildRemotePathFromBezier` to emit dynamic expressions or record a dynamic path builder.

#### 2. `PolyStar` Geometry Drops Group Transforms and Ignores Dynamic Animation
- **Location:** [`remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/PolyStar.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/PolyStar.kt#L94) & [`GeometryTransform.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/GeometryTransform.kt#L45-L48)
- **Root Cause:** `evaluatePolyStar` returns `RemoteCompiledPath` instead of `RemoteLottiePath`. `GeometryTransform.transformRemoteShape` ignores non-`RemoteLottiePath` instances, causing polystars inside child groups to lose affine translations, scales, rotations, and skews when styled by container-level fills/strokes. In addition, `evaluatePolyStar` extracts static floats via `.constantValueOrNull`.
- **Fix:** Refactor `evaluatePolyStar` to return `RemoteLottiePath(listOf(remoteBezier))` with dynamic `RemoteBezierValue` vertices.

#### 3. Parent Layer Opacity Lost in Multi-Level Ancestor Hierarchy
- **Location:** [`remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt#L79-L81)
- **Root Cause:** `layerOpacity` extracts only `transformStack.lastOrNull()?.opacity`, ignoring intermediate ancestor layer opacities.
- **Fix:** Fold through all transforms in the stack: `val layerOpacity = transformStack.fold(layerVisibility) { acc, t -> acc * (animateScalar(t.opacity, animationSettings) / 100f) }`.

#### 4. Singularity / Division by Zero on Scale = 0 in `inverseTransform`
- **Location:** [`remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt#L88-L89)
- **Root Cause:** `1f.rf / scaleX` divides by zero when an animated scale passes through 0%, generating `NaN`/`Infinity` in canvas transformation matrices.
- **Fix:** Guard with `selectIfLt(abs(scaleX), 0.0001f.rf, 1f.rf, 1f.rf / scaleX)`.

#### 5. Missing Default Values in AST Triggering `MissingFieldException`
- **Location:** [`format/Animation.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/Animation.kt#L28-L37), [`format/layer/SolidColorLayer.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/SolidColorLayer.kt#L42-L44), [`format/graphicelement/styles/Fill.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt#L46)
- **Root Cause:** Several required Kotlinx Serialization properties lack default values. Valid real-world Lottie JSON files omitting these fields crash with `MissingFieldException`. Furthermore, `Animation.frameRate` typed as `Int` throws `SerializationException` on fractional framerates (e.g. `"fr": 29.97`).
- **Fix:** Add default values to all AST properties and change `frameRate` to `Float = 30f`.

#### 6. Keyframe `hold` Flag Parsing Bug in `ColorPropertyKeyframeSerializer`
- **Location:** [`format/properties/Color.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Color.kt#L290)
- **Root Cause:** `(obj["h"]?.jsonPrimitive?.intOrNull ?: 0) == 1` fails when `"h": true` (boolean) is exported by Bodymovin, causing hold keyframes to interpolate smoothly instead of stepping.
- **Fix:** Parse `booleanOrNull` or `intOrNull == 1`.

#### 7. Gradient Stop Count `p` Miscalculation When Omitted
- **Location:** [`format/values/Gradient.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/values/Gradient.kt#L160)
- **Root Cause:** `floatList.size % 4 == 0` evaluates true for transparent gradients (e.g. 2 color stops + 2 opacity stops = 12 floats). The parser assumes 3 color stops and 0 opacity stops, corrupting color channels.
- **Fix:** Do not infer `count = floatList.size / 4` when opacity stops are present without explicit `p`.

#### 8. Potential Infinite Recursion on Circular Parent Hierarchies
- **Location:** [`LottieAnimation.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/LottieAnimation.kt#L198)
- **Root Cause:** `populateAncestorTransforms` lacks cycle detection. Circular parent references in corrupted files throw `StackOverflowError`.
- **Fix:** Track a `visited: Set<Int>` set during tree traversal.

---

## 3. Part 2: Official Lottie 1.0.1 Specification Gap Analysis

### 3.1 Graphic Elements (Shapes) & Modifiers Parity Matrix

| Feature | Spec Type | AST Support | Renderer Support | Compliance Verdict | Notes / Discrepancies |
|---|:---:|:---:|:---:|:---:|---|
| **Path (Bézier)** | `"sh"` | Full | Full | **Compliant** | Direction $d=3$ (counter-clockwise) not inverted |
| **Rectangle** | `"rc"` | Full | Full | **Compliant** | Rounded corners dynamic evaluation supported |
| **Ellipse** | `"el"` | Full | Full | **Compliant** | Dynamic `RemoteLottiePath` evaluation supported |
| **PolyStar** | `"sr"` | Full | Partial | **Partially Compliant** | Static evaluation via `constantValueOrNull`; unhandled keyframe animation |
| **Solid Fill** | `"fl"` | Full | Partial | **Partially Compliant** | FillRule EvenOdd (`r=2`) not passed to `RemotePath` |
| **Solid Stroke** | `"st"` | Full | Partial | **Partially Compliant** | Dash pattern array `d` and `miterLimit` ignored in `RemotePaint` |
| **Gradient Fill** | `"gf"` | Full | Stub | **AST Only** | `RemoteGradientFill.getPaint()` returns empty `RemotePaint()` |
| **Gradient Stroke** | `"gs"` | Buggy | Stub | **AST Bug / Stub** | SerialName bugs: `highlightLength` uses `"r"` (spec `"h"`), `highlightAngle` uses `"h"` (spec `"a"`) |
| **Group / Container** | `"gr"` | Full | Full | **Compliant** | Group transform baking supported |
| **Transform** | `"tr"` | Full | Full | **Compliant** | Anchor, Position, Scale, Rotation, Opacity, Skew, SkewAxis evaluated |
| **Trim Path** | `"tm"` | Full | Partial | **Partially Compliant** | Supported on `Path`; bypassed on `Rectangle`, `Ellipse`, `PolyStar` |
| **Repeater** | `"rp"` | Full | Missing | **AST Only** | Multi-instance geometry duplication not implemented in renderer |
| **Rounded Corners** | `"rd"` | Full | Missing | **AST Only** | Corner rounding modifier not implemented in renderer |
| **Merge Paths** | `"mm"` | Full | Missing | **AST Only** | Boolean path operations (Union, Subtract, Intersect) not implemented |
| **Offset Path** | `"op"` | Full | Missing | **AST Only** | Path outline offset not implemented |
| **Pucker / Bloat** | `"pb"` | Full | Missing | **AST Only** | Deformation modifier not implemented |
| **Twist** | `"tw"` | Full | Missing | **AST Only** | Angular rotational distortion not implemented |
| **Zig Zag** | `"zz"` | Full | Missing | **AST Only** | Ridged path deformation not implemented |

---

### 3.2 Layers, Composition, Assets, Mattes & Masks Parity Matrix

| Feature | Spec Type | AST Support | Renderer Support | Compliance Verdict | Notes / Discrepancies |
|---|:---:|:---:|:---:|:---:|---|
| **Root Assets (`assets[]`)** | `Asset[]` | Missing | Missing | **Critical Gap** | Omitted from `Animation.kt`; blocks Precomps and Image assets |
| **Precomposition Layer** | `ty: 0` | Partial | Missing | **Critical Gap** | Parsed in `PrecompLayer.kt` but dispatcher is no-op `{}` |
| **Solid Color Layer** | `ty: 1` | Full | Full | **Compliant** | Renders quad path on `RemoteCanvas` |
| **Image Layer** | `ty: 2` | Partial | Missing | **AST Only** | No bitmap texture decoding or drawing |
| **Null Layer** | `ty: 3` | Full | Full | **Compliant** | Anchor for parent transform chains |
| **Shape Layer** | `ty: 4` | Full | Full | **Compliant** | Core vector rendering engine |
| **Text Layer** | `ty: 5` | Stub | Missing | **AST Only** | Document data and glyph typography unrendered |
| **Audio Layer** | `ty: 6` | Missing | Missing | **Missing** | Falls back to `UnknownLayer` |
| **Layer Masks (`masksProperties`)** | `Mask[]` | Missing | Missing | **Critical Gap** | Layer-level Bézier clipping completely absent from AST and renderer |
| **Track Mattes (`tt`, `td`, `tp`)** | `Matte` | Partial | Partial | **Partially Compliant** | Alpha matte (`tt: 1`) supported; Inverted Alpha (`tt: 2`), Luma (`tt: 3, 4`), and non-adjacent `tp` unsupported |
| **Layer Timing (`st`, `sr`, `tm`)** | `Float` | Partial | Missing | **Partially Compliant** | Start time offset `st` and time stretch `sr` not applied to local layer clock |
| **Blend Modes (`bm`)** | `Enum` | Full | Missing | **AST Only** | Blend modes parsed in AST but not applied to canvas paints |
| **Markers (`markers[]`)** | `Marker[]` | Missing | Missing | **Missing** | Named cue points omitted from AST |

---

### 3.3 Properties, Keyframes & Mathematical Interpolation Parity Matrix

| Feature | Spec Section | AST Support | Renderer Support | Compliance Verdict | Notes / Discrepancies |
|---|---|:---:|:---:|:---:|---|
| **Scalar Properties** | `#specs-properties-value` | Full | Full | **Compliant** | Static and keyframed scalar properties supported |
| **Vector Properties** | `#specs-properties-vector` | Full | Partial | **Partially Compliant** | Multidimensional easing handles collapsed to 1D (`firstOrNull()`) |
| **Position Properties (Linear)** | `#specs-properties-position` | Full | Full | **Compliant** | Split position (`s: true`) and linear keyframes supported |
| **Spatial Bézier Tangents (`to`, `ti`)** | `#specs-properties-position-keyframe` | Full | Missing | **Critical Math Gap** | `to` and `ti` parsed in AST but **ignored in renderer**; curved paths collapse to straight lines |
| **Color Properties** | `#specs-properties-color` | Full | Full | **Compliant** | RGBA `tween()` interpolation across keyframe timeline |
| **Gradient Colors Property** | `#specs-values-gradient-colors` | Full | Partial | **Partially Compliant** | Stops unpacked in AST; shader execution in `RemotePaint` is no-op |
| **Bézier Shape Morphing** | `#specs-properties-bezier` | Full | Full | **Compliant** | Dynamic vertex/tangent morphing with cubic easing |
| **Hold Keyframes (`h: 1`)** | `#specs-properties-base-keyframe` | Full | Full | **Compliant** | Instantaneous step transition supported |
| **Cubic Timing Easing (`i`, `o`)** | `#specs-properties-easing-handle` | Full | Partial | **Partially Compliant** | Discretized to integer frames (`duration.toInt()`); sub-frame interpolation quantized |
| **Timeline Clock Progress** | `#specs-composition` | Full | Partial | **Partially Compliant** | `floor(...)` in `LottieAnimation.kt` quantizes default playback to integer frames |

---

## 4. Part 3: Prioritized Action Plan & Dev-Flow Implementation Roadmap

```
+===================================================================================+
| PHASE 1: Critical Bug Fixes & Serialization Hardening (Immediate)                 |
+===================================================================================+
| 1. Fix `GradientStroke.kt` SerialName annotations (`highlightLength`, `angle`).    |
| 2. Add default values across all AST models to eliminate `MissingFieldException`. |
| 3. Change `Animation.frameRate` to `Float = 30f` for fractional framerates.       |
| 4. Fix keyframe `hold` flag parsing in `ColorPropertyKeyframeSerializer`.         |
| 5. Guard scale division by zero in `Transform.inverseTransform`.                  |
| 6. Compound all ancestor layer opacities in `Shape.kt`.                           |
| 7. Convert `PolyStar` to return `RemoteLottiePath` with dynamic expressions.      |
| 8. Add cycle detection to `buildAncestorTransforms` in `LottieAnimation.kt`.     |
+===================================================================================+
                                         |
                                         v
+===================================================================================+
| PHASE 2: Core Rendering & Mathematical Parity (High Priority)                     |
+===================================================================================+
| 1. Implement Spatial Bézier Tangents (`to`, `ti`) in `Position.kt`.               |
| 2. Implement Gradient Shaders in `RemotePaint` for `GradientFill` & `Stroke`.     |
| 3. Implement Stroke Dash Array (`d`) & Miter Limit (`ml`) in `RemoteStroke`.      |
| 4. Pass `FillRule` (`EvenOdd` vs `NonZero`) to `RemotePath`.                      |
| 5. Wire `TrimPath` to `Rectangle`, `Ellipse`, and `PolyStar` shapes.              |
| 6. Implement Local Layer Timing Context: $t_{\text{local}} = (t - st) / sr$.      |
| 7. Support Inverted Alpha Track Matte (`tt: 2`) and explicit matte parent `tp`.   |
+===================================================================================+
                                         |
                                         v
+===================================================================================+
| PHASE 3: Composition, Precomps & Layer Mask Pipeline (Medium Priority)            |
+===================================================================================+
| 1. Add `assets[]` registry to `format/Animation.kt`.                              |
| 2. Implement `PrecompLayer` recursive sub-composition rendering engine.          |
| 3. Add `masksProperties` to `Layer` AST and implement canvas mask clipping.       |
| 4. Support Precomposition Time Remapping (`tm`).                                  |
| 5. Add `markers[]` named timeline cues to `Animation.kt`.                         |
+===================================================================================+
                                         |
                                         v
+===================================================================================+
| PHASE 4: Advanced Modifiers, Typography & Assets (Long-Term)                      |
+===================================================================================+
| 1. Implement `Repeater` modifier geometry duplication with transform compounding.|
| 2. Implement `RoundedCorners` modifier on `RemoteBezierValue`.                    |
| 3. Implement `MergePaths` boolean path operations.                                |
| 4. Implement Bitmap Asset loading and `ImageLayer` rendering (`ty: 2`).           |
| 5. Implement Vector Typography (`ty: 5`) and Text Document data structures.       |
+===================================================================================+
```
