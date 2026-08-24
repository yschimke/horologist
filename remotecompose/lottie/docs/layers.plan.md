# Implementation Plan: Layers Architecture & Lottie 1.0.1 Compliance {#PL_LOTTIE_LAYERS}

> **Code:** PL_LOTTIE_LAYERS
> **Status:** completed
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Layers Concept](layers.concept.md)
> **Specification:** [Layers Specification](layers.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Layers](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-layers)
> **Depends on:** none
> **Used by:** `format/layer`, `renderer/layers`, `MediaLottieDiffScreenshotTest`, `LottieFeatureDiffScreenshotTest`
>
> Implementation plan for Lottie Layers specification compliance and modular architecture in `:remotecompose:lottie`: separating monolithic `format/Layers.kt` and `renderer/Layer.kt` into dedicated per-file models under `format/layer/` and `renderer/layers/` with co-located serializers, aligning timing properties to floating-point numbers (`Float?`), implementing timeline active frame window visibility checks ($[ip, op)$), supporting `SolidColorLayer` rendering, and ensuring unknown layers preserve transform hierarchies for child layers.

## Goal

Align all Lottie Layers with the [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-layers) guided by three strict priorities:
1. **Priority 1 (Refactor Existing Codebase):**
   - Modularize `format/Layers.kt` and `format/LottieDecoder.kt` layer serializers into `com.google.android.horologist.remotecompose.lottie.format.layer`:
     - `Layer.kt` (sealed class `Layer`, `LayerType` enum, `LayerSerializer`, `LayerTypeSerializer`).
     - `ShapeLayer.kt` (`data class ShapeLayer : Layer`).
     - `NullLayer.kt` (`data class NullLayer : Layer`).
     - `SolidColorLayer.kt` (`data class SolidColorLayer : Layer`, `sc: String`, `sw: Float`, `sh: Float`).
     - `PrecompLayer.kt` (`data class PrecompLayer : Layer`, `refId: String`, `w: Float?`, `h: Float?`, `tm: BaseScalarProperty?`).
     - `ImageLayer.kt` (`data class ImageLayer : Layer`, `refId: String`).
     - `TextLayer.kt` (`data class TextLayer : Layer`).
     - `UnknownLayer.kt` (`data class UnknownLayer : Layer`).
     - `BlendMode.kt` (`enum class BlendMode`, `BlendModeSerializer`).
     - `MatteMode.kt` (`enum class MatteMode`, `MatteModeSerializer`).
     - Co-locate each serializer in the exact file of the model/enum it serves.
   - Modularize `renderer/Layer.kt` into `com.google.android.horologist.remotecompose.lottie.renderer.layers`:
     - `Layer.kt` (dispatcher, timeline window $[ip, op)$ checks, `hidden` check, transform accumulation).
     - `ShapeLayer.kt` (`RenderShapeLayer`, delegating to `RenderShapes`).
     - `SolidColorLayer.kt` (`RenderSolidColorLayer`, drawing solid color quad via `RemoteCanvas`).
2. **Priority 2 (Fix Problems & Complete Existing Layers):**
   - Change `ip` (`startFrame`) and `op` (`endFrame`) types from `Int?` to `Float?` to match Lottie 1.0.1 floating-point specification.
   - Add `st: Float?` (`startTime`) and `sr: Float?` (`timeStretch`).
   - Add metadata fields `ao: Int?` (Auto-Orient), `bm: BlendMode?` (BlendMode), `tt: MatteMode?` (MatteMode), `tp: Int?` (MatteParent), `td: Int?` (MatteTarget), `ddd: Int?` (3D).
   - Enforce active timeline window in renderer: skip rendering when $currentFrame < ip$ or $currentFrame \ge op$.
   - Retain unrecognized layer types in AST as `UnknownLayer` with `ind`, `parent`, `ks`, `ip`, `op`, `st`, `sr` to preserve parent transform chains for child layers.
3. **Priority 3 (Add Missing Elements & Rendering):**
   - Implement `SolidColorLayer` (`ty: 1`) format model and Remote Compose rendering.
   - Expand AST layer models (`PrecompLayer`, `ImageLayer`, `TextLayer`).
   - Verify that all existing and new layer test cases pass cleanly.

---

## Architectural Design & Package Structure

```
remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/
├── format/
│   ├── layer/
│   │   ├── Layer.kt            # Base Layer sealed class, LayerType enum, LayerSerializer, LayerTypeSerializer
│   │   ├── ShapeLayer.kt       # ShapeLayer ("ty": 4, shapes: List<GraphicElement>)
│   │   ├── NullLayer.kt        # NullLayer ("ty": 3)
│   │   ├── SolidColorLayer.kt  # SolidColorLayer ("ty": 1, sc: String, sw: Float, sh: Float)
│   │   ├── PrecompLayer.kt     # PrecompLayer ("ty": 0, refId: String, w: Float?, h: Float?, tm: BaseScalarProperty?)
│   │   ├── ImageLayer.kt       # ImageLayer ("ty": 2, refId: String)
│   │   ├── TextLayer.kt        # TextLayer ("ty": 5)
│   │   ├── UnknownLayer.kt     # UnknownLayer (resilient fallback preserving transform hierarchy)
│   │   ├── BlendMode.kt        # BlendMode enum (0..17) and BlendModeSerializer
│   │   └── MatteMode.kt        # MatteMode enum (0..4) and MatteModeSerializer
│   ├── Animation.kt            # Top-level Animation holding List<Layer>
│   └── LottieDecoder.kt        # Root decoder
└── renderer/
    ├── layers/
    │   ├── Layer.kt            # Layer dispatcher composable, [ip, op) active window check, transform chaining
    │   ├── ShapeLayer.kt       # ShapeLayer composable delegating to RenderShapes
    │   └── SolidColorLayer.kt  # SolidColorLayer composable rendering solid colored quad via RemoteCanvas
    ├── shapes/                 # Geometry evaluators (Path, Rectangle, Ellipse, PolyStar)
    ├── RemoteShape.kt
    ├── RemoteStyle.kt
    ├── Shape.kt                # RenderShapes, gatherShapes, group
    └── Transform.kt
```

