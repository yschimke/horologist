# Lottie Specification Testing Strategy

**Target Audience:** `remotecompose/lottie` library developers.

**Purpose:** Provide a systematic structure for writing, finding, and fixing Lottie 1.0.1 specification compliance tests. This document ensures consistent coverage across AST representation, JSON parsing, and visual rendering.

## Test Workflow for Bug Fixes

When a bug is reported (e.g., "Gradients crash on load" or "Opacity is ignored"), follow this flow to resolve it using this test structure:

1. **Locate the domain:** Identify which of the 11 Lottie specification sections the failing element belongs to (e.g., `Shapes -> Gradient Fill`).
2. **Verify AST Parsing:** Run the corresponding `ParsingTest`. If parsing crashes, fix the JSON decoder (e.g., `LottieDecoder.kt`).
3. **Verify AST Representation:** Assert the instantiated `RemoteShape` or `Property` object contains the extracted values. If values are empty, fix the mapping payload.
4. **Verify Rendering:** Check the Roborazzi screenshot diff test for that feature. If the AST is correct but the output is wrong, fix the Compose projection (e.g., `RemoteStyle.kt`).

---

## 1. Values & Enumerations (Constants)
Tests elementary data models and Lottie enumeration constants, which serve as the foundation for all complex properties.

**1.1 Data Values to test:**
*   **Integer Boolean (`0` or `1`):** 
    *   *Parsing Test:* Assert that integer values `0` and `1` strictly decode to Kotlin `false` / `true` booleans without exceptions.
*   **Vector (`[x, y]` or `[x, y, z]`):** 
    *   *Parsing Test:* Extract raw float arrays of variable lengths.
    *   *AST Test:* Assert the parsed structure correctly retains X, Y, and optional Z coordinates mapping to Native floats.
*   **Color (RGB Floats / Hex Color):** 
    *   *Parsing Test:* Extract standard `[R, G, B, A]` arrays where values range from 0.0 to 1.0, and string Hex codes (`#FF0000`).
    *   *AST Test:* Assert that a `[1.0, 0.0, 0.0, 1.0]` array or `#FF0000` translates to the correct ARGB representation in the AST.
*   **Gradient Array:** 
    *   *Parsing Test:* Extract flattened `4 * Nc + 2 * No` float arrays heavily used in Gradient Fills.
    *   *AST Test:* Assert the array separates explicitly into `(offset, color)` and `(offset, opacity)` pairs, validating that `offset` lies between `0.0` and `1.0`. Verify both "Gradient without transparency" and "Gradient with transparency" array structures.
*   **Bezier Shape (`v`, `i`, `o`, `c`):** 
    *   *Parsing Test:* Extract vertex arrays `v`, in-tangents `i`, out-tangents `o`, and the closed boolean `c`.
    *   *AST Test:* Assert that a generated node contains exactly $N$ points containing valid `v`/`i`/`o` coordinates.
*   **Data URL (Embedded Assets):**
    *   *Parsing Test:* Ensure string sequences prefixed with `data:image/` (RFC2397 base64 dumps) parse as strings without JSON decoding crashes.

**1.2 Enumerations (Constants) to test:**
The AST must faithfully map Lottie's integer IDs to internal system types.
*   **Painting Constants:**
    *   *Fill Rule:* `1` (NonZero), `2` (EvenOdd).
    *   *Line Cap:* `1` (Butt), `2` (Round), `3` (Square).
    *   *Line Join:* `1` (Miter), `2` (Round), `3` (Bevel).
    *   *Gradient Type:* `1` (Linear), `2` (Radial).
    *   *Stroke Dash Type:* `d` (Dash), `g` (Gap), `o` (Offset).
*   **Shape / Mask Constants:**
    *   *Star Type:* `1` (Star), `2` (Polygon).
    *   *Shape Direction:* `1` (Normal), `3` (Reverse).
    *   *Mask Mode:* `a` (Add), `s` (Subtract), `i` (Intersect), etc.
*   **Compositing Constants:**
    *   *Matte Mode:* `0` (Normal), `1` (Alpha), `2` (Inverted Alpha), `3` (Luma), `4` (Inverted Luma).
    *   *Blend Mode:* Normal, Multiply, Screen, Overlay, etc.

*Rendering Note: Tests for elementary values and enums do not verify visual pixels directly. Rendering correctness of these basic types is proven implicitly during Phase 5 (Shapes).*

## 2. Properties
Tests property containers and their interpolation semantics. Properties can be static (scalar/array values) or animated (arrays of keyframes).

**2.1 Animatable Property Containers (Parsing & AST):**
*   **Scalar Property (`s`, `o`, `r`, etc.):** 
    *   *Parsing Test:* Decode static `{"a": 0, "k": 100}` and animated `{"a": 1, "k": [{...}]}`.
    *   *AST Test:* Assert the property correctly holds the static float or the array of keyframes.
