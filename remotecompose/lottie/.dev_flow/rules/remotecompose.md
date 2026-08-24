# RemoteCompose Restrictions

- **must**: Most specific Remote Compose functions (like `RemoteCanvas`, `RemotePaint`, geometries) require `@SuppressLint("RestrictedApi")` because these APIs are experimental.
- **must**: Limit usage of standard Compose UI nodes in a Remote Compose context; prefer `RemoteModifier`, `RemoteBox`, `RemotePath`.
