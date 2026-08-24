# Specification: Track Matte Masking & Container Style Resolution {#SP_LOTTIE_MATTE}

> **Code:** SP_LOTTIE_MATTE
> **Status:** active
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Concept:** [Track Matte Concept](track_matte_and_styles.concept.md)
> **Depends on:** `C_LOTTIE_MATTE`, `SP_LOTTIE_LAYERS`, `SP_LOTTIE_SHAPES`
> **Used by:** `PL_LOTTIE_MATTE`, `Shape.kt`, `ShapeLayer.kt`, `LottieAnimation.kt`
>
> Formal specification for container-level shape style scope resolution across sibling groups and Alpha Track Matte layer pairing with canvas-level geometry clipping in `:remotecompose:lottie`.

## 01 Data Structures & Rendering Models {#SP_LOTTIE_MATTE_01_01}

### 1. Track Matte Pairing Model (`format/layer/Layer.kt`)
Each layer in the Lottie composition contains the following properties parsed by `LayerSerializer`:
```kotlin
abstract val index: Int?
abstract val matteMode: MatteMode?     // "tt": 0=Normal, 1=Alpha, 2=InvertedAlpha, 3=Luma, 4=InvertedLuma
abstract val matteTarget: Int?        // "td": 1 if this layer acts as a track matte source
```

### 2. Paired Matte Definition (`renderer/layers/ShapeLayer.kt`) {#SP_LOTTIE_MATTE_01_02}
During composition traversal, a target layer that references an active matte source is provided with a `MatteContext`:
```kotlin
internal data class MatteContext(
  val matteLayer: Layer,
  val matteTransforms: List<Transform>,
)
```

## 02 Container & Sibling Style Scope Resolution {#SP_LOTTIE_MATTE_02_01}

### 1. Shape Accumulation Contract (`renderer/Shape.kt`)
In Lottie container scopes (`ShapeLayer.shapes` or `Group.shapes`), style elements (`Fill`, `Stroke`, `GradientFill`, `GradientStroke`) bind to all preceding geometry elements in that scope. When a geometry is encapsulated in a sibling `Group` without internal styles, container-level styles must apply to the group's internal geometries transformed into the container's coordinate space. Group transformations are baked into the `RemoteLottiePath` geometry, ensuring stroke widths and paint properties remain uniform in the container coordinate system.

### 2. Resolution Algorithm (`gatherShapes`) {#SP_LOTTIE_MATTE_02_02}
```text
Input: List<GraphicElement> shapes, LottieSettings animationSettings, TrimPath parentTrimPath
Output: List<StyledShapes>

1. Initialize currentGeometries = List<RemoteShape>()
2. Initialize currentGroups = List<Group>()
3. For each shape in shapes:
   a. If shape is GeometryShape (Path, Rectangle, Ellipse, PolyStar):
      - Evaluate RemoteShape with activeTrimPath.
      - Append RemoteShape to currentGeometries.
   b. If shape is Group:
      - If group contains internal styles:
        - Evaluate RemoteGroup recursively and append to shapeGroups (with NoopStyle).
      - Append Group to currentGroups for potential sibling style binding.
   c. If shape is Style (Fill, Stroke, GradientFill, GradientStroke):
      - If shape.hidden != true:
        - Evaluate RemoteStyle.
        - targetShapes = List<RemoteShape>()
        - If currentGeometries is not empty:
          - Append currentGeometries to targetShapes.
        - For each group in currentGroups:
          - Recursively evaluate and transform group geometries by group.transform into container space.
          - Append transformed geometries to targetShapes.
        - If targetShapes is not empty:
          - Append StyledShapes(targetShapes.toList(), RemoteStyle) to shapeGroups.
        - Clear currentGeometries.
4. Return shapeGroups reversed to maintain bottom-up painter stacking order.
```

## 03 Track Matte Layer Pairing Contract {#SP_LOTTIE_MATTE_03_01}

### 1. Composition Layer Filtering (`LottieAnimation.kt`)
In `LottieAnimation(animation, ...)`:
1. Scan `animation.layers` in list order.
2. Maintain `var pendingMatteSource: Layer? = null`.
3. For each `layer` in `animation.layers`:
   - If `layer.matteTarget == 1` (`td: 1`):
     - Record `pendingMatteSource = layer`.
     - Do not emit `Layer(layer, ...)` to the composable tree (suppress direct rendering).
   - Else:
     - If `layer.matteMode != MatteMode.Normal && pendingMatteSource != null`:
       - Construct `matteContext = MatteContext(pendingMatteSource, ancestorTransforms[pendingMatteSource.index] ?: emptyList())`.
       - Emit `Layer(layer, ancestorTransforms, matteContext = matteContext)`.
       - Reset `pendingMatteSource = null`.
     - Else:
       - Emit `Layer(layer, ancestorTransforms, matteContext = null)`.
       - Reset `pendingMatteSource = null`.

