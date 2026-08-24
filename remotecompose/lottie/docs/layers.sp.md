# Specification: Layers Architecture & Lottie 1.0.1 Compliance {#SP_LOTTIE_LAYERS}

> **Code:** SP_LOTTIE_LAYERS
> **Status:** active
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Layers Concept](layers.concept.md)
> **Depends on:** [Format Concept](format.concept.md), [Renderer Concept](renderer.concept.md), [Shapes Spec](shapes.sp.md)
> **Used by:** [Layers Plan](layers.plan.md), `format/layer`, `renderer/layers`
>
> Formal specification for Lottie Layers modular architecture under `format/layer/` and `renderer/layers/`, co-located serialization contracts, floating-point timeline visibility, transform parenting accumulation, and Solid Color Layer rendering according to [Lottie 1.0.1 Layers](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-layers).

## 01 Data Structures & Package Layout {#SP_LOTTIE_LAYERS_01_01}

### 1. Root Layer Base & Serializers (`format/layer/Layer.kt`)
```kotlin
package com.google.android.horologist.remotecompose.lottie.format.layer

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import kotlinx.serialization.Serializable

@Serializable(with = LayerSerializer::class)
internal sealed class Layer {
  abstract val name: String?
  abstract val hidden: Boolean?
  abstract val type: LayerType
  abstract val index: Int?
  abstract val parent: Int?
  abstract val startFrame: Float?
  abstract val endFrame: Float?
  abstract val startTime: Float?
  abstract val timeStretch: Float?
  abstract val transform: Transform?
  abstract val autoOrient: Int?
  abstract val blendMode: BlendMode?
  abstract val matteMode: MatteMode?
  abstract val matteParent: Int?
  abstract val matteTarget: Int?
  abstract val is3d: Int?
}

@Serializable(with = LayerTypeSerializer::class)
internal enum class LayerType(val value: Int) {
  Precomposition(0),
  Solid(1),
  Image(2),
  Null(3),
  Shape(4),
  Text(5),
  Audio(6),
  Unknown(-1);

  companion object {
    fun fromValueOrNull(value: Int): LayerType? = values().firstOrNull { it.value == value }
  }
}
```

### 2. Specific Layer Models (`format/layer/`) {#SP_LOTTIE_LAYERS_01_02}

- **`ShapeLayer.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.layer

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  internal data class ShapeLayer(
    @SerialName("nm") override val name: String? = "",
    @SerialName("hd") override val hidden: Boolean? = false,
    @SerialName("ty") override val type: LayerType = LayerType.Shape,
    @SerialName("ind") override val index: Int? = null,
    @SerialName("parent") override val parent: Int? = null,
    @SerialName("ip") override val startFrame: Float? = null,
    @SerialName("op") override val endFrame: Float? = null,
    @SerialName("st") override val startTime: Float? = 0f,
    @SerialName("sr") override val timeStretch: Float? = 1f,
    @SerialName("ks") override val transform: Transform? = null,
    @SerialName("ao") override val autoOrient: Int? = 0,
    @SerialName("bm") override val blendMode: BlendMode? = BlendMode.Normal,
    @SerialName("tt") override val matteMode: MatteMode? = MatteMode.Normal,
    @SerialName("tp") override val matteParent: Int? = null,
    @SerialName("td") override val matteTarget: Int? = 0,
    @SerialName("ddd") override val is3d: Int? = 0,
    @SerialName("shapes") val shapes: List<GraphicElement> = emptyList(),
  ) : Layer()
  ```

- **`SolidColorLayer.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.layer

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  internal data class SolidColorLayer(
    @SerialName("nm") override val name: String? = "",
    @SerialName("hd") override val hidden: Boolean? = false,
    @SerialName("ty") override val type: LayerType = LayerType.Solid,
    @SerialName("ind") override val index: Int? = null,
    @SerialName("parent") override val parent: Int? = null,
    @SerialName("ip") override val startFrame: Float? = null,
    @SerialName("op") override val endFrame: Float? = null,
    @SerialName("st") override val startTime: Float? = 0f,
    @SerialName("sr") override val timeStretch: Float? = 1f,
    @SerialName("ks") override val transform: Transform? = null,
    @SerialName("ao") override val autoOrient: Int? = 0,
    @SerialName("bm") override val blendMode: BlendMode? = BlendMode.Normal,
    @SerialName("tt") override val matteMode: MatteMode? = MatteMode.Normal,
    @SerialName("tp") override val matteParent: Int? = null,
    @SerialName("td") override val matteTarget: Int? = 0,
    @SerialName("ddd") override val is3d: Int? = 0,
    @SerialName("sc") val solidColor: String,
    @SerialName("sw") val solidWidth: Float,
    @SerialName("sh") val solidHeight: Float,
  ) : Layer()
  ```

