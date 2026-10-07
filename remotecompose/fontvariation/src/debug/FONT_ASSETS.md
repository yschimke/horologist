# Font Variation Test Resources

Fonts used by the previews, unit tests and Roborazzi screenshot tests in
`remotecompose/fontvariation`. Each is an OFL variable font from
[google/fonts](https://github.com/google/fonts), reduced with `fontTools`: `subset` to the
characters listed, then `varLib.instancer` to pin every axis not listed to its default.

| File | Font | Characters | Axes kept | Source |
|------|------|------------|-----------|--------|
| `res/raw/google_sans_flex_wght_rond.ttf` | Google Sans Flex | U+0020–007E, U+00A0–00FF | `wght`, `ROND` | [`ofl/googlesansflex`](https://github.com/google/fonts/tree/main/ofl/googlesansflex) |
| `res/raw/roboto_flex.ttf` | Roboto Flex | U+0020–007E | `wght`, `wdth`, `slnt`, `opsz`, `GRAD`, `XTRA` | [`ofl/robotoflex`](https://github.com/google/fonts/tree/main/ofl/robotoflex) |
| `res/raw/recursive.ttf` | Recursive | U+0020–007E | `wght`, `slnt`, `CASL`, `MONO` | [`ofl/recursive`](https://github.com/google/fonts/tree/main/ofl/recursive) |
| `res/raw/fraunces.ttf` | Fraunces | U+0020–007E | `wght`, `opsz`, `SOFT` | [`ofl/fraunces`](https://github.com/google/fonts/tree/main/ofl/fraunces) |
| `res/raw/noto_sans.ttf` | Noto Sans | U+0020–007E | `wght`, `wdth` | [`ofl/notosans`](https://github.com/google/fonts/tree/main/ofl/notosans) |
| `res/raw/inter.ttf` | Inter | U+0020–007E | `wght`, `opsz` | [`ofl/inter`](https://github.com/google/fonts/tree/main/ofl/inter) |

Recursive's `CRSV` and Fraunces' `WONK` are pinned: they mostly swap glyphs through GSUB feature
variations, which the module's cmap-only layout does not apply.

`src/test/resources/<font>_golden.json.gz` holds each font's outlines and advances at its
default, minimum, maximum and three random locations, produced by `fontTools`
`getGlyphSet(location=...)` with `DecomposingRecordingPointPen`. The unit tests check the Kotlin
reader against them.

## License & Copyright

All six fonts are licensed under the SIL Open Font License, Version 1.1; each license, with its
copyright notice, is in [`licenses/`](licenses). The reduced fonts are Modified Versions under
that license and keep the original name tables, as the license permits for fonts without a
Reserved Font Name.
