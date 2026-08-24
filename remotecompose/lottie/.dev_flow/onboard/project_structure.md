# Project Structure

```
remotecompose/lottie
├── api                     # API visibility tracking dumps
├── build.gradle.kts        # Gradle build script for the module
├── src
│   ├── debug               # Debug-specific sources and assets
│   │   ├── java/com/google/android/horologist/remotecompose/lottie # Previews
│   │   └── res/raw         # Sample Lottie JSON files for previews/tests
│   ├── main                # Main source set
│   │   └── java/com/google/android/horologist/remotecompose/lottie
│   │       ├── docs        # Existing documentation
│   │       ├── format      # Lottie JSON parsing and data classes (Decoder)
│   │       ├── renderer    # Rendering logic (converting parsed data to RemoteCompose commands)
│   │       ├── LottieAnimation.kt # Main Composable for rendering Lottie in Remote Compose
│   │       └── SlotMap.kt  # Utility for handling slots/replacements
│   └── test                # Unit and Screenshot tests
│       ├── java/com/google/android/horologist/remotecompose/lottie # Test classes
│       └── screenshots     # Baseline screenshots for Roborazzi
```