*   **Vector Property / Multi-dimensional (`p`, `s`, `a`, etc.):** 
    *   *Parsing Test:* Decode static `{"a": 0, "k": [100, 200]}` and animated variants.
    *   *AST Test:* Assert output structure maintains arrays of numbers per frame.
*   **Color Property (`c`):** 
    *   *Parsing Test:* Decode RGBA arrays `{"a": 0, "k": [1, 0, 0, 1]}` into native colors.
*   **Gradient Property (`g`):**
    *   *Parsing Test:* Decode `{"a":0, "k":{"p":3, "k":[...]}}` verifying gradient stops array matches.
*   **Position / Splittable Position Property (`p`):**
    *   *Parsing Test:* Decode non-split `{"a": 0, "k": [10,20]}` and split `{"s": true, "x": {...}, "y": {...}}`.
    *   *AST Test:* Assert `SplitPositionProperty` routes independent `x` and `y` scalars dynamically based on the boolean `s`.
*   **Shape/Bezier Property (`pt`):**
    *   *Parsing Test:* Validate parsing of geometry (vertices, in/out tangents, closed flag) nested inside the `k` payload.

**2.2 Keyframes & Interpolation Semantics:**
*   **Base Keyframes & Typed Variants:**
    *   *Parsing Test:* Assert keyframe objects decode `t` (start time), `s` (start value) and `h` (hold boolean). Extract type-specific payloads faithfully (`color-keyframe`, `vector-keyframe`, `position-keyframe`, `bezier-keyframe`, `gradient-keyframe`).
    *   *AST Test:* Verify that `h: 1` correctly maps to a step/hold interpolation curve.
*   **Gradient Stops Data:**
    *   *Parsing Test:* Assert `gradient-stops` arrays natively isolate offsets, colors, and alpha masks dynamically.
*   **Easing Handles (Bezier Curves):**
    *   *Parsing Test:* Decode `i` and `o` objects containing `x` and `y` arrays (control points 0.0 to 1.0). Fallback gracefully if missing context (linear interpolation).
    *   *AST Test:* Check parsing of different curves per dimension (since vector properties have an array of easing handles per axis).
    *   *Rendering Test:* Visually verify speed ramping in an animation with non-linear easing across discrete frames. Verify hold keyframes do not interpolate.

## 3. Composition
Tests the root-level animation structure (`animation.json`), global timescale, and metadata bounding the entire document.

**3.1 Root Animation Node:**
*   **Version & Metadata (`v`, `nm`, `meta`):**
    *   *Parsing Test:* Extract string version (`v`), composition name (`nm`), and the dedicated `meta` JSON object (authoring info).
    *   *AST Test:* Ensure AST retains this metadata explicitly.
*   **Markers & Slots (`markers`, `slots`):**
    *   *Parsing Test:* Ensure composition-level `markers` array parses string triggers, and global `slots` map dynamic properties.
    *   *AST Test:* Verify the root node holds references for timeline event listening and slot resolution.
*   **Framerate & Lifecycle (`fr`, `ip`, `op`):**
    *   *Parsing Test:* Parse `fr` (framerate float), `ip` (in-point frame), and `op` (out-point frame).
    *   *AST Test:* Verify that `op - ip` defines the exact total frame duration mathematically.
    *   *Rendering Test:* Assert rendering out-of-bounds (`t < ip` or `t > op`) properly limits to empty or frozen states.
*   **Canvas Dimensions (`w`, `h`):**
    *   *Parsing Test:* Parse width `w` and height `h`.
    *   *AST Test:* Store intrinsic resolution.
    *   *Rendering Test:* Verify the root Compose container matches this intrinsic shape or scales appropriately.
*   **Global Assets & Layers (`assets`, `layers`):**
    *   *Parsing Test:* Ensure massive arrays of `layers` and nested precomposited `assets` are correctly traversed without deep recursion stack overflows.

## 4. Layers
Tests composition hierarchies, layer types, and their transform stacks. Layers define how elements composite and clip over time.

**4.1 Layer Types & Instantiation:**
*   **Shape Layer (`ty`: 4) & Base Attributes:**
    *   *Parsing Test:* Assert parsing of nested `shapes`. Assert the `hd` (hidden) boolean is mapped universally on layers.
*   **Precomposition / Null / Solid / Image Layers (`ty`: 0, 3, 1, 2):**
    *   *Parsing Test:* Assert attributes specific to the layer type parse explicitly (`refId` for precomps, `sc` hex and explicit `sw` width / `sh` height for Solids, etc.).
*   **Time Bounds, Stretch & Remapping (`ip`, `op`, `st`, `sr`, `tm`):**
    *   *Parsing Test:* Decode layer time variables and explicit time-remapping `tm` properties for precomps.
    *   *Parsing Test:* Decode layer-specific in-point (`ip`), out-point (`op`), start time (`st`), and stretch multiplier (`sr`).
    *   *AST Test:* Assert layers map to their own active timelines distinct from the root composition's `ip`/`op`.

