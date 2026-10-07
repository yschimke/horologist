# Remote Compose: variable-font axis animation

`RemoteVariableFontText` draws a line of text in a variable font with any of its axes (`wght`,
`slnt`, `ROND`, …) driven by `RemoteFloat`s, and the player never loads, re-instances or lays out a
font.

```kotlin
val font = VariableFont.parse(context.resources.openRawResource(R.raw.my_font).readBytes())

RemoteVariableFontText(
  text = "Hello",
  font = font,
  axes = mapOf("wght" to weight, "slnt" to slant), // any RemoteFloats
  fontSize = 32.rdp,
  location = mapOf("wdth" to 90f), // axes that don't move
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
about 2.5 ms for an outline path; with the value held still both were about 1 ms.

## How

Every outline coordinate of a variable font is the font's own linear model:
`default + Σ delta(region) × scalar(region, axes)`, where each region scalar is a product of
per-axis "tents" over the normalized axis values. The document carries that model as Remote
Compose float expressions:

1. `VariableFont` (a small `glyf`/`gvar`/`HVAR`/`avar`/`cmap` reader) turns the text into outlines
   whose coordinates are those linear forms.
2. Each animated axis is normalized once: the font's user-to-normalized mapping, `avar` included,
   written as a sum of clamped ramps.
3. Each distinct product of tents is written once and shared. Axes that are not animated are
   folded into the coefficients at creation time, and regions that cannot move drop out.
4. Each distinct outline coordinate is one expression, `constant + Σ coefficient × scalar`.
5. The path is an ordinary `RemotePath` whose coordinates are those expressions' ids; the player
   resolves them whenever the expressions change.

The result is the font's own outline at every value, not an approximation. The text's box is its
widest advance over the animated range, computed exactly at creation time, so animating never
reflows the layout.

## Driving the axes

The axes are any `RemoteFloat`s. Named floats that a host sets work, but nothing requires them: an
axis can be an expression of the document's own clock, so the document animates with no host
input at all. For example, weight and slant on their own periods:

```kotlin
fun sweep(from: Float, to: Float, periodSeconds: Float): RemoteFloat {
  val phase = (RemoteTime().ContinuousSec() % periodSeconds) / periodSeconds
  return (-abs(phase * 2f - 1f) + 1f) * (to - from) + from
}

RemoteVariableFontText(
  text = "Hello",
  font = robotoFlex,
  axes = mapOf("wght" to sweep(100f, 1000f, 4f), "slnt" to sweep(0f, -10f, 6f)),
  fontSize = 32.rdp,
)
```

## Cost

The document grows with the distinct coordinates and regions in the text, and fastest with the
number of axes animated together, since regions that span several axes multiply. Roboto Flex, in
`rc-player-compose` on the desktop JVM, warm:

| Text | Animated axes | Document | Frame |
| --- | --- | --- | --- |
| "Hamburg" | `wght` | 20 KB | 0.39 ms |
| "Hamburg" | `wght`, `slnt` | 32 KB | 0.35 ms |
| "Hello, Wear OS 12:45!" | `wght` | 35 KB | 0.44 ms |
| "Hello, Wear OS 12:45!" | all six | 594 KB | 3.6 ms |

Animate only the axes that move and fix the rest with `location`.

An earlier design tweened pre-instanced outlines between per-axis keyframes. It is still here,
internal, as an independent check (`RemoteVariableFontTweenText`): for one axis it is about the
same size and a little cheaper per frame (0.26 ms for "Hamburg"), but it animates only one axis.

## Players

Checked in the View player (`RemoteDocumentPlayer`), the embedded Compose player (`RcPlayer`,
behind `RemoteComposePlayerFlags.isEmbeddedPlayerEnabled`) and the CMP player
(`rc-player-compose`), with axes driven by named floats and by the document's clock. Every frame
matches a document built with that instant's axis values as constants.

The embedded player in `remote-player-compose` 1.0.0-alpha18 keeps its own time: it counts
Compose frame time from its first frame rather than reading the document's `RemoteClock`, so
`ContinuousSec` there is time since the player started, not wall-clock time. Newer AndroidX
sources start from the document's clock.

## Tests

- `VariableFontTest` checks the reader against `fontTools` to 0.01 font units for six fonts
  (Google Sans Flex, Roboto Flex, Recursive, Fraunces, Noto Sans, Inter).
- `VariationModelTest` checks the linear forms, and their specialization to the animated axes,
  against the font everywhere in each font's design space.
- `AxisKeyframesTest` checks the tween's keyframes reproduce the font at every value.
- `RenderFidelityTest` renders the expression path, the tween and the platform's own text for
  every tested axis of all six fonts, and several axes at once: the two paths agree to within
  antialiasing, and match the platform glyph for glyph.
- `LiveUpdateTest` and `ClockDrivenAxesTest` check frames follow a named float changed after load
  and the document's clock, in the View and embedded players.

## Limits

- One line, placed by nominal advances: no kerning, ligatures or complex-script shaping.
- No font fallback: characters the font lacks draw as `.notdef`.
- TrueType (`glyf`) outlines only, not CFF2. Composite glyphs must place their components by
  offset; a composite that anchors a component by point matching throws when it is drawn.
- The text is fixed when the document is created, and it is drawn as a path: there is no
  accessible text unless the caller adds a content description, and edge anti-aliasing is that
  of a path fill rather than the platform's text rasterizer. Light weights at small sizes look
  lighter than platform text, which thickens thin stems when it rasterizes.
