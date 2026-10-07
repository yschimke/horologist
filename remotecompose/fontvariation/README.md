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
2. Each tent is written once, straight from its axis' user value: the font's user-to-normalized
   mapping (`avar` included) and the tent together are piecewise linear, so a sum of clamped
   ramps.
3. Each distinct product of tents is written once and shared. Axes that are not animated are
   folded into the coefficients at creation time, and regions that cannot move drop out.
4. Each distinct outline coordinate is one expression, a chain of multiply-adds
   `constant + Σ coefficient × scalar`.
5. The path is an ordinary `RemotePath` whose coordinates are those expressions' ids; the player
   resolves them whenever the expressions change.

When one axis is animated and its regions do not overlap (they usually meet only at the
default), there is a cheaper exact form: at most one tent is non-zero, so the outline is the
default outline tweened towards that tent's key outline (the outline with the tent at its peak).
The document then holds a few whole outlines and no coordinate expressions, and draws one
`drawTweenPath`, picked by a condition on the tents. It is used when its keys come to fewer bytes
than the expressions (`TweenGrid`).

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

## Text that changes: `RemoteString`

An overload takes the text as a `RemoteString`, so the player can change it — a clock, a counter,
a label a host updates — limited to a known set of characters and a maximum length:

```kotlin
RemoteVariableFontText(
  text = minutes + ":" + seconds, // any RemoteString
  characters = "0123456789:",
  maxLength = 5,
  font = robotoFlex,
  axes = mapOf("wght" to weight),
  fontSize = 32.rdp,
)
```

Remote Compose has no operation that reads a character's code, so the document reads one with a
hidden bitmap font. Each character in `characters` gets a glyph as wide as its index, and measuring
the one-character substring at position *i* with that font gives the index of the character there
(0 for none). Each character's outline is in the document once, as expressions of the axes; each
position draws the one whose index matches, after the advances of the glyphs before it. Kerning
comes from a second hidden bitmap font whose kerning table holds the font's pair kerning, so the
player kerns while it measures the text so far.

Characters outside `characters` and beyond `maxLength` are not drawn. The box is as wide as
`maxLength` of the widest character, and the text starts at its left edge.

### Sharing glyphs between texts

Several texts in one document — a clock's time and date, a counter and its label — can share their
glyphs. Build them once with `rememberVariableFontGlyphs` and pass them to each text:

```kotlin
val glyphs = rememberVariableFontGlyphs(robotoFlex, "0123456789:/", mapOf("wght" to weight))
RemoteVariableFontText(time, maxLength = 5, glyphs = glyphs, fontSize = 32.rdp)
RemoteVariableFontText(date, maxLength = 5, glyphs = glyphs, fontSize = 16.rdp)
```

The first text writes the outlines, their axis expressions and the hidden bitmap fonts; the others
refer to them. The overload that takes `characters` and `font` builds glyphs for itself.

Each position reads its character, places it, and draws it once: its path id is the first glyph's
path id plus the character's index, which the player reads from an integer (a dereferenced draw).
When no animated axis moves an advance — roundness, grade and slant usually don't; weight and width
usually do — a third hidden bitmap font carries the advances as well as the kerning, and each glyph
is placed by measuring alone.

Roboto Flex digits and `:`, per position: about 240 bytes with constant advances (`GRAD`), about
1.3 KB when the advances vary (`wght`), on top of the glyphs (about 6–7 KB). A ten-character `GRAD`
clock is 9.5 KB. Two five-character weight-animated texts are 19.6 KB with shared glyphs, 26.4 KB
without. Repeated characters within a text always share their outline.

## Kerning

Both overloads kern with the font's own `GPOS` pair kerning (the `kern` feature's pair lookups,
formats 1 and 2, with their `GDEF` variation deltas), read at `kerningLocation`, which defaults to
`location`. It is a constant per pair: kerning does not follow the animated axes. `KerningTest`
checks it against the platform's own shaping of each test font at three locations.

## Cost

The document grows with the distinct coordinates and regions in the text, and fastest with the
number of axes animated together, since regions that span several axes multiply. Animate only the
axes that move and fix the rest with `location`.

"Hamburgefonstiv 0123", one axis (`TweenGridTest`; frame in `rc-player-compose` 2.1.2 on the
desktop JVM, CPU raster, a new value every frame, best of five runs):

| Font | Axis | Expressions | Key outlines |
| --- | --- | --- | --- |
| Roboto Flex | `wght` | 42 KB, 1.02 ms | 32 KB, 0.70 ms |
| Roboto Flex | `slnt` | 27 KB, 0.91 ms | 21 KB, 0.76 ms |
| Roboto Flex | `GRAD` | 38 KB, 1.06 ms | 32 KB, 0.64 ms |
| Inter | `wght` | 57 KB, 1.26 ms | 38 KB, 0.80 ms |
| Fraunces | `SOFT` | 76 KB, 1.69 ms | 48 KB, 1.05 ms |
| Recursive | `CASL` | 104 KB, 2.19 ms | 59 KB, 1.02 ms |
| Google Sans Flex | `ROND` | 32 KB, 0.95 ms | 32 KB, 0.86 ms |

Google Sans Flex's and Noto Sans' weight regions overlap, so their weight is expressions only (69
and 70 KB). Several axes at once are expressions; writing each tent as ramps of the user value and
each coordinate as multiply-adds made those 12 to 20% smaller than before: Roboto Flex `wght` +
`slnt` 82 → 68 KB, Inter `wght` + `opsz` 124 → 100 KB, Fraunces three axes 324 → 258 KB.

