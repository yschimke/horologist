# Font Variation Test Resources

Fonts used by the previews, unit tests and Roborazzi screenshot tests in
`remotecompose/fontvariation`.

| File | Description | Source |
|------|-------------|--------|
| `res/raw/google_sans_flex_wght_rond.ttf` | Google Sans Flex, subset to Basic Latin and Latin-1 (U+0020–007E, U+00A0–00FF), with `opsz`, `wdth`, `GRAD` and `slnt` pinned to their defaults, so only the `wght` and `ROND` axes remain. About 120 KB rather than 4 MB. | [google/fonts `ofl/googlesansflex`](https://github.com/google/fonts/tree/main/ofl/googlesansflex), reduced with `fontTools` `subset` then `varLib.instancer` |

`src/test/resources/google_sans_flex_golden.json` holds outlines and advances of the same font at
several `wght`/`ROND` locations, produced by `fontTools` `getGlyphSet(location=...)` with
`DecomposingRecordingPointPen`. The unit tests check the Kotlin reader against it.

## License & Copyright

Google Sans Flex is Copyright 2015 The Google Sans Flex Authors and is licensed under the SIL
Open Font License, Version 1.1; see [`OFL.txt`](OFL.txt). The reduced font is a Modified Version
under that license and keeps the original name table, as the license permits for fonts without
a Reserved Font Name.
