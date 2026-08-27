# Concept: Lottie 1.0.1 Specification Parity Remediation & Hardening {#C_LOTTIE_REMED}

> **Code:** C_LOTTIE_REMED
> **Status:** active
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
>
> **Depends on:** [Format](format.concept.md), [Renderer](renderer.concept.md), [Shapes](shapes.concept.md), [Layers](layers.concept.md), [Track Matte & Styles](track_matte_and_styles.concept.md), [Screenshot Diff](screenshot_diff.concept.md)
> **Used by:** [Remediation Specification](lottie_remediation.sp.md), [Remediation Plan](lottie_remediation.plan.md)
> **Source Audit:** [Lottie 1.0.1 Specification Audit & Visual Verification Analysis](lottie_audit_and_gap_analysis.md) (`DOC_LOTTIE_AUDIT_2026_V3`)
>
> Comprehensive architectural concept addressing all critical bugs, mathematical regressions, compositing defects, and visual screenshot diff discrepancies identified during the 18-agent audit of `:remotecompose:lottie`.

---

## 1. Context & Motivation {#C_LOTTIE_REMED_01}

### 1.1 Problem Statement {#C_LOTTIE_REMED_01_01}
Following the initial rollout of the 19 Phase 1–4 capabilities, an exhaustive 18-agent audit evaluated `:remotecompose:lottie` across the official Lottie 1.0.1 specification, the last 50 commits, and pixel-level Roborazzi screenshot diffs against `lottie-android`. The audit verified high structural parity across static geometry, aspect ratio letterboxing, and complex parenting chains, but identified critical rendering defects in solid fill opacity compounding, vector glyph preservation in text layers, EvenOdd fill rule dispatch, sub-frame Look-Up Table easing quantization, inverted track matte clipping, and animated primitive trim path keyframe interpolation.

### 1.2 Core Architectural Principles {#C_LOTTIE_REMED_01_02}
1. **Mathematical Rigor & Spec Invariance:** Every rendering path must conform to the W3C/Lottie 1.0.1 mathematical specification. Hidden layers must act as track matte sources, skew rotations must follow standard Affine shear conventions, and Look-Up Table easing must be continuous across fractional sub-frames.
2. **Dynamic Keyframe Fidelity:** Graphic primitives (`Rectangle`, `Ellipse`, `PolyStar`) and modifier pipelines (`TrimPath`, `Repeater`) must evaluate dynamically across keyframes without collapsing to `(0, 0)` via lossy static unwrapping (`constantValueOrNull`).
3. **Compositing & Layer Integrity:** Layer masks (`masksProperties`) and track mattes (`tt`, `td`, `tp`) must compose algebraically using Skia/Canvas path operations (`UNION`, `DIFFERENCE`) and symmetric transform clamping so that sibling layers never inherit corrupted coordinate matrices.
4. **Typography & Styling Completeness:** Vector glyph subpaths must be preserved through the AST shape gathering pipeline even when unstyled, allowing parent `TextLayer` fill/stroke properties to dynamically style font contours.

---

## 2. Component Architecture & Remediation Areas {#C_LOTTIE_REMED_02}

### 2.1 Styling & Shader Pipeline (`RemoteStyle.kt`, `Shape.kt`) {#C_LOTTIE_REMED_02_01}
- **Solid Fill Opacity:** [`RemoteFill`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt) must receive `fill.opacity` (`o`) and compute effective alpha as $\alpha_{\text{eff}} = \alpha_{\text{color}} \times \left(\frac{\text{opacity}}{100}\right) \times \alpha_{\text{inherited}}$, mirroring `RemoteStroke`.
- **Stroke Miter Limit & Gradient Stop Guard:** [`RemoteStroke`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt) must assign `strokeMiterLimit` to `RemotePaint`, and gradient stop extraction must clamp color count against array boundaries to eliminate out-of-bounds risks.

### 2.2 Shape Evaluation & Modifier Math (`shapes/`, `properties/`) {#C_LOTTIE_REMED_02_02}
- **EvenOdd Fill Rule Canvas Dispatch:** [`RemoteShape.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteShape.kt) must assign `PathFillType.EvenOdd` to `drawScope.remotePath` when `fillRule == FillRule.EvenOdd`, ensuring self-intersecting stars and concentric cutouts render with transparent holes.
- **Dynamic Primitive Keyframe Evaluation:** [`Rectangle.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Rectangle.kt), [`Ellipse.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Ellipse.kt), and [`PolyStar.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/PolyStar.kt) must evaluate animated properties without collapsing to `(0, 0)` during dynamic playback.
- **Repeater Group Compounding:** [`Repeater.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Repeater.kt) must recursively transform child shapes when duplicating `RemoteGroup` nodes.

### 2.3 Timing, Easing & Interpolation (`Animation.kt`, `Scalar.kt`) {#C_LOTTIE_REMED_02_03}
- **Sub-Frame Look-Up Table Lerping:** [`lookupValueInBezier`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Animation.kt) must compute piecewise linear interpolation between integer table indices, eliminating the 0.9-frame animation lag and morphing curvature distortion on fractional progress captures.
- **Zero-Duration Easing Guard:** Guard `i / duration` when `duration == 0f` to prevent `Float.NaN` propagation in Bézier tables.

### 2.4 Compositing, Masks, Mattes & Transforms (`layers/`, `Transform.kt`) {#C_LOTTIE_REMED_02_04}
- **Track Matte & Mask Compositing (Upstream Limitation):** [`Shape.kt`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt) resolves `matteParent` pointers across non-adjacent layer indices and hidden matte sources (`hd: true`). *Upstream Blocker:* Canvas-level `ClipOp.Difference` clipping for inverted alpha (`tt: 2`), inverted luma (`tt: 4`), and subtract masks (`mode: "s"`) is blocked by upstream `androidx.compose.remote` `RemoteCanvas.clipPath` ignoring `clipOp` and serializing all clips as `INTERSECT`. These items must not be modified or worked around until the upstream fix lands.
- **Multi-Mask `Add` Mode Path Union:** Sequential `Add` masks must be unified via `Path.Op.UNION` before invoking canvas clipping.
- **Scale-Zero Transform Matrix Symmetry:** Clamp forward and inverse scale factors symmetrically to prevent matrix corruption on sibling layers.
- **Canvas Skew Axis Alignment:** Match `renderer/Transform.kt` rotation sequence with `GeometryTransform.kt`.

### 2.5 Typography & Assets (`TextLayer.kt`, `ImageLayer.kt`) {#C_LOTTIE_REMED_02_05}
- **TextLayer Glyph Harvesting:** [`Shape.kt:gatherShapes`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt) must harvest unstyled glyph geometries from font character definitions and apply text document fill/stroke styles dynamically.
- **ImageLayer Scaling:** Use native `bitmap.width` and `bitmap.height` for source rect bounds in `drawScaledBitmap`.
