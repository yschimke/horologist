# Specification: Renderer (`renderer`)
**Status**: active
**Depends on**: [Format](format.sp.md)

## 01 Data Structures
- Stateless conversion functions — no state is held here natively.
- Expects `LottieSettings` at execution time for frame lookups.

## 02 Contracts
- **Contract 1: Path Creation**
  - Inputs: Primitive parameters (`points`, `innerRadius`, `outerRadius`, `roundedness` etc.).
  - Output: Compiled `RemotePath`.
- **Contract 2: Transforming**
  - Inputs: `Transform` data, `RemotePaint`, `LottieSettings`, `RemoteCanvas`.
  - Output: `Unit` (Mutates `RemoteCanvas` state via stacking matrices).

## 03 Validation Rules
- Asserts that keyframes are structurally valid for interpolation.
