# Specification: Lottie 1.0.1 Specification Parity Remediation & Hardening {#SP_LOTTIE_REMED}

> **Code:** SP_LOTTIE_REMED
> **Status:** active
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
>
> **Concept:** [Lottie Remediation Concept](lottie_remediation.concept.md) (`C_LOTTIE_REMED`)
> **Depends on:** [Format Spec](format.sp.md), [Renderer Spec](renderer.sp.md), [Shapes Spec](shapes.sp.md), [Layers Spec](layers.sp.md), [Track Matte Spec](track_matte_and_styles.sp.md)
> **Used by:** [Remediation Plan](lottie_remediation.plan.md) (`PL_LOTTIE_REMED`)
>
> Technical specification defining data contracts, mathematical formulas, state machine invariants, and verification criteria for fixing the defects identified in `DOC_LOTTIE_AUDIT_2026_V3`.

---

## 1. Mathematical Contracts & Formulas {#SP_LOTTIE_REMED_01}

### 1.1 Solid Fill Opacity Compounding {#SP_LOTTIE_REMED_01_01}
In `RemoteStyle.kt`, `RemoteFill` must accept `fillOpacity: RemoteFloat`:
$$\alpha_{\text{final}} = \alpha_{\text{fillColor}} \times \left(\frac{\text{fillOpacity}}{100.0}\right) \times \alpha_{\text{inherited}}$$
When `fillOpacity` is static ($100$), this reduces identically to the current formula without extra arithmetic nodes.

### 1.2 Sub-Frame Look-Up Table Linear Interpolation (`lerp`) {#SP_LOTTIE_REMED_01_02}
In `Animation.kt:lookupValueInBezier`, given continuous frame offset $t \in [0, \text{duration}]$ and integer Look-Up Table array $V[0 \dots N]$:
$$i_{\text{floor}} = \lfloor t \rfloor, \quad i_{\text{ceil}} = \min(i_{\text{floor}} + 1, N), \quad \text{frac} = t - i_{\text{floor}}$$
$$\text{Value}(t) = (1 - \text{frac}) \cdot V[i_{\text{floor}}] + \text{frac} \cdot V[i_{\text{ceil}}]$$
This guarantees $C^0$ continuity across all non-integer timeline progress steps ($p \in [0.0, 1.0]$).

### 1.3 Canvas Skew Axis Rotation Inversion Fix {#SP_LOTTIE_REMED_01_03}
To apply skew $\theta_{\text{skew}}$ along axis $\phi_{\text{axis}}$ to the Canvas matrix:
$$\mathbf{M} = \mathbf{R}(\phi_{\text{axis}}) \cdot \mathbf{S}_{\text{skew}}(\theta_{\text{skew}}) \cdot \mathbf{R}(-\phi_{\text{axis}})$$
The sequence of Canvas operations must be:
1. `canvas.rotate(skewAxis)`
2. `canvas.skew(tan(toRad(skew)), 0f)`
3. `canvas.rotate(-skewAxis)`

### 1.4 Scale-Zero Singularity Symmetry {#SP_LOTTIE_REMED_01_04}
Given scale components $(s_x, s_y)$, forward scale factor $s_{\text{fwd}}$ and inverse scale factor $s_{\text{inv}}$ must satisfy:
$$s_{\text{clamped}} = \text{sign}(s) \cdot \max(|s|, \epsilon), \quad \epsilon = 10^{-4}$$
$$s_{\text{fwd}} = s_{\text{clamped}}, \quad s_{\text{inv}} = \frac{1}{s_{\text{clamped}}}$$
Forward transforms scale by $s_{\text{clamped}}$, ensuring the composite matrix $\mathbf{M}_{\text{fwd}} \cdot \mathbf{M}_{\text{inv}} = \mathbf{I}$.

---

## 2. Component Contracts & Interfaces {#SP_LOTTIE_REMED_02}

### 2.1 Styling & Canvas Path Fill Type {#SP_LOTTIE_REMED_02_01}
| Class / Function | Modified Interface / Signature | Behavior Contract |
|---|---|---|
| `RemoteFill` | `constructor(fillColor: RemoteColor, opacity: RemoteFloat = 100f.rf, fillRule: FillRule)` | Modulates `fillColor.alpha` by `(opacity / 100f) * inheritedOpacity`. |
| `RemoteLottiePath` | `draw(drawScope: RemoteDrawScope, inheritedOpacity: RemoteFloat)` | Sets `path.fillType = PathFillType.EvenOdd` when `fillRule == FillRule.EvenOdd`. |
| `RemoteCompiledPath` | `draw(drawScope: RemoteDrawScope, inheritedOpacity: RemoteFloat)` | Sets `path.fillType = PathFillType.EvenOdd` when `fillRule == FillRule.EvenOdd`. |
| `RemoteStroke` | `getPaint(inheritedOpacity: RemoteFloat): RemotePaint` | Assigns `this.strokeMiterLimit = miterLimit` when non-null. |

