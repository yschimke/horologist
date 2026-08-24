# Code Style and Guidelines

- **must**: Use `kotlinx.serialization` for JSON parsing. Data classes must be annotated with `@Serializable`.
- **must**: Provide descriptive error handling and stick to functional data mappings.
- **should**: Extract calculations out of `@Composable` functions where possible to minimize allocation churn during recomposition. Example: `buildAncestorTransforms` is cached with `remember`.
