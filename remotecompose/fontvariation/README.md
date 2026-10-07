# Remote Compose: variable-font axis animation

Two composables draw a line of text in a variable font with its axes (`ROND`, `wght`, `slnt`, …)
driven by `RemoteFloat`s. In both, the player never loads, re-instances or lays out a font.

```kotlin
val font = VariableFont.parse(context.resources.openRawResource(R.raw.my_font).readBytes())

// 1. Path tween: one axis, keyframe outlines tweened on the player.
RemoteVariableFontText(
  text = "Hello",
  font = font,
  axis = "ROND",
  value = roundness, // any RemoteFloat: a named float, an animation, a time expression
  fontSize = 32.rdp,
)

// 2. Expression path: any number of axes, the font's variation model evaluated on the player.
RemoteVariableFontExpressionText(
  text = "Hello",
  font = font,
  axes = mapOf("wght" to weight, "slnt" to slant),
  fontSize = 32.rdp,
)
```

## Why

Changing a font-variation setting at play time is expensive. The axis values can already be
`RemoteFloat`s on the wire (`CoreText` and the paint `FONT_AXIS` op both accept variables), but
each new value means a new typeface instance:

- the AndroidX player's `BuilderFontInstance.applyVariationSettings` rebuilds `Font` →
  `FontFamily` → `Typeface` (with system fallback) on every paint application, uncached, and
  generic families ignore the axes entirely;
- the CMP player (`rc-player-compose`) builds and caches one `FontFamily` per distinct value, so a
  continuously animated value grows that cache without bound.

Measured in `rc-player-compose` 2.1.2 (desktop JVM, CPU raster, "Hello" at 64 px): animating
`ROND` through `CoreText` cost about 22 ms per frame when the value changed every frame, against
about 2.5 ms for the path tween used here; with the value held still both were about 1 ms.

## How

Every outline coordinate of a variable font is the font's own linear model:
`default + Σ delta(region) × scalar(region, axes)`, where each region scalar is a product of
per-axis "tents" over the normalized axis values. That makes outlines piecewise linear in each
axis, and the two approaches use that in different ways.

### 1. Path tween (`RemoteVariableFontText`)
 Their deltas only change slope where a
`gvar` or `HVAR` region starts, peaks or ends (or where `avar` bends the mapping). So, with the
other axes held fixed:

1. `axisKeyframes` collects those breakpoints for just the glyphs in the text. Google Sans Flex
   needs 2 for `ROND` and 9 for `wght`.
2. `VariableFont` (a small `glyf`/`gvar`/`HVAR`/`avar`/`cmap` reader) produces the text's
   outline at each keyframe. Every outline is emitted from the font's own point list, so all
   keyframes have exactly the same path structure, which `DrawTweenPath` requires.
3. The document draws `drawTweenPath(keyframe[i], keyframe[i + 1], t)` for the segment that
   contains the value, chosen with `drawConditionally`.

The result is the font's own outline at every value, not an approximation. `AxisKeyframesTest`
checks the tween against the font at 201 values per axis, and `VariableFontTest` checks the reader
against `fontTools` to 0.01 font units.

### 2. Expression path (`RemoteVariableFontExpressionText`)

The document carries the model itself, as Remote Compose float expressions:

1. Each animated axis is normalized once: the font's user-to-normalized mapping, `avar` included,
   written as a sum of clamped ramps.
2. Each distinct product of tents is written once and shared; axes that are not animated are
   folded into the coefficients at creation time, and regions that cannot move drop out.
3. Each distinct outline coordinate is one expression, `constant + Σ coefficient × scalar`.
4. The path is written with `PathCreate`/`PathAppend`, whose coordinates are those expressions'
   ids. These are paint operations, so the player rebuilds the path from the current values on
   each paint. (A `RemotePath` is `PathData`, which reads its variables once, at load.)

Several axes can move at once and independently, and the document needs no keyframes: it grows
with the number of distinct coordinates and regions in the text. The text's box is its widest
advance over the animated range, computed exactly at creation time, so animating never reflows
the layout.

`VariationModelTest` checks the expressions' model against the font everywhere in each test
font's design space, and `RenderFidelityTest` renders both approaches and the platform's own text
for every tested axis of six fonts (Google Sans Flex, Roboto Flex, Recursive, Fraunces, Noto
Sans, Inter): the tween and the expression path agree to within antialiasing, and both match the
platform glyph for glyph. `LiveUpdateTest` checks both follow a float changed after load.

## Limits

- One line, placed by nominal advances: no kerning, ligatures or complex-script shaping.
- No font fallback: characters the font lacks draw as `.notdef`.
- TrueType (`glyf`) outlines only, not CFF2. Composite glyphs must place their components by
  offset; a composite that anchors a component by point matching throws when it is drawn.
- The text is fixed when the document is created, and it is drawn as a path: there is no
  accessible text unless the caller adds a content description, and edge anti-aliasing is that
  of a path fill rather than the platform's text rasterizer.
- The path tween animates one axis; the others are fixed by `location`. The expression path
  animates any set of axes.