### 2.2 TextLayer Glyph Harvesting & Styling {#SP_LOTTIE_REMED_02_02}
In `Shape.kt`:
```kotlin
internal fun gatherShapes(
  elements: List<GraphicElement>,
  activeTrimPath: TrimPath?,
  activeRoundedCorners: RoundedCorners?,
  animationSettings: LottieSettings,
  inheritedStyle: ShapeStyle? = null,
): List<StyledShapes>
```
When `inheritedStyle` is non-null (e.g. from parent `TextLayer`), unstyled `Path` geometries inside `chars[].data.shapes` are harvested and wrapped in `StyledShapes(shapes = [...], style = inheritedStyle)`.

### 2.3 Layer Masks & Track Matte Compositing {#SP_LOTTIE_REMED_02_03}
| Feature | Contract | Expected Output |
|---|---|---|
| `MaskMode.Add` Multi-Mask | Merge all `Add` masks into `compositeAddPath` via `Path.Op.UNION`. | Canvas clips intersection with composite union path: `canvas.clipPath(compositeAddPath, ClipOp.Intersect)`. |
| `MatteMode.InvertedAlpha` | Clip layer bounds difference with matte path: `canvas.clipPath(mattePath, ClipOp.Difference)`. | **[BLOCKED UPSTREAM]** Alpha content cuts out a hole; blocked by upstream `RemoteCanvas.clipPath` ignoring `ClipOp.Difference`. |
| Non-Adjacent `matteParent` | Resolve `layer.matteParent` index by searching all layers in `animation.layers`. | Matte target receives clipping even when intermediate layers exist. |
| Hidden Matte Source | If `matteLayer.hidden == true`, matte path is evaluated for clipping but not drawn. | Spec-compliant track matte masking from hidden control layers. |

### 2.4 Modifiers & Asset Scaling {#SP_LOTTIE_REMED_02_04}
- `Repeater.kt`: When `shape is RemoteGroup`, return a new `RemoteGroup` where `childShapes` have their inner shapes transformed recursively via `transformRepeaterShape`.
- `ImageLayer.kt`: `drawScaledBitmap` passes `srcLeft = 0f`, `srcTop = 0f`, `srcRight = bitmap.width.toFloat()`, `srcBottom = bitmap.height.toFloat()` to `drawBitmap`.

---

## 3. Verification Criteria & Test Contracts {#SP_LOTTIE_REMED_03}

| Contract ID | Test Area | Verification Method | Pass Criteria |
|---|---|---|---|
| `SP_LOTTIE_REMED_03_01` | Solid Fill Opacity | Unit & Screenshot | Fills with `o: 50` render with 50% transparency against background. |
| `SP_LOTTIE_REMED_03_02` | TextLayer Glyphs | `textLayerVectorGlyphs` Screenshot | Character glyph contours render with crisp vector fill matching `lottie-android`. |
| `SP_LOTTIE_REMED_03_03` | EvenOdd Fill Rule | `fillRuleEvenOdd` Screenshot | 5-pointed star renders hollow center cutout with transparent interior. |
| `SP_LOTTIE_REMED_03_04` | Sub-Frame Lerp | `m3Next` & `playPause` Screenshot | Intermediate progress frames ($p=0.23, p=0.25$) render without morph lag or duplicate shapes. |
| `SP_LOTTIE_REMED_03_05` | Inverted Alpha Matte | `trackMatteInvertedAlpha` Screenshot | **[BLOCKED UPSTREAM]** Square renders with circular hole clipped out (pending upstream `clipPath` fix). |
| `SP_LOTTIE_REMED_03_06` | Non-Adjacent Matte | `trackMatteNonAdjacentParent` Screenshot | Target star layer is rendered with inverted circle cutout. |
| `SP_LOTTIE_REMED_03_07` | Primitive Keyframes | Unit Test & Animated Screenshots | Animated rectangles/ellipses/polystars preserve nonzero geometry throughout animation. |
| `SP_LOTTIE_REMED_03_08` | Image Scaling | `imageLayerBase64` Screenshot | 1x1 bitmap scales to declared layer/asset bounds $(48 \times 48)$. |
| `SP_LOTTIE_REMED_03_09` | Multi-Mask Add Union | Unit & Screenshot | Two overlapping `Add` masks render the union area rather than their mutual intersection. |
| `SP_LOTTIE_REMED_03_10` | Repeater Groups | Unit Test | Duplicating a group produces distinct geometric copies offset along transform vector. |
