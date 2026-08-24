# RC Lottie Player for Devs

We can divide all the code into three parts:

* What to animate,
* Description of the animation, and
* Animation rendering

# What to animate

### **Shapes**

The key entity is the `Shape`: rectangles, circles, Bezier paths, … `Shape`s can be very different, and thus have different properties. But they also have common ones: Position, Scale, Rotation.

You can animate the size of a rectangle, the color of a fill, the width of a line, or even the coordinate points of a Bezier path morphing from a circle into a star. All of these properties can hold arrays of `Keyframe`s to change values smoothly across the timeline.

What `Shape`s do not have are the structural appear and disappear parameters (in-point and out-point).

### **Layers**

As in a file system we have files and folders, in Lottie we have `Shape` and `Layer`s. A `Layer` is essentially an invisible container, and changes to the `Layer` will be applied for whatever is placed inside it. A `Layer` has nothing to draw, it is just a bounding box.

A `Layer` can be parented to another `Layer`. `Shape`s themselves can't be parented to one another. You must put them in `Layer`s and parent the `Layer`s together to share transformation.

### **JSON representation**

Layers are stored as a completely flat array at the root of the animation under the layers key. They are not nested inside one another. Instead, they achieve hierarchy through a relational pointer called “parent”.

```json
{
  "layers": [
    {
      "nm": "Layer A (Parent)",
      "ty": 4,  // type = Shape Layer
      "ind": 1  // Unique ID is 1
    },
    {
      "nm": "Layer B (Child)",
      "ty": 4,
      "ind": 2, // Unique ID is 2
      "parent": 1 // Physically flat in the JSON, but logically parented to Layer A
    }
  ]
}
```

Unlike layers, Shapes are stored as an actual nested tree inside the layer that owns them. And Group (a type of Shape) can contain nested Shapes.

A Shape Layer has a shapes array. Inside that array, you can have physical Group objects, and those groups contain their own array with more shapes inside them.

```json
{
  "layers": [
    {
      "nm": "Layer B",
      "ty": 4,
      "shapes": [
        {
          "ty": "gr", // Group (Node)
          "it": [
            { "ty": "el" }, // Ellipse (Leaf)
            { "ty": "sr" }  // Star (Leaf)
          ]
        }
      ]
    }
  ]
}
```

### **Summary**

So, `Layer`s cut the movie into scenes, while `Shape`s are the entities moving and morphing within those scenes.

# Description of the animation

All animation is done via changing the properties of `Shape`s and `Layer`s over time.

`Layer`s own the existence timeline: a layer has an ip (in-point) and op (out-point). The layer completely ceases to exist outside of these two frames. It won't be calculated, so it saves memory. `Shape`s exist for the entire life of their layer. `Shape`s do not have in and out points: you cannot add or hide it.

If you define a `Keyframe` for a `Shape` outside of the parent `Layer`'s visible time period, the computer still interpolates the values between the `Keyframe`s, but it will simply not be drawn on the screen.

# Animation rendering

Due to the specifics of Remote Compose, all the drawing is formulated as an expression. The trigger is the ANIMATION\_TIME property of RC (or passing "progress" parameter).

The current frame is calculated based on the ANIMATION\_TIME or "progress". Filter out `Shape`s/`Layer`s that are invisible.

Then, using the current frame and `Keyframe`s (specific to each `Shape`s/`Layer`s) we create an expression to calculate the values of corresponding properties.

### **Lower-level details**

On the lower level, the parent LottieAnimation RemoteComposable creates a RemoteBox and loops over every layer in the JSON, calling the Layer composable for each one. Before doing so, it builds a map of parentTransforms so child layers can look up their parents.

# Example

End-to-end example walkthrough of rendering a single frame of animation using system clock time.

Let's freeze time when the animation has been playing for precisely 2.0 seconds (ANIMATION\_TIME \= 2.0).

### **The Scene Setup**

* Global Animation: 100 frames long at 30 fps (takes \~3.3 seconds to loop).
* Layer A (Parent Layer):
  * Time: Exists from frame 0 to 100\.
  * Transform: Moves left to right across the screen.
  * Content: Contains Shape 1 (a Red Square).
*  Layer B (Child Layer):
* Time: Exists from frame 20 to 80\.
* Parenting: Its Parent Index points to Layer A.
* Content: Contains Shape 2 (a Blue Circle with an animated glowing stroke) and Shape 3 (a Green Triangle).

### **Step 1: The Master Clock Calculation**

The RemoteCompose engine calculates the global state equation:

`currentFrame = floor(ANIMATION_TIME * 30fps) % 100 totalFrames`

Calculation: floor(2.0 \* 30\) % 100 \= 60

Result: We need to draw Frame 60

### **Step 2: Evaluating Layer A**

The engine loops through the layers and finds Layer A.



1\. Visibility Check: Is 60 between Layer A's in-point (0) and out-point (100)? Yes.

2\. Transform Calculation: The engine looks at Layer A's position keyframes, calculates the math for frame 60, and determines the layer is currently 60% across the screen. It

creates a "Matrix" (a mathematical grid) shifted to that exact spot.

3\. Drawing Shape 1: It enters Layer A's contents. It draws the Red Square inside the shifted Matrix.



(On screen right now: A single red square shifted to the right.)

### **Step 3: Evaluating Layer B (The Child)**

Next, the engine looks at Layer B.



1\. Visibility Check: Is 60 between Layer B's in-point (20) and out-point (80)? Yes, it's alive.

(Note: If the time was 1.0 seconds, the frame would be 30, and Layer B would be alive. But if the time was 3.0 seconds, the frame would be 90\. The engine would skip Layer B

entirely. It would not bother doing any of the math below, saving precious watch battery).

2\. Transform Calculation: Because Layer B is a child of Layer A, it grabs Layer A's shifted Matrix (the one currently 60% across the screen) and multiplies its own local

transformations on top of it.

3\. Drawing Shape 2: It enters Layer B and finds the Blue Circle. It evaluates the animation keyframes on the glowing stroke specifically for frame 60, and paints the circle

according to the inherited, parented Matrix.

4\. Drawing Shape 3: It finds the Green Triangle. It has no internal animated properties, so it just paints a static green triangle directly into the same parented Matrix next

to the Blue Circle.



(On screen right now: A red square, and riding piggy-back on top of that square's invisible bounding box are a glowing blue circle and a static green triangle).



\#\#\# The "Remote" Magic



Because this is Remote Compose, the watch processor doesn't run this top-to-bottom logic block frame-by-frame on the CPU like Android Views do.



Instead, the phone app sends a massive, compiled mathematical tree over bluetooth that says: "Hey watch, draw the Blue Circle, but its X coordinate is equal to: (equation for

Layer A's position at ANIMATION\_TIME \* 30 % 100\) \+ (equation for Layer B's position at that same time)".



As the watch's hardware clock (ANIMATION\_TIME) ticks forward to 2.1, 2.2, 2.3 seconds, those mathematical expressions automatically update natively in the GPU, driving the

shapes effortlessly\!                                      

