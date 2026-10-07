# Remote Compose: variable-font axis animation

`RemoteVariableFontText` draws a line of text in a variable font with one axis (`ROND`, `wght`,
…) driven by a `RemoteFloat`, and the player never loads, re-instances or lays out a font.

```kotlin
val font = VariableFont.parse(context.resources.openRawResource(R.raw.my_font).readBytes())

RemoteVariableFontText(
  text = "Hello",
  font = font,
  axis = "ROND",
  value = roundness, // any RemoteFloat: a named float, an animation, a time expression
  fontSize = 32.dp,
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

Variable-font outlines are piecewise linear in each axis. Their deltas only change slope where a
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

## Limits

- One line, placed by nominal advances: no kerning, ligatures or complex-script shaping.
- No font fallback: characters the font lacks draw as `.notdef`.
- TrueType (`glyf`) outlines only, not CFF2. Composite glyphs must place their components by
  offset; a composite that anchors a component by point matching throws when it is drawn.
- The text is fixed when the document is created, and it is drawn as a path: there is no
  accessible text unless the caller adds a content description, and edge anti-aliasing is that
  of a path fill rather than the platform's text rasterizer.
- Only one axis animates; the others are fixed by `location`.