- **`NullLayer.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.layer

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  internal data class NullLayer(
    @SerialName("nm") override val name: String? = "",
    @SerialName("hd") override val hidden: Boolean? = false,
    @SerialName("ty") override val type: LayerType = LayerType.Null,
    @SerialName("ind") override val index: Int? = null,
    @SerialName("parent") override val parent: Int? = null,
    @SerialName("ip") override val startFrame: Float? = null,
    @SerialName("op") override val endFrame: Float? = null,
    @SerialName("st") override val startTime: Float? = 0f,
    @SerialName("sr") override val timeStretch: Float? = 1f,
    @SerialName("ks") override val transform: Transform? = null,
    @SerialName("ao") override val autoOrient: Int? = 0,
    @SerialName("bm") override val blendMode: BlendMode? = BlendMode.Normal,
    @SerialName("tt") override val matteMode: MatteMode? = MatteMode.Normal,
    @SerialName("tp") override val matteParent: Int? = null,
    @SerialName("td") override val matteTarget: Int? = 0,
    @SerialName("ddd") override val is3d: Int? = 0,
  ) : Layer()
  ```

- **`PrecompLayer.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.layer

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
  import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  internal data class PrecompLayer(
    @SerialName("nm") override val name: String? = "",
    @SerialName("hd") override val hidden: Boolean? = false,
    @SerialName("ty") override val type: LayerType = LayerType.Precomposition,
    @SerialName("ind") override val index: Int? = null,
    @SerialName("parent") override val parent: Int? = null,
    @SerialName("ip") override val startFrame: Float? = null,
    @SerialName("op") override val endFrame: Float? = null,
    @SerialName("st") override val startTime: Float? = 0f,
    @SerialName("sr") override val timeStretch: Float? = 1f,
    @SerialName("ks") override val transform: Transform? = null,
    @SerialName("ao") override val autoOrient: Int? = 0,
    @SerialName("bm") override val blendMode: BlendMode? = BlendMode.Normal,
    @SerialName("tt") override val matteMode: MatteMode? = MatteMode.Normal,
    @SerialName("tp") override val matteParent: Int? = null,
    @SerialName("td") override val matteTarget: Int? = 0,
    @SerialName("ddd") override val is3d: Int? = 0,
    @SerialName("refId") val refId: String = "",
    @SerialName("w") val width: Float? = null,
    @SerialName("h") val height: Float? = null,
    @SerialName("tm") val timeRemap: BaseScalarProperty? = null,
  ) : Layer()
  ```

- **`ImageLayer.kt`:** `data class ImageLayer(...) : Layer` (`refId: String = ""`)
- **`TextLayer.kt`:** `data class TextLayer(...) : Layer`
- **`UnknownLayer.kt`:** `data class UnknownLayer(...) : Layer`
- **`BlendMode.kt`:** `enum class BlendMode(val value: Int)`, `BlendModeSerializer`
- **`MatteMode.kt`:** `enum class MatteMode(val value: Int)`, `MatteModeSerializer`

### 3. Renderer Package Layout (`renderer/layers/`) {#SP_LOTTIE_LAYERS_01_03}

- **`Layer.kt`:**
  ```kotlin
  @Composable
  @RemoteComposable
  internal fun Layer(
    layer: Layer,
    parentTransforms: Map<Int, List<Transform>>,
    transform: Transform?,
  )
  ```
  Evaluates active timeline window $[startFrame, endFrame)$, `hidden == true`, transform stack, and delegates to `ShapeLayer`, `SolidColorLayer`, or no-op.

- **`ShapeLayer.kt`:**
  ```kotlin
  @Composable
  @RemoteComposable
  internal fun ShapeLayer(layer: ShapeLayer, transformStack: List<Transform?>? = null)
  ```

- **`SolidColorLayer.kt`:**
  ```kotlin
  @Composable
  @RemoteComposable
  internal fun SolidColorLayer(layer: SolidColorLayer, transformStack: List<Transform?>? = null)
  ```

## 02 Contracts {#SP_LOTTIE_LAYERS_02_01}

### Contract 1: Polymorphic Layer Deserialization (`LayerSerializer`)
- **Input:** JSON element representing a layer.
- **Output:** Concrete `Layer` subclass instance (`ShapeLayer`, `NullLayer`, `SolidColorLayer`, `PrecompLayer`, `ImageLayer`, `TextLayer`, `UnknownLayer`).
- **Error Handling / Fallback:** If `"ty"` is unrecognized or missing, returns `UnknownLayer` with parsed `ind`, `parent`, `ks`, and timing fields.

