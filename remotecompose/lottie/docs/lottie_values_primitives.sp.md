# Lottie Values and Primitives — Specification  {#SP_LOT_VAL}

> **Code:** SP_LOT_VAL
> **Status:** active
> **Created:** 2026-09-02
> **Updated:** 2026-09-02
>
> **Plan:** [lottie_values_primitives.plan.md](./lottie_values_primitives.plan.md)
> **Depends on:** none
> **Used by:** [SP_LOT_SHAPES](./lottie_shapes.sp.md)
>
> Specification defining core Lottie value primitives (Bézier curves and Multi-stop Gradients) aligned with the official Lottie 1.0.1 Specification and JSON Schema.

## Contents

- [01. Data Structures](#SP_LOT_VAL_01) — Defines BezierValue, ColorStop, OpacityStop, ResolvedColorStop, and GradientValue
- [02. Contracts](#SP_LOT_VAL_02) — Serialization, coordinate validation, and stop resolution operations
- [03. Validation Rules](#SP_LOT_VAL_03) — Invariants across point lists, closed flags, and stop counts
- [04. State Transitions](#SP_LOT_VAL_04) — Lifecycle of raw JSON array to resolved visual stops
- [05. Verification Criteria](#SP_LOT_VAL_05) — Expected test cases, edge cases, and round-trip fidelity
- [06. Reversibility](#SP_LOT_VAL_06) — Rollback strategy and compatibility guarantees
- [07. Design Decisions](#SP_LOT_VAL_DEC) — Resolution of integer closed flags, stop count requirements, and float deduplication

## 01. Data Structures  {#SP_LOT_VAL_01}

### 01_00. Point (AST)  {#SP_LOT_VAL_01_00}

A 2D point [x, y] in the Lottie AST utilizing Remote Compose types (`RemoteFloat`).

Fields:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| x | RemoteFloat | yes | none | none | Horizontal coordinate |
| y | RemoteFloat | yes | none | none | Vertical coordinate |

Invariants:
- Serialized as a 2-element JSON array `[x, y]` using `.constantValue`.
- Deserialized from JSON arrays of length $\ge 2$: takes the first two coordinates and converts to `RemoteFloat` via `.rf`, discarding any extra dimensions (e.g. 3D z-axis).
- Deserialization throws `SerializationException` if the array contains fewer than 2 coordinates or non-float elements.

### 01_01. BezierValue  {#SP_LOT_VAL_01_01}

Defines a cubic polybezier path conforming to Lottie 1.0.1 Schema (`#/$defs/values/bezier`) utilizing Remote Compose types.

Fields:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| closed | RemoteBoolean | no | false.rb | none | Whether path loop is closed (`c`) |
| inTangents | List<Point> | yes | none | size == vertices.size | Tangent vector entering vertex (`i`) |
| outTangents | List<Point> | yes | none | size == vertices.size | Tangent vector leaving vertex (`o`) |
| vertices | List<Point> | yes | none | size >= 1 | Anchor vertices of the path (`v`) |

Invariants:
- `vertices.size == inTangents.size && vertices.size == outTangents.size`.
- Every point in `vertices`, `inTangents`, and `outTangents` is a 2D `Point(x, y)` with `RemoteFloat` coordinates.
- Missing `v`, `i`, or `o` fields in JSON fail deserialization with `SerializationException`.

### 01_01_01. RemoteBezierValue  {#SP_LOT_VAL_01_01_01}

Evaluated Bézier curve representation utilizing Remote Compose types (`RemoteBoolean`, `RemoteFloat`).

Fields:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| closed | RemoteBoolean | yes | none | none | Whether path loop is closed |
| inTangents | List<Point> | yes | none | size == vertices.size | Incoming tangent control points (RemoteFloat) |
| outTangents | List<Point> | yes | none | size == vertices.size | Outgoing tangent control points (RemoteFloat) |
| vertices | List<Point> | yes | none | size >= 1 | Anchor vertices (RemoteFloat) |

Invariants:
- Control points and vertices utilize `RemoteFloat` state holders.
- Closed flag utilizes `RemoteBoolean`.

### 01_02. ColorStop & OpacityStop  {#SP_LOT_VAL_01_02}

Decomposed color and opacity stop representations.

Fields for ColorStop:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| offset | Float | yes | none | 0.0f .. 1.0f | Position along the gradient line |
| red | Float | yes | none | 0.0f .. 1.0f | Normalized red component |
| green | Float | yes | none | 0.0f .. 1.0f | Normalized green component |
| blue | Float | yes | none | 0.0f .. 1.0f | Normalized blue component |

Fields for OpacityStop:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| offset | Float | yes | none | 0.0f .. 1.0f | Position along the gradient line |
| alpha | Float | yes | none | 0.0f .. 1.0f | Normalized opacity / alpha |

### 01_04. ResolvedColorStop  {#SP_LOT_VAL_01_04}

Represents a fully resolved color stop ready for Remote Compose rendering, using Remote types.

Fields:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| offset | Float | yes | none | 0.0f .. 1.0f | Position along the gradient line |
| color | RemoteColor | yes | none | valid RemoteColor | Resolved RGBA color using Remote Compose `RemoteColor` (`androidx.compose.remote.creation.compose.state.RemoteColor`) |


### 01_05. GradientValue  {#SP_LOT_VAL_01_05}

Internal model for flattened gradient stop arrays adhering to Lottie Spec §2.5 and formula $4 \times N_c + 2 \times N_o$.

Fields:
| Field | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| numberOfColors | Int | yes | none | > 0 | Explicit color stop count ($N_c$) |
| values | List<Float> | yes | none | size >= numberOfColors * 4, size - 4*Nc is even | Flattened stop float array |

Invariants:
- Total size must equal $4 \times N_c + 2 \times N_o$.
- Opacity stop count is derived deterministically: $N_o = (\text{values.size} - 4 \times N_c) / 2$.
- When $N_o > 0$, `hasTransparency` evaluates to `true`.

## 02. Contracts  {#SP_LOT_VAL_02}

### 02_01. BezierValueSerializer  {#SP_LOT_VAL_02_01}

Purpose: Deserializes Bézier paths with strict point validation while permitting legacy Bodymovin integer closed flags (`c: 0` / `c: 1`).

Input:
| Parameter | Type | Required | Constraints |
|---|---|---|---|
| decoder | Decoder | yes | JSON decoder |

Output:
| Field | Type | Description |
|---|---|---|
| value | BezierValue | Strongly typed Bézier path |

Errors:
| Code | Condition | Guidance |
|---|---|---|
| `ERR_MISSING_FIELD` | `v`, `i`, or `o` missing from JSON | Reject malformed path |
| `ERR_INVALID_COORDINATE` | Point array length != 2 or non-numeric item | Reject corrupted point rather than dropping coordinates |

### 02_02. GradientValue.resolveStops()  {#SP_LOT_VAL_02_02}

Purpose: Combines color stops and opacity stops into a single sorted list of `ResolvedColorStop` with interpolated colors and alphas.

Input: None (operates on `colorStops` and `opacityStops`).

Output:
| Field | Type | Description |
|---|---|---|
| result | List<ResolvedColorStop> | De-duplicated, sorted stops with resolved RGBA colors |

Processing logic:
1. If `colorStops.isEmpty()`, return empty list.
2. If `opacityStops.isEmpty()`, map `colorStops` with `alpha = 1f`.
3. Merge offsets from color and opacity stops using an epsilon threshold ($|\Delta| < 10^{-4}$) to prevent floating-point duplication.
4. Sort offsets in ascending order.
5. For each unique offset, linearly interpolate RGB from `colorStops` and Alpha from `opacityStops`.

## 03. Validation Rules  {#SP_LOT_VAL_03}

### 03_01. Point Geometry Validation  {#SP_LOT_VAL_03_01}
- A point list in JSON must be an array of arrays.
- Each point array must contain at least two numeric values representing `x` and `y`.
- If a point array contains more than two values (e.g. 3D coordinates `[x, y, z]`), the serializer extracts `x` and `y` and discards any remaining coordinates.
- If a point array contains fewer than two values, deserialization throws `SerializationException`.
- Silent dropping of invalid elements via `mapNotNull` is strictly prohibited.

### 03_02. Gradient Stop Partitioning  {#SP_LOT_VAL_03_02}
- Gradient values require known `numberOfColors > 0`.
- The data array must be partitioned into the first $4 \times N_c$ floats for color stops and the remaining $2 \times N_o$ floats for opacity stops.
- Unspecced heuristics based on `values.size / 4` or `values.size % 4 != 0` are prohibited.

## 05. Verification Criteria  {#SP_LOT_VAL_05}

### 05_01. Functional Expectations  {#SP_LOT_VAL_05_01}

| Contract | Scenario | Input | Expected outcome |
|---|---|---|---|
| `BezierValueSerializer` | Strict missing fields | `{"c": true}` | Throws `SerializationException` (missing `v`, `i`, `o`) |
| `BezierValueSerializer` | Under-dimensioned point (< 2) | `{"v": [[10.0]], "i": [[0.0, 0.0]], "o": [[0.0, 0.0]]}` | Throws `SerializationException` (coordinates < 2) |
| `BezierValueSerializer` | Extra coordinates (3D points) | `{"v": [[10.0, 20.0, 30.0]], "i": [[0.0, 0.0]], "o": [[0.0, 0.0]]}` | Normalizes to `Point(10f, 20f)`, discarding 30.0 |
| `BezierValueSerializer` | Bodymovin integer closed flag | `{"c": 1, "v": [[1,2]], "i": [[0,0]], "o": [[0,0]]}` | Deserializes `closed = true` |
| `BezierValueSerializer` | Boolean round-trip | Deserialized integer closed flag | Serializes to `"c": true` (boolean) |
| `GradientValue` | Multi-stop transparency | 2 colors + 2 opacities (12 floats) | `hasTransparency = true`, 2 color stops, 2 opacity stops |
| `GradientValue.resolveStops()` | Epsilon offset merging | Color stop at 0.5f, Opacity stop at 0.50001f | Merged into single stop at 0.5f |
| `GradientValueSerializer` | Round-trip fidelity | Valid `GradientValue` | Serializes and re-deserializes with identical `p` and `values` |

## 06. Reversibility  {#SP_LOT_VAL_06}

| Aspect | Rollback approach |
|---|---|
| Internal data classes | Models are internal to `:remotecompose:lottie`; no public binary compatibility impact |
| Downstream callers | Existing test cases and layer renderers adapt seamlessly to non-null constructor params |

## 07. Design Decisions  {#SP_LOT_VAL_DEC}

### DEC_01 — Dual Boolean & Integer Closed Flag Support  {#SP_LOT_VAL_DEC_01}
> **Status:** resolved  
> **Question:** Is it actually the case that `c` can come as `true`/`false` or `(0|1)`, and should we force only one?  
> **Decision:** Support both boolean (`true`/`false`) and integer (`1`/`0`) during deserialization without forcing only one representation. Always emit canonical boolean (`"c": true` / `"c": false`) during serialization.  
> **Rationale:** 
> - On one hand, the Lottie specification text states: *"Represents boolean values as an integer. `0` is false, `1` is true."* (legacy Bodymovin representation).
> - On the other hand, in the Lottie 1.0.1 JSON Schema (`#/$defs/values/bezier/properties/c`), it is explicitly defined as `"type": "boolean", "default": false`. According to the JSON Schema specification (https://json-schema.org/understanding-json-schema/reference/boolean): *"The boolean type matches only two special values: true and false. Note that values that evaluate to true or false, such as 1 and 0, are not accepted by the schema."*
> - Because real-world Bodymovin exporters produce integers while strict schema validators require booleans, it is safer to flexibly accept both options upon deserialization while standardizing on boolean serialization.

### DEC_02 — Mandatory Color Stop Count  {#SP_LOT_VAL_DEC_02}
> **Status:** resolved  
> **Question:** Can `numberOfColors` default to 0 or be inferred from array size?  
> **Decision:** Make `numberOfColors` mandatory without defaults.  
> **Rationale:** Per Lottie Spec §2.5, flat stop arrays cannot be deterministically partitioned into color ($4 \times N_c$) and opacity ($2 \times N_o$) stops without knowing $N_c$.

### DEC_03 — Epsilon-Tolerant Offset Deduplication  {#SP_LOT_VAL_DEC_03}
> **Status:** resolved  
> **Question:** How should identical color and opacity offsets be merged during stop resolution?  
> **Decision:** Replace IEEE-754 `.distinct()` with an epsilon comparison ($|\Delta| < 10^{-4}$).  
> **Rationale:** IEEE-754 float comparison fails when color and opacity offsets differ by floating-point rounding errors, creating duplicate adjacent stops and division-by-near-zero artifacts.

### DEC_04 — Point Model and Remote Types Adoption for Bézier Curves  {#SP_LOT_VAL_DEC_04}
> **Status:** resolved  
> **Question:** How should Bézier points and closed flags be modeled in the format AST and renderer, and how should coordinate dimensions be handled?  
> **Decision:** 
> 1. In `format.values`: Define `Point(val x: RemoteFloat, val y: RemoteFloat)` with a convenience constructor `Point(x: Float, y: Float) : this(x.rf, y.rf)`. Deserialization requires $\ge 2$ coordinates, taking the first two as `RemoteFloat` via `.rf` and discarding extra dimensions. Less than 2 coordinates throws `SerializationException`. Serialization outputs `.constantValue`.
> 2. In `format.values`: Use `closed: RemoteBoolean = false.rb` in `BezierValue`, parsing booleans or integers and serializing `.constantValue`.
> 3. In `renderer.properties`: Reuse `format.values.Point` and `closed: RemoteBoolean` in `RemoteBezierValue`, eliminating duplicate class definitions and conversion boilerplate.  
> **Rationale:** Provides strong type safety and explicit coordinate naming (`p.x`, `p.y`), guarantees that every point has strictly two 2D coordinates, eliminates duplicate `Point` declarations between format and renderer, and directly integrates Remote Compose state holders.
