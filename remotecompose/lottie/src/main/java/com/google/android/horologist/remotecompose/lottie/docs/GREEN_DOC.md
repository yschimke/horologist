# Remote Compose Lottie Player

| \#begin-approvals-addon-section See [go/g3a-approvals](http://goto.google.com/g3a-approvals) for instructions on how to add reviewers. Do not edit this section manually. |
| :---: |

**Self link:** [go/lottie-rc](http://goto.google.com/lottie-rc)  
**Visibility:** Confidential  
**Authors:** [Mark Yavorskyi](mailto:myavorskyi@google.com)  
**Last major revision:** Jul 15, 2026

# Objective

Enable external developers to display Lottie animations in Remote Compose applications by providing a useful and well tested published open-source library.

# Background

Lottie is a JSON-based animation file format describing vector graphics and animations using keyframes. Remote Compose (short: RC) is an AndroidX library enabling developers to define user interfaces in Jetpack Compose syntax, serialize them into a binary format (CoreDocument), and render it on a remote host (Player).

Remote Compose lacks general-purpose Lottie animation playback. A prototype exists with limited features. This proposal aims to transition from the prototype to a production-ready solution supporting diverse Lottie files for Android and Wear OS apps.

# Requirements

- [x] ~~\[P0\] Make the Remote Compose Lottie Player an open source library~~
- [x] ~~\[P0\] Ensure the test coverage (including unit, integration, and system tests)~~
- [x] ~~\[P0\] Ensure the lib’s API match the Remote Compose API patterns~~
- [x] ~~\[P1\] Move to kotlinx.serialization instead of Moshi~~
- [ ] \[P1\] Support the cubic Bézier curve for Lottie animation
- [ ] \[P1\] Ensure the Remote Compose Lottie Player supports common Lottie features in \[public Lottie samples, specification\] and correctly displays them
- [ ] \[P2\] Have public demos/samples
- [ ] \[P3\] Support the regular and low-power profile on Wear OS
- [ ] \[P3\] Benchmark the performance

# Proposal

Continue to build on the existing prototype and add vector graphical features to support the existing Lottie animations.

Implement the Lottie Player as a Kotlin library that runs on the Remote Compose writer side. The library will:

1. parse a Lottie JSON file and translate its structures into Animation object (internal representation, AST model).
2. Render the Animation object by emitting drawing primitives within a @RemoteComposable function.

The consequencing actions, performed on the resulting @RemoteComposable function, is out of the scope of this library.

API refinement and internal [optimization](#optimizations) should go first before adding graphical features.

## Library Hosting

Host the library inside the **Horologist** repository under `com.google.android.horologist.remotecompose.lottie`. See [A1. Code Hosting](#a1.-code-hosting) for more details.

## Supported Lottie features

We will adopt a Spec-Driven Scoping strategy (see [A2. Feature Scoping Strategy](#a2.-feature-scoping-strategy)). We will examine the Lottie Specifications to create a definitive categorization of features into supported and unsupported sets for this library. This approach provides a clear roadmap and boundary for the library, ensuring developers know exactly which Lottie files are compatible. Resources:

1. [Lottie supported features](https://lottiefiles.com/supported-features)
2. [Lottie Animation Format](https://lottie.github.io/lottie-spec/1.0.1/)

## Optimizations {#optimizations}

As it has been considerable amount of time passed since the prototype last update, accumulated changes in G3 allows us to optimize some features:

* Bézier Curves: Use native cubic Bézier curve ([link 1](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/Easing.kt?q=file:androidx%2Fcompose%2Fanimation%2Fcore%2FEasing.kt%20class:androidx.compose.animation.core.CubicBezierEasing)) (via "lookup point in a bezier curve" operation which hoford@ added) in Remote Compose instead of pre-calculating arrays.
* Serialization: Use kotlinx.serialization instead of Moshi to remove reflection overhead and reduce app size.

# Alternatives considered

## A1. Code Hosting {#a1.-code-hosting}

Option A: Dedicated GitHub Repository

* Pros: Decoupling from Wear OS (as it is not Wear OS specific).
* Cons: High overhead (paperwork) for setup; who will be responsible for maintenance after Mark finishes his internship?
* Decision: Rejected due to high overhead and vague maintenance responsibility.

Option B: Direct Integration into androidx.compose.remote

* Pros: Core framework integration.
* Cons: Slower release cycles, strict API reviews.
* Decision: Rejected for initial phases. Target Horologist for rapid iteration.

Option C: Horologist (Proposed)

* Pros: Existing infrastructure for releases and bug monitoring, active maintainers, lower administrative overhead.
* Cons: Horologist is a group of libraries supplementing Wear OS, whereas Remote Compose is general-purpose (not Wear OS specific).
* Decision: Selected as the initial hosting location.

## A2. Feature Scoping Strategy {#a2.-feature-scoping-strategy}

Option A: Sample-Driven Scoping

Select 5–10 representative real-world public Lottie samples, audit the vector graphical features they rely on, and map them to Remote Compose.

* Pros: Prioritizes features used in actual animations; enables rapid delivery of value for common use cases.
* Cons: May miss less common but important features or edge cases.

Option B: Spec-Driven Scoping

Examine the Lottie supported features and Lottie Specifications to create a definitive list of supported and unsupported features.

* Pros: Provides a comprehensive understanding of library capabilities and a clear roadmap.
* Cons: May consume resources on features that are rarely used in practice.