---

## Phases

### Phase 1 — Test Harness & Resilience Assertions {#PL_LOTTIE_LAYERS_P1}
- **Verify:** `SP_LOTTIE_LAYERS_05_01`, `SP_LOTTIE_LAYERS_05_02`
- Add unit tests in `ParsingTest.kt` for deserialization of:
  - `ShapeLayer` with float `ip` and `op` timings (e.g. `12.5f`, `45.0f`).
  - `SolidColorLayer` (`ty: 1`) with `sc` hex color, `sw` width, and `sh` height.
  - `NullLayer` (`ty: 3`) with parent index and transform.
  - `PrecompLayer` (`ty: 0`) with `refId`, `w`, `h`, and animated `tm`.
  - `ImageLayer` (`ty: 2`) with `refId`.
  - `TextLayer` (`ty: 5`).
  - `BlendMode` and `MatteMode` integer enums.
- Add unit tests in `LottieDecoderResilienceTest.kt` for:
  - Unknown layer type `"ty": 999` deserializing into `UnknownLayer` while preserving `ind`, `parent`, `ks`, `ip`, `op`.
  - Parenting hierarchy retention when a child layer links to an `UnknownLayer`.

### Phase 2 — Priority 1: Format Layer Category Package Modularization (`format/layer/`) {#PL_LOTTIE_LAYERS_P2}
- **Verify:** `SP_LOTTIE_LAYERS_05_01`
- Create `format/layer/Layer.kt` defining sealed class `Layer`, `LayerType` enum, `LayerSerializer`, and `LayerTypeSerializer`.
- Create per-file layer models with co-located serializers:
  - `format/layer/ShapeLayer.kt`
  - `format/layer/NullLayer.kt`
  - `format/layer/SolidColorLayer.kt`
  - `format/layer/PrecompLayer.kt`
  - `format/layer/ImageLayer.kt`
  - `format/layer/TextLayer.kt`
  - `format/layer/UnknownLayer.kt`
  - `format/layer/BlendMode.kt`
  - `format/layer/MatteMode.kt`
- Update `format/Animation.kt`, `format/LottieDecoder.kt`, and `LottieAnimation.kt` to import from `format.layer.*`.
- Remove obsolete `format/Layers.kt`.

### Phase 3 — Priority 1: Renderer Layer Modularization (`renderer/layers/`) {#PL_LOTTIE_LAYERS_P3}
- **Verify:** `SP_LOTTIE_LAYERS_05_03`
- Create `renderer/layers/Layer.kt` containing top-level `Layer(...)` dispatcher composable.
- Create `renderer/layers/ShapeLayer.kt` containing `ShapeLayer(...)` composable delegating to `RenderShapes`.
- Create `renderer/layers/SolidColorLayer.kt` containing `SolidColorLayer(...)` composable.
- Update `LottieAnimation.kt` to call modular `renderer.layers.Layer`.
- Remove obsolete monolithic `renderer/Layer.kt`.

### Phase 4 — Priority 2 & 3: Timeline Active Window & Dynamic Layer Visibility {#PL_LOTTIE_LAYERS_P4}
- **Verify:** `SP_LOTTIE_LAYERS_05_02`, `SP_LOTTIE_LAYERS_05_03`
- Implement timeline active frame range check ($ip \le t < op$) in `renderer/layers/Layer.kt` using both static short-circuiting and dynamic `selectIfLt` expressions (`layerVisibility = isAfterStart * isBeforeEnd`).
- Pass `layerVisibility: RemoteFloat` to `ShapeLayer`, `SolidColorLayer`, and `RenderShapes`, multiplying into `layerOpacity` and paint alpha.
- Implement solid quad drawing in `renderer/layers/SolidColorLayer.kt` via `RemoteCanvas` with accumulated transforms and modulated opacity.

### Phase 5 — Full Verification & Roborazzi Screenshot Suite {#PL_LOTTIE_LAYERS_P5}
- **Verify:** `SP_LOTTIE_LAYERS_05_04`
- Run mandatory verification commands in order:
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:check`
  7. `./gradlew :remotecompose:lottie:verifyRoborazziDebug`

---

## Alternatives Considered

### 1. Single Monolithic File vs Modular `format/layer/` Sub-Package
- **Option A (Single monolithic `format/Layers.kt`)**: Monolithic file with multiple models, violating SRP.
- **Option B (Modular `format/layer/` package with 1 file per layer type) [Selected]**: Clean separation of concerns, high locality of reference, symmetric with `format/graphicelement/`.

### 2. Discard Unknown Layers vs AST `UnknownLayer` Retention
- **Option A (Discard unrecognized layers)**: Breaks spatial transforms for child layers referencing the unknown layer.
- **Option B (Preserve in `UnknownLayer` with transform and parenting metadata) [Selected]**: Retains transform hierarchy integrity, completely resilient.

### 3. Dedicated NullLayer Renderer vs Dispatcher Direct No-Op
- **Option A (Create empty `renderer/layers/NullLayer.kt` composable)**: Redundant empty composable file.
- **Option B (Direct no-op in `renderer/layers/Layer.kt` dispatcher) [Selected]**: NullLayer is non-visual; its spatial transforms are pre-accumulated in ancestor graph in `LottieAnimation.kt`.
