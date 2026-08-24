# “Blue doc” design doc template

| \#begin-approvals-addon-section See [go/g3a-approvals](http://goto.google.com/g3a-approvals) for instructions on how to add reviewers. Do not edit this section manually. |
| :---: |

**Self link:** [go/lottie-rc](http://goto.google.com/lottie-rc)  
**Visibility**: Confidential  
**Status**: Draft  
**Authors**: [Mark Yavorskyi](mailto:myavorskyi@google.com)  
**Contributors**: Person, Person  
**Team**: [Remote Compose](https://moma.corp.google.com/team/1607465629501)  
**PRD**: *This is where the go/ link to your [PRD](https://moma.corp.google.com/glossary/prd) should be. Use [go/indigodoc](http://go/indigodoc) to write it.*  
**Tracking Buganizer issue/hotlist**: b/  
**Last major revision**: Jul 28, 2026

# Context

## Objective

Enable external developers to display Lottie animations in Remote Compose applications by providing a useful and well tested published  open-source library.

## Background

Lottie is a JSON-based animation file format describing vector graphics and animations using keyframes. Remote Compose (short: RC) is an AndroidX library enabling developers to define user interfaces in Jetpack Compose syntax, serialize them into a binary format (CoreDocument), and render it on a remote host (Player).

Remote Compose lacks general-purpose Lottie animation playback. A prototype exists with limited features. This proposal aims to transition from the prototype to a production-ready solution supporting diverse Lottie files for Android and Wear OS apps.

# Design

## Overview

Continue to build on the existing prototype and add vector graphical features to support the existing Lottie animations.

Implement the Lottie Player as a Kotlin library that runs on the Remote Compose writer side. The library will:

1. Parse a Lottie JSON file and translate its structures into an Animation object (internal representation, Abstract syntax tree (AST) model).
2. Render the Animation object by emitting drawing primitives within a @RemoteComposable function.

The consequencing actions, performed on the resulting @RemoteComposable function, is out of the scope of this library.

API refinement and internal optimization should go first before adding graphical features.

Changes:

1. Make the internal structure (Layer, Animation, GraphicElement, …) internal classes.
2. There are some comments referencing bugs that are closed. We have to address them.
3.  In LottiePreview: remove RemoteColumn because as the defaults provided (black background, center aligned) might not suit every use case.
4.  Maybe move LottiePreview in our tests or debug source set?

## Infrastructure

* [Horologist](https://github.com/google/horologist) open-source GitHub repository: Gradle/Blaze build configuration, code quality checks.
* [AndroidX Remote Compose](https://developer.android.com/jetpack/androidx/releases/compose-remote): Writer engine, @RemoteComposable compiler plugin, and RemoteCreationScope.
* Kotlin 2.x & kotlinx.serialization: JSON deserialization framework.
* Horologist Test Framework: Compose UI screenshot and layout rendering verification suite on Wear OS.

## Detailed design

### **Package Structure & Visibility**

```kotlin
com.google.android.horologist.remotecompose.lottie
├── LottieAnimation.kt       // @Composable @RemoteComposable entry point (public)
├── SlotMap.kt               // Dynamic property overrides (theming) (public)
├── format/                  // AST model & kotlinx.serialization decoders
│   ├── Animation.kt
│   ├── Layer.kt             // ShapeLayer, NullLayer, SolidLayer
│   ├── GraphicElement.kt    // Path, Group, Fill, Stroke, Transform, TrimPath
│   └── Property.kt          // AnimatableProperty & Bezier keyframes
└── renderer/                // Remote Compose rendering engine
    ├── LottieRenderer.kt
    └── BezierEvaluator.kt   // CubicBezierEasing evaluator
```

### **Public API Surface**

The API provides two simple public entry points: an animation can be loaded from a saved resource or a JSON string.

```kotlin
// Render directly from a raw resource ID
@Composable
@RemoteComposable
public fun LottieAnimation(
    @RawRes rawRes: Int,
    modifier: RemoteModifier = RemoteModifier,
    slotMap: SlotMap? = null,
)

// Render directly from a JSON string
@Composable
@RemoteComposable
public fun LottieAnimation(
    json: String,
    modifier: RemoteModifier = RemoteModifier,
    slotMap: SlotMap? = null,
)
```

The SlotMap is a list of color/style overrides. SlotMap lets you swap "red" to "blue" color at runtime without having to edit the original animation file.

### **Internal Animation AST structure (format/ folder)**

[Link](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-helpers:~:text=of%20left%20multiplication.-,Visual%20Object,-Composition%20Diagram%20for) to the source in the official documentation. The idea is to recreate this structure in code.

### **renderer/ package**

While in the format/ folder we had pure data objects without logic, in renderer/ folder we have the rendering logic. The renderer engine translates the decoded Lottie AST (Animation, Layer, GraphicElement) into dynamic RemoteCompose canvas drawing operations (RemoteCanvas, RemotePath, RemotePaint). It is done according to the animation clock (ANIMATION\_TIME).

It follows a 4-stage pipeline:

| Input: AST Data (format/) \- Layer structure, Keyframes (t, s, e), Path Vertices, Colors, Transforms |
| :---- |

⬇️

| Stage 1: Keyframe Interpolation \- Takes *currentFrame* (from ANIMATION\_TIME). \- Evaluates animated properties using RemoteCompose conditional logic (*selectIfLt* \+ *lerp* \+ *CubicBezierEasing*). Output: Dynamic reactive *RemoteFloat*, *RemoteColor*, *RemoteBezierValue* |
| :---- |

⬇️

| Stage 2: Shape Gathering & Path Construction \- Takes interpolated values from Stage 1\. \- Converts AST paths (*sh*, *rc*, *el*) into dynamic *RemotePath* commands. \- Resolves fill/stroke paints & SlotMap color overrides into *RemoteStyle*. Output: *StyledShapes* (Group of *RemotePath*s \+ *RemotePaint*) |
| :---- |

⬇️

| Stage 3: Transform Stack \- Combines layer transform & parent layer transforms. \- Applies matrix operations in spec order: *translate(pos)* → *rotate(r)* → *scale(s)* → *translate(-anchor)* Output: Canvas matrix boundary (*canvas.save()* ... *canvas.restore()*) |
| :---- |

⬇️

| Stage 4: RemoteCanvas Drawing Operations \- Binds *RemotePaint* for current shape group via *usePaint(...)*. \- Executes *drawPath(remotePath)* calls on *RemoteCanvas*. Output: Executable RemoteCompose drawing instructions |
| :---- |

#### **Example**

Suppose a red circle moves from X \= 0 to X \= 100 over 2 seconds:

1. AST Data:  
   Position \= Keyframe(0s \-\> 0, 2s \-\> 100\)  
   Shape \= Ellipse(radius \= 20\)  
   Fill \= Red.

2. Stage 1 (Interpolation expression): constructs a lambda (dynamic expression, formula)  
   PositionX \= lerp(0, 100, ANIMATION\_TIME / 2s)  
   as a RemoteFloat placeholder (does not set any value to X yet).

3. Stage 2 (Shape & Path): ellipse constructs a circle RemotePath centered at origin (0,0). Fill constructs a red RemotePaint.

4. Stage 3 (Transform Stack): applies canvas origin shift translate(PositionX, 0\) inside canvas.save()...canvas.restore().

5. Stage 4 (Canvas Draw): Inside @RemoteComposable, RemoteCanvas executes drawPath using the red paint, rendering the circle at (PositionX,0).

#### **Structure**

```kotlin
com.google.android.horologist.remotecompose.lottie.renderer/
├── LayerRenderer.kt       // @RemoteComposable layer hierarchy & parent transform stack traversal
├── KeyframeEvaluator.kt   // Interpolates properties/keyframes over time (CubicBezierEasing ? RemoteFloat)
├── ShapeGatherer.kt       // Traverses GraphicElements into StyledShapes (RemoteShape +   RemoteStyle)
├── RemoteShape.kt         // Drawing primitives (RemoteLottiePath, RemoteGroup) translating AST to RemotePath    
├── TransformRenderer.kt   // Applies matrix operations (translate, rotate, scale) within canvas save/restore          
└── SlotMap.kt             // Resolves dynamic theme & color overrides at runtime
```

```kotlin
com.google.android.horologist.remotecompose.lottie.renderer/
├── LayerRenderer.kt
├── KeyframeEvaluator.kt
├── ShapeGatherer.kt
├── RemoteShape.kt   
├── TransformRenderer.kt        
└── SlotMap.kt
```

| Class / Module | Primary Responsibility | Input | Output |
| :---- | :---- | :---- | :---- |
| LayerRenderer | Resolves timeline layers (ShapeLayer, NullLayer, SolidLayer) and handles parent-child transform chaining. | Layer, parentTransforms | Composables |
| KeyframeEvaluator | Interpolates scalar/vector/path keyframes dynamically using selectIfLt (IFELSE) and native CubicBezierEasing. | AnimatableProperty | RemoteFloat / RemoteBezierValue |
| ShapeGatherer | Combines nested shapes (Path, Rect, Ellipse, Fill, Stroke, TrimPath) into draw groups. | List\<GraphicElement\> | List\<StyledShape\> |
| RemoteShape | Translates AST shape geometries into dynamic RemotePath commands and emits RemoteCanvas draw calls. | GraphicElement.Path / Group | RemotePath & Canvas draw operations |
| TransformRenderer | Applies matrix operations (translate, rotate, scale, translate(-anchor)) within canvas.save() / canvas.restore(). | Transform AST node | Canvas matrix coordinate state |
| SlotMap | Resolves dynamic runtime theme and color overrides. | Slot ID \+ default color | RemoteColor override |

### **Key Design Decisions**

1. Private AST Format: Keep Animation, Layer, GraphicElement, and keyframe classes internal. Public consumers use @RawRes rawRes or json: String overloads.
2. Retain SlotMap: Retain optional slotMap: SlotMap? \= null parameter for dynamic property and color overrides (e.g. Wear OS system theme matching).
3. kotlinx.serialization Migration: Replace Moshi with kotlinx.serialization.json.Json to eliminate reflection overhead and reduce app binary size.
4. Native Bezier Easing: Evaluate keyframe curves via AndroidX CubicBezierEasing lookup instead of pre-calculating frame array tables.
5. Lottie Spec (1.0.1) Feature Mapping:
  1. Supported Shapes: Group (\`gr\`), Path (\`sh\`), Rectangle (\`rc\`), Ellipse (\`el\`), Fill (\`fl\`), Stroke (\`st\`), Transform (\`tr\`), Trim Path (\`tm\`).
  2. Supported Layers: Shape Layer (4), Null Layer (3), Solid Layer (1).
6. Cleanups:
  1. Move LottiePreview from public API to debug / test source set and remove RemoteColumn wrapper.
  2. Resolve obsolete/closed bug reference comments.
