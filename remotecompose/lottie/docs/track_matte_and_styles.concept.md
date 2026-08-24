# Concept: Track Matte Masking & Container Style Resolution {#C_LOTTIE_MATTE}

> **Code:** C_LOTTIE_MATTE
> **Status:** active
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Depends on:** `C_LOTTIE_LAYERS`, `C_LOTTIE_SHAPES`, `C_RND`
> **Used by:** `SP_LOTTIE_MATTE`, `PL_LOTTIE_MATTE`, `LottieAnimation`, `ShapeLayer`, `Shape.kt`
>
> Concept definition for Track Matte layer masking and container-level shape style scope resolution in the RemoteCompose Lottie renderer to achieve visual parity for media icons (`volume_down`, `volume_up`).

## 01 Goal & Problem Statement {#C_LOTTIE_MATTE_01_01}

Visual comparison between `:remotecompose:lottie` and reference `lottie-android` for media animations (`volume_down.json`, `volume_up.json`) shows two visual rendering defects:
1. **Unfilled Geometry in Container Scope:** In animations where a shape geometry is defined inside a child group and its corresponding `Fill` style is defined at the container (layer) root, the renderer drops the fill style and renders only the stroke outline.
2. **Opaque Matte Rectangles & Unmasked Geometries:** In animations using track mattes (`td: 1` on matte source layers, `tt: 1` on matte target layers), the renderer draws matte source rectangles as visible opaque content and renders target layers without clipping masks.

The goal is to implement container-level style scope resolution and layer track matte canvas clipping in `:remotecompose:lottie` without requiring changes to the RemoteCompose native player.

## 02 Architectural Mechanisms {#C_LOTTIE_MATTE_01_02}

```
+-------------------------------------------------------------------------+
| Lottie Animation Layer Pipeline                                         |
|                                                                         |
|  [ Layer i (td=1) ]  -- (Suppressed from Direct Draw) --> [ Matte Mask ]|
|                                                                  |      |
|  [ Layer i+1 (tt=1) ] <------------------------------------------+      |
|           |                                                             |
|           v                                                             |
|  [ ShapeLayer Renderer ]                                                |
|           |                                                             |
|           +--> remoteCanvas.save()                                      |
|           +--> Apply Matte Clip: remoteCanvas.clipRect / clipPath       |
|           +--> [ Sibling Style Scope Resolution in gatherShapes() ]     |
|           |         +-> Group Geometries + Root Fill/Stroke Styles      |
|           |         +-> Draw Styled Shapes                              |
|           +--> remoteCanvas.restore()                                   |
+-------------------------------------------------------------------------+
```

### 1. Sibling & Container Style Scope Resolution
In Lottie AST structure, style elements (`Fill`, `Stroke`, `GradientFill`, `GradientStroke`) apply to all geometry elements preceding them in the same container, including geometries encapsulated within preceding sibling `Group` elements. The shape collection algorithm must accumulate geometries across sibling groups within the active container scope before binding downstream styles.

### 2. Track Matte Layer Pairing & Geometry Clipping
In Lottie specification:
- A layer declaring `matteTarget == 1` (`td: 1`) is a **Matte Source Layer**. The renderer suppresses direct output of the matte source layer to the display tree.
- A layer declaring `matteMode != MatteMode.Normal` (`tt != 0`) is a **Matte Target Layer**. The renderer binds the preceding matte source layer to the target layer.
- During target layer rendering, the target layer evaluates the matte source's geometry under the matte source's transform hierarchy and executes canvas clipping (`clipRect` or `clipPath`) within a `save()` / `restore()` block on `RemoteCanvas`.

## 03 Scope & Boundaries {#C_LOTTIE_MATTE_01_03}

### In Scope
- Container-level style binding across sibling `Group` geometries in `gatherShapes()`.
- Alpha Track Matte (`MatteMode.Alpha`, `tt: 1`) layer pairing in `LottieAnimation.kt`.
- Rectangle and Path canvas clipping for matted layers using `RemoteCanvas.clipRect` and `RemoteCanvas.clipPath`.
- Verification against `volume_down.json` and `volume_up.json` in `MediaLottieDiffScreenshotTest`.

### Out of Scope
- Luma and Inverted Luma track mattes requiring off-screen pixel luminance extraction.
- Nested pre-composition track mattes spanning multiple composition trees.
- Native player compositing modifiers requiring off-screen layer allocation.

## 04 Pre-Concept Checklist {#C_LOTTIE_MATTE_01_04}
- **Does RemoteCanvas support clipping?** Yes, `RemoteCanvas` exposes `clipRect` and `clipPath` operations.
- **Does canvas clipping work without native player changes?** Yes, canvas clipping instructions serialize directly into the RemoteCompose command buffer (`CLIP_RECT = 39`, `CLIP_PATH = 38`).
- **Does container style accumulation break existing tests?** No, animations with isolated group styles retain their local styles; container styles only bind to geometries in their enclosing scope.

## 05 Dependencies {#C_LOTTIE_MATTE_01_05}
- `remotecompose/lottie/format/layer/Layer.kt` (`Layer`, `matteTarget`, `matteMode`)
- `remotecompose/lottie/format/layer/MatteMode.kt` (`MatteMode`)
- `remotecompose/lottie/LottieAnimation.kt` (`buildAncestorTransforms`, layer iteration)
- `remotecompose/lottie/renderer/layers/ShapeLayer.kt` (`ShapeLayer`)
- `remotecompose/lottie/renderer/Shape.kt` (`RenderShapes`, `gatherShapes`)

## 06 Design Decisions {#C_LOTTIE_MATTE_DEC_01}

| Decision ID | Choice | Rationale | Alternatives Rejected |
|---|---|---|---|
| `C_LOTTIE_MATTE_DEC_01` | Canvas-level clipping (`clipRect` / `clipPath`) for Alpha track mattes | Direct support in `RemoteCanvas` command stream without engine allocations. | Porter-Duff blend modes (`saveLayer` + `DST_IN`), parse-time geometry boolean subtraction. |
| `C_LOTTIE_MATTE_DEC_02` | Recursive geometry collection in container `gatherShapes()` | Preserves AST hierarchy while allowing outer styles to bind to inner sibling paths. | Flattening all groups at decode time, duplicating style nodes into every group. |

## 07 Alternatives Considered {#C_LOTTIE_MATTE_01_06}

### Alternative 1: Porter-Duff Blend Modes (`saveLayer` + `DST_IN`)
- **Pros:** Supports arbitrary alpha and luminance gradients.
- **Cons:** RemoteCompose command buffer does not expose `saveLayer` with custom blend modes to composable callers.
- **Decision:** Rejected. Canvas geometry clipping satisfies vector track matte requirements directly.

### Alternative 2: Parse-Time Path Boolean Subtraction
- **Pros:** Computes static clipped paths once during JSON decode.
- **Cons:** Fails on dynamic animated transforms, keyframed scale/position, and complex Bézier curves.
- **Decision:** Rejected. Dynamic canvas clipping during render evaluation is required for animated timelines.
