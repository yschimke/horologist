# Implementation Plan: Color Value Specification Compliance & `RemoteColor` Migration  {#PL_LOTTIE_COLOR_VALUE}

> **Code:** PL_LOTTIE_COLOR_VALUE
> **Status:** completed
> **Created:** 2026-08-21
> **Updated:** 2026-08-21
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Depends on:** none
> **Used by:** `renderer`, `format`
>
> Implementation plan for Lottie Color Value specification compliance: 4-component alpha support (array and hex), animated color properties, AST modeling via `RemoteColor`, extraction to `format/properties/Color.kt`, and frame interpolation.

## Goal

Ensure compliance for Lottie Color values:
1. Support Hex colors (`#RRGGBB`, `#AARRGGBB`, `#RGB`, `#ARGB`).
2. Support 4-component float/integer array colors with alpha (`[r, g, b, a]`).
3. Support animated color properties with keyframes (`a: 1`).
4. Model colors using `RemoteColor` directly in `format/properties/Color.kt`.
5. Provide frame interpolation for animated colors in `renderer/Animation.kt`.

## Phases

### Phase 1 — Test Harness & Failing Assertions [DONE]  {#PL_LOTTIE_COLOR_P1}
- Add failing unit tests in `LottieDecoderResilienceTest.kt` / `ParsingTest.kt` for Hex color strings (3, 4, 6, 8 characters), 4-component alpha arrays, and animated color keyframes.

### Phase 2 — `RemoteColor` Extraction to `format/properties/Color.kt` [DONE]  {#PL_LOTTIE_COLOR_P2}
- Create `format/properties/Color.kt` defining `BaseColorProperty`, `StaticColorProperty`, `AnimatedColorProperty`, and `ColorPropertyKeyframe` holding `RemoteColor`.
- Remove `StaticColorProperty` from `format/Properties.kt`.
- Update `GraphicElement.Fill` to take `BaseColorProperty`.

### Phase 3 — Deserialization Implementation [DONE]  {#PL_LOTTIE_COLOR_P3}
- Implement `BaseColorPropertySerializer`, `StaticColorPropertySerializer` (with Hex and 4-component alpha parsing), and `ColorPropertyKeyframeSerializer` in `format/properties/Color.kt`.
- Remove legacy color serializer from `format/LottieDecoder.kt`.

### Phase 4 — Renderer Color Interpolation [DONE]  {#PL_LOTTIE_COLOR_P4}
- Implement `animateColor(BaseColorProperty, LottieSettings): RemoteColor` in `renderer/Animation.kt`.
- Integrate `animateColor` into `renderer/Shape.kt` `fill(...)`.

### Phase 5 — Verification & Checks [DONE]  {#PL_LOTTIE_COLOR_P5}
- Run `./gradlew :remotecompose:lottie:ktfmtFormat`
- Run `./gradlew :remotecompose:lottie:compileDebugKotlin`
- Run `./gradlew :remotecompose:lottie:testDebugUnitTest`
- Run `./gradlew :remotecompose:lottie:check`