## 04 Canvas Clipping & Transform Propagation Contract {#SP_LOTTIE_MATTE_04_01}

### 1. ShapeLayer Canvas Clipping (`ShapeLayer.kt` / `Shape.kt`)
When rendering a `ShapeLayer` with an active `matteContext`:
1. `remoteCanvas.save()`.
2. Extract all `GraphicElement` items from `matteContext.matteLayer`.
3. Apply `matteContext.matteTransforms` to `remoteCanvas`:
   - For each transform in `matteTransforms`:
     - `transform(transform, null, animationSettings, remoteCanvas)`.
4. Apply clipping shape according to matte geometry:
   - For `Rectangle`:
     - Evaluate rectangle coordinates $(x, y, w, h)$.
     - Compute bounds: `left = x - w/2`, `top = y - h/2`, `right = x + w/2`, `bottom = y + h/2`.
     - Execute `remoteCanvas.clipRect(left, top, right, bottom)`.
   - For `Path`, `Ellipse`, `PolyStar`:
     - Evaluate `RemotePath`.
     - Execute `remoteCanvas.clipPath(remotePath)`.
5. Restore canvas transform state before drawing target shapes:
   - `remoteCanvas.restore()`.
6. Render target layer shapes with `RenderShapes(layer.shapes, transformStack)`.

## 05 Verification Criteria & Test Matrix {#SP_LOTTIE_MATTE_05_01}

### 1. Automated Functional Tests (`SP_LOTTIE_MATTE_05_02`)

| Test ID | Test Target | Input File | Expected Outcome |
|---|---|---|---|
| `SP_LOTTIE_MATTE_05_02_T1` | Sibling Group Fill | `volume_down.json` (Layer 2) | Speaker cone renders with solid white fill (`#FFFFFF`) and white stroke outline. |
| `SP_LOTTIE_MATTE_05_02_T2` | Matte Layer Suppression | `volume_down.json` (Layers 3, 5, 7) | "Cover 3", "Cover 2", and "Cover" opaque white rectangles are not rendered directly. |
| `SP_LOTTIE_MATTE_05_02_T3` | Matte Target Clipping | `volume_down.json` (Layers 4, 6, 8) | Ellipse rings are clipped by cover rectangles, rendering clean right-side concentric arcs `)))`. |
| `SP_LOTTIE_MATTE_05_02_T4` | Volume Up Parity | `volume_up.json` | `volume_up` renders solid speaker body and 3 clipped concentric sound wave arcs. |
| `SP_LOTTIE_MATTE_05_02_T5` | Multi-Progress Regression | Full suite | Roborazzi screenshot verification matches reference `lottie-android` outputs at 0%, 50%, 100% progress. |

## 06 Rollback & Failure Modes {#SP_LOTTIE_MATTE_06_01}

- **Non-Matted Layer Fallback:** If `layer.matteMode == MatteMode.Normal` or `matteContext == null`, `ShapeLayer` executes normal unclipped drawing without additional `save()`/`restore()` overhead.
- **Empty Matte Geometry:** If `matteContext.matteLayer` contains no evaluable geometries, clipping is bypassed and target shapes draw unclipped.
- **Rollback Strategy:** Revert changes to `gatherShapes()` and `LottieAnimation.kt` to restore baseline single-layer rendering.

## 07 Alternatives Considered {#SP_LOTTIE_MATTE_07_01}

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| **Offscreen Bitmap Masking** | Handles arbitrary opacity and luminance mattes | Allocates native bitmaps per frame, incompatible with RemoteCompose command buffer | Rejected |
| **Path Boolean Operations at Parse Time** | Avoids canvas clipping commands | Fails when matte layer has dynamic position/scale animation keyframes | Rejected |
| **Direct Canvas Geometry Clipping** | Native RemoteCompose command buffer support (`clipRect`/`clipPath`), zero extra heap allocations | Limited to Alpha/Inverted Alpha vector masks (sufficient for Lottie vector icons) | Accepted |