**4.2 Transformation & Hierarchy (`ks`, `parent`, `ind`):**
*   **Layer Properties (`ks`, `ao`):**
    *   *Parsing Test:* Validate parsing of Transform struct (Anchor, Position, Scale, Rotation, Opacity) and visual auto-orient paths (`ao`).
    *   *AST Test:* Ensure AST maps into independent projection matrices.
*   **Parenting (`ind`, `parent`):**
    *   *Parsing/AST Test:* Find the internal layer index (`ind`) and link dependent layers requesting `parent: ind`. Ensure cyclic parent relationships throw or fail gracefully.
    *   *Rendering Test:* Verify transform multipliers (parent-child matrices) compound correctly (e.g., child offsets scale by parent scale).

**4.3 Compositing (`bm`, `tt`, `tp`, `masksProperties`):**
*   **Blend Modes & Mattes:**
    *   *Parsing Test:* Track enum combinations for `bm` (Blend Mode), `tt` (Track Matte type), and explicitly verify `tp` (Track Matte Parent index mapping).
    *   *Rendering Test:* Visually confirm Z-index parity matching array indices and targeted alpha masking over siblings.

## 5. Shapes (Sub-divided)
The largest domain. Tests distinct visual vectors drawn on the canvas (`ty: 4` payloads).

### 5.1 Geometric Shapes (Paths & Primitives)
*   **Rectangle (`rc`), Ellipse (`el`):**
    *   *Parsing Test:* Decode corner radius (`r`), size (`s`), and position (`p`) dimensions.
    *   *AST Test:* Assert properties map to internal `RemoteShape` rectangle/circle primitives.
    *   *Rendering Test:* Verify exact pixel limits.
*   **Path/Bezier (`sh`), Polystar (`sr`):**
    *   *Parsing Test:* Decode bezier curves (`sh`) and Polystar variables (`pt` points, `or`/`ir` radiuses).
    *   *AST Test:* Ensure custom polygon or arbitrary point maps create fully distinct path instructions.

### 5.2 Style Shaders (Fills & Strokes)
*   **Solid Fill (`fl`), Solid Stroke (`st`):**
    *   *Parsing Test:* Extract color (`c`) and opacity (`o`). Extract stroke widths (`w`) and `line-cap`/`line-join` enums.
    *   *AST Test:* Assign `RemoteStyle` shading correctly. Note: Lottie handles shape rendering with a last-in order depending on the `Group` hierarchy, confirm rendering parity with golden outputs.
*   **Gradient Fill (`gf`), Gradient Stroke (`gs`):**
    *   *Parsing Test:* Ensure `GradientProperty` logic correctly extracts `p` (stop count), `s` / `e` (start / end coords).
    *   *Rendering Test:* Compare generated Compose `Brush` shaders against AE boundaries.
*   **Stroke Dashes (`d` arrays):**
    *   *Parsing/Rendering Test:* Assert `StrokeDash` variables (Dash, Gap, Offset) parse and render accurately on lines.

### 5.3 Structural Groupings & Modifiers
*   **Group (`gr`) & Transform (`tr`):**
    *   *Parsing Test:* Assert elements nestled inside `gr.it` shape arrays successfully recurse into children AST representations.
    *   *AST Test:* Ensure group bounds and transform boundaries (`tr`) append to the stack rather than overriding global layer transforms.
*   **Trim Path Modifiers (`tm`):**
    *   *Parsing Test:* Decode `s` (start), `e` (end), `o` (offset).
    *   *Rendering Test:* Ensure paths are accurately chopped before fills and strokes are applied visually in Compose.
*   **Pucker/Bloat (`pb`), Rounded Corners (`rd`):**
    *   *(Implement tests progressively as `remotecompose/lottie` expands feature support)* Validate property ingestion of modifier payloads.

## 6. Assets
* **Types:** Nested JSON Precompositions, Image wrappers.
* **Parsing / AST Tests:** Map asset `id` linking.

## 7. Helpers & Math Expressions
* **Types:** Easing curves, in/out tangents, JS Expressions.
* **Tests:** Validate cubic bezier tangent values (`ti`, `to`, `i`, `o`) parse as tuples without failing. 

## 8. Text & Text Documents
*(Implement tests progressively as `remotecompose/lottie` expands feature support)*
* **Types:** Text layer (`ty: 5`), Text Animators, Font properties.

## 9. Formatting & Masks
* **Types:** Mask shapes (`pt`), Matte definitions (Alpha/Luma inverting).

## 10. Effects
* **Types:** Gaussian blurs, Drop shadows.

## 11. Markers
* **Types:** Timeline string triggers.

---

## Test Adding Policy
When an external feature is adopted or missing coverage is spotted:
1. Extract the raw JSON snippet from `lottie-spec/1.0.1`.
2. Add a `parse_X_successfully()` to `ParsingTest.kt` covering AST integrity.
3. Inject the element into a minimal `test_X.json`.
4. Trigger `./gradlew :remotecompose:lottie:recordRoborazziDebug` to establish the rendering baseline. 