An earlier design tweened pre-instanced outlines between per-axis keyframes. It is still here,
internal, as an independent check (`RemoteVariableFontTweenText`).

## Ahead of time

Most of the cost of making a document is working the text's outline out from the font: about 40
ms for "Hello, Wear OS 12:45!" on the desktop JVM, where parsing the font is 0.1 ms and writing the
document about 1 ms. `RemoteVariableFontText` keeps the outlines of recent texts, so making the same
text's document again costs only the writing. To pay nothing at run time, and leave the font out of
the app, work the outline out ahead of time:

```kotlin
// At build time:
val encoded = font.outline("Hamburg", axes = listOf("wght")).encode()

// In the app, with no font:
val outline = VariableTextOutline.decode(encoded) // 0.07 ms
RemoteVariableFontText(outline, mapOf("wght" to weight), fontSize = 32.rdp)
```

`VariableTextOutline` holds the outline in whichever exact form the document will use (key
outlines or one expression per coordinate), the tents as clamped ramps of the axis values and the
box; `encode` writes it compactly as a string (whole numbers, as most font values are, in one
character, path coordinates as differences). "Hamburg" with `wght` is 2,344 characters.

`VariableFontCodegen` (in the tests) writes such an outline into a Kotlin file with a composable that
draws it; `VariableFontCodegenTest` regenerates the examples in `src/debug/.../generated`
(`CODEGEN_WRITE=1`) and fails when they are stale, and `GeneratedVsLibraryTest` checks each writes
the same document as `RemoteVariableFontText` from the font, byte for byte.

### Simplifying for a pixel size

With `pixelSize`, the font size in pixels the text will be drawn at, `outline` also drops what
cannot move an edge by more than `tolerancePixels`: variation terms too small to matter, curves flat
at every axis value, points on a straight line at every axis value. Glyph outlines at watch sizes
have few flat curves or collinear points, so it mostly prunes small terms, and saves little
(`SimplifiedOutlineTest`, "Hamburgefonstiv 0123"):

| Font, axes, size | Exact | ¹⁄₁₆ px | ¹⁄₈ px | ¹⁄₂ px |
| --- | --- | --- | --- | --- |
| Roboto Flex `wght` (keys), 44 px | 31.7 KB | −0.2% | −0.6% | −4% |
| Roboto Flex `wght` + `slnt`, 44 px | 68.1 KB | −4% | −7% | −20% |
| Roboto Flex `wght` + `slnt`, 24 px | 68.1 KB | −7% | −11% | −31% |
| Google Sans Flex `wght` + `ROND`, 44 px | 125.0 KB | −1% | −2% | −8% |

No tolerance leaves every pixel the same: antialiasing quantizes coverage, so any moved edge can
change a few dozen pixels by up to a quarter. At a sixteenth of a pixel the change is invisible.

## Players

Checked in the View player (`RemoteDocumentPlayer`), the embedded Compose player (`RcPlayer`,
behind `RemoteComposePlayerFlags.isEmbeddedPlayerEnabled`) and the CMP player
(`rc-player-compose`), with axes driven by named floats and by the document's clock. Every frame
matches a document built with that instant's axis values as constants.

The embedded player in 1.0.0-alpha18 draws nothing for a path made by a `PathTween` operation,
though it draws `drawTweenPath`; so the key outlines are used for one animated axis only, where a
single `drawTweenPath` suffices, and not as tweens of tweens across axes.

The `RemoteString` overload plays in the View and CMP players. The embedded player does not
implement `BitmapTextMeasure`, which it needs to read the text, and draws nothing.

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
- `TweenGridTest` checks the key outlines draw what the expressions draw, to within
  antialiasing, at each axis' extremes, default and random values, for each test font.
- `VariableTextOutlineTest` checks an outline is the font's own at random axis values and that
  `encode` round-trips exactly; `SimplifiedOutlineTest` measures simplification.
- `LiveUpdateTest` and `ClockDrivenAxesTest` check frames follow a named float changed after load
  and the document's clock, in the View and embedded players.
- `KerningTest` checks the `GPOS` reader against the platform's shaping.
- `RemoteStringTextTest` checks one `RemoteString` document against the `String` overload for
  each of several texts, kerned letters and clock digits.
- `SharedGlyphsTest` checks two texts sharing glyphs draw exactly what two texts with their own do;
  `RemoteStringTextTest` also covers axes that leave advances constant.

## Sizzle reel

`SizzleReel.kt` (debug) has five round-watch scenes after the Wear OS Material 3 Expressive guide's
"Rich color" and "Variable fonts" sections: Roboto Flex weight and width, a weight wave through a
word in three accents, a stopwatch whose digits are a `RemoteString` with a red stop button,
Google Sans Flex roundness, and a closing title. Every axis follows the document's own clock.
`./remotecompose/fontvariation/sizzle-reel.sh [out-dir]` records them on the View player
(`SizzleReelRecorder`, skipped unless `SIZZLE_OUT` is set) and joins them into an MP4 and a GIF
with `ffmpeg`.

## Limits

- One line, placed by advances and pair kerning: no ligatures, contextual alternates or
  complex-script shaping.
- No font fallback: characters the font lacks draw as `.notdef`.
- TrueType (`glyf`) outlines only, not CFF2. Composite glyphs must place their components by
  offset; a composite that anchors a component by point matching throws when it is drawn.
- The text is fixed when the document is created, and it is drawn as a path: there is no
  accessible text unless the caller adds a content description, and edge anti-aliasing is that
  of a path fill rather than the platform's text rasterizer. Light weights at small sizes look
  lighter than platform text, which thickens thin stems when it rasterizes.