### Contract 2: Layer Timeline Visibility Evaluation
- **Input:** `layer: Layer`, `currentFrame: RemoteFloat`.
- **Output:** `RemoteFloat` `layerVisibility` modulating layer opacity and paint alpha.
- **Rule:** Active if and only if $\text{hidden} \ne \text{true} \land \text{startFrame} \le \text{currentFrame} < \text{effectiveEndFrame}$.
- **Dynamic Formulation:** Evaluated as `val layerVisibility = selectIfLt(currentFrame, startFrame.rf, 0f.rf, 1f.rf) * selectIfLt(currentFrame, effectiveEndFrame.rf, 1f.rf, 0f.rf)`, where `effectiveEndFrame = if (endFrame >= compositionEndFrame) endFrame + 0.01f else endFrame`. When `currentFrame.constantValueOrNull` is available, static bounds filtering short-circuits execution.

### Contract 3: Solid Color Layer Rendering (`SolidColorLayer.kt`)
- **Input:** `SolidColorLayer` with `sc` (Hex string), `sw`, `sh`, `transformStack`.
- **Output:** Remote Compose drawing commands executing `drawRect` with solid color and transform/opacity applied.

## 03 Validation Rules & Mathematical Invariants {#SP_LOTTIE_LAYERS_03_01}

1. **Timeline Interval Invariant:** $t \in [ip, op)$. If $op$ is omitted, defaults to animation timeline end frame. If $ip$ is omitted, defaults to $0.0f$.
2. **Local Frame Transform Invariant:** $t_{\text{local}} = (t_{\text{comp}} - st) / sr$.
3. **Parent Transform Invariant:** Spatial transforms accumulate from root ancestor to child. Opacity values are isolated and do not compound across parent layers.
4. **Hex Color Invariant:** `sc` string format `#RRGGBB` or `#AARRGGBB` parsed to 32-bit ARGB color integer.

## 04 Integration Scenarios {#SP_LOTTIE_LAYERS_04_01}

1. **Scenario 1: Grandchild Parenting with Deep Hierarchy**
   - Layer chain `20 -> 19 -> ... -> 1` inherits accumulated spatial transforms accurately down the chain without losing ancestors (`LottieFeatureDiffScreenshotTest.parentChain`).
2. **Scenario 2: Solid Color Background Drawing**
   - Animation with `SolidColorLayer` (`ty: 1`) renders a solid rectangle of dimensions `sw` $\times$ `sh` with hex fill `sc`.
3. **Scenario 3: Unknown Layer Ingestion with Parenting Preservation**
   - Animation with unknown `ty: 999` parent node connected to child `ShapeLayer` preserves the transform chain for the child shape layer.

## 05 Verification Criteria {#SP_LOTTIE_LAYERS_05_01}

- `SP_LOTTIE_LAYERS_05_01`: Unit tests in `ParsingTest.kt` verifying deserialization of all layer types (`Shape`, `Null`, `Solid`, `Precomp`, `Image`, `Text`, `Unknown`) and enums (`BlendMode`, `MatteMode`).
- `SP_LOTTIE_LAYERS_05_02`: Resilience tests verifying floating-point `ip`/`op`/`st`/`sr` timings, missing fields, and `UnknownLayer` fallback.
- `SP_LOTTIE_LAYERS_05_03`: Roborazzi screenshot tests in `LottieFeatureDiffScreenshotTest` (`parentChain`, `positionStatic`, `rectEllipse`, `polystar`) passing cleanly.
- `SP_LOTTIE_LAYERS_05_04`: Full Gradle check suite (`check`, `ktfmtFormat`, `compileDebugKotlin`, `testDebugUnitTest`) passing without warnings or errors.

## 06 Rollback Strategy {#SP_LOTTIE_LAYERS_06_01}

Revert modular layer files to the previous baseline commit state without modifying public API contracts.

## 07 Alternatives Considered {#SP_LOTTIE_LAYERS_07_01}

| # | Approach | Pros | Cons | Verdict |
|---|----------|------|------|---------|
| 1 | Single monolithic file for all layers (`format/Layers.kt`) | Single file | Tight coupling, 300+ lines | Rejected |
| 2 | Modular `format/layer/` package with 1 file per layer type + co-located serializers | Single responsibility, high cohesion, small files (~30-60 lines), symmetric with `graphicelement/` | Multiple files | Chosen |
| 3 | Dedicated `renderer/layers/NullLayer.kt` renderer file | 1:1 filename symmetry | Redundant empty composable with dead code | Rejected |
| 4 | Dispatcher no-op for `NullLayer` in `renderer/layers/Layer.kt` | Zero overhead, transforms pre-accumulated in ancestor graph | None | Chosen |
