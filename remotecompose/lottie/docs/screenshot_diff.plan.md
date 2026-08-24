# Implementation Plan: Lottie Multi-Progress Screenshot Diff Tests {#PL_LOTTIE_DIFF}

> **Code:** PL_LOTTIE_DIFF
> **Status:** completed
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Concept:** [Lottie Screenshot Diff Concept](screenshot_diff.concept.md)
> **Specification:** [Lottie Screenshot Diff Specification](screenshot_diff.sp.md)
> **Specification Reference:** [TESTING_DOC.md](../src/main/java/com/google/android/horologist/remotecompose/lottie/docs/TESTING_DOC.md)
> **Depends on:** `C_LOTTIE_DIFF`, `SP_LOTTIE_DIFF`
> **Used by:** `LottieDiffScreenshotTest`, `MediaLottieDiffScreenshotTest`, `LottieFeatureDiffScreenshotTest`
>
> Implementation plan for upgrading the RemoteCompose Lottie screenshot diff testing framework: expanding animated test suites across `:remotecompose:lottie` to capture multi-frame progress milestones (0%, 50%, 100%), recording golden baselines, and cleaning obsolete single-frame artifacts.

## Goal

Enhance visual regression testing for `:remotecompose:lottie`:
1. **Multi-Progress Animation Coverage:** Upgrade all animated media tests (`geometry`, `playPause`, `next`, `m3PlayPause`, `m3Next`, `volumeUp`, `volumeDown`, `muteToUnmute`, `unmuteToMute`) to capture key milestones at 0%, 50%, and 100% progress.
2. **Clean Golden Baselines & Verification:** Record updated Roborazzi golden images, prune obsolete single-frame PNG files, and verify visual parity against reference `lottie-android` outputs across the full test suite.

---

## Commit & Task Isolation Rules (AGENTS.md Protocol)

Following the project's Spec-Driven Execution Protocol:
- **Atomic Commits:** Each task must be executed, verified, and committed independently before proceeding to the next.
- **Commit Titles:** Must describe the *effect* of the change (e.g. `Expand media Lottie screenshot diff tests with multi-frame animation milestones`), not implementation details.
- **Verification Commands:** Run the mandatory module verification pipeline in exact order before finalizing.

---

## Tasks & Phases

### Task 1: Upgrade Animated Test Cases to Multi-Progress Capture {#PL_LOTTIE_DIFF_T1}

- **Objective:** Configure multi-progress capture blocks across all animated tests in `MediaLottieDiffScreenshotTest.kt` and audit `LottieFeatureDiffScreenshotTest.kt`.
- **Files to Modify:**
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/MediaLottieDiffScreenshotTest.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Implementation Details:**
  - Update `MediaLottieDiffScreenshotTest.kt`:
    - `geometry`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `playPause`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `next`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `m3PlayPause`: Already configured (`0.0f`, `0.5f`, `1.0f`)
    - `m3Next`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `volumeUp`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `volumeDown`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `muteToUnmute`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
    - `unmuteToMute`: `captureProgress(0.0f)`, `captureProgress(0.5f)`, `captureProgress(1.0f)`
  - Verify `LottieFeatureDiffScreenshotTest.kt`:
    - `positionAnimated`: frames `0f`, `20f`, `40f`, `60f`.
    - Static tests (`positionStatic`, `rectEllipse`, `polystar`, `parentChain`, `transformSkew`): single snapshot at 0%.
- **Verification:**
  - Compile unit tests: `./gradlew :remotecompose:lottie:compileDebugUnitTestKotlin`
- **Commit Scope:**
  - **Commit (Functional):** `Expand media Lottie screenshot diff tests with multi-frame animation milestones`

---

### Task 2: Record Golden Baselines & Clean Obsolete Screenshots {#PL_LOTTIE_DIFF_T2}

- **Objective:** Generate golden Roborazzi images for all updated test suites, delete deprecated single-frame images, and run full module verification pipeline.
- **Files to Modify / Delete:**
  - Delete obsolete single-frame PNGs in `remotecompose/lottie/src/test/screenshots/` (e.g. `MediaLottieDiffScreenshotTest_geometry.png`, `MediaLottieDiffScreenshotTest_playPause.png`, `MediaLottieDiffScreenshotTest_next.png`, `MediaLottieDiffScreenshotTest_m3Next.png`, `MediaLottieDiffScreenshotTest_volumeUp.png`, `MediaLottieDiffScreenshotTest_volumeDown.png`, `MediaLottieDiffScreenshotTest_muteToUnmute.png`, `MediaLottieDiffScreenshotTest_unmuteToMute.png`).
  - Record new golden PNGs via `./gradlew :remotecompose:lottie:recordRoborazziDebug`.
- **Verification Pipeline:**
  1. Format Code: `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. Update Signatures: `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. Compile Kotlin: `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. Assemble Build: `./gradlew :remotecompose:lottie:assembleDebug`
  5. Run Unit Tests: `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. Run All Checks: `./gradlew :remotecompose:lottie:check`
  7. Verify Screenshots: `./gradlew :remotecompose:lottie:verifyRoborazziDebug`
- **Commit Scope:**
  - **Commit (Verification):** `Record multi-progress screenshot golden baselines and remove obsolete single-frame diffs`
