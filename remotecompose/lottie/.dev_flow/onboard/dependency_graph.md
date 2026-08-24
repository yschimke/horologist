# Dependency Graph

- `format`: Leaf node, parses JSON.
- `renderer`: Depends on `format`.
- `root_compose`: Depends on `format` and `renderer`. Exposes Compose UI.
