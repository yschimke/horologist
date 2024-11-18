package com.google.android.horologist.scratch


import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress


@Composable
fun Modifier.scrollTransform(
    scope: TransformingLazyColumnItemScope,
    backgroundColor: Color,
    shape: Shape = RectangleShape
): Modifier =
    with(scope) {
        var minMorphingHeight by remember { mutableStateOf<Float?>(null) }
        val spec = remember { LazyColumnScrollTransformBehavior { minMorphingHeight } }
        val painter =
            remember(backgroundColor, shape) {
                ScalingMorphingBackgroundPainter(
                    spec,
                    shape,
                    border = null,
                    backgroundPainter = ColorPainter(backgroundColor)
                ) {
                    scrollProgress
                }
            }
        this@scrollTransform then
                TargetMorphingHeightConsumerModifierElement { minMorphingHeight = it?.toFloat() }
                    .paint(painter)
                    .transformedHeight { height, scrollProgress ->
                        with(spec) {
                            scrollProgress.placementHeight(height.toFloat()).fastRoundToInt()
                        }
                    }
                    .graphicsLayer { contentTransformation(spec) { scrollProgress } }
    }

internal class LazyColumnScrollTransformBehavior(private val morphingMinHeight: () -> Float?) {
    // Scaling

    /** Scale factor applied to the item at the top edge of the LazyColumn. */
    private val topEdgeScalingFactor = 0.6f

    /** Scale factor applied to the item at the bottom edge of the LazyColumn. */
    private val bottomEdgeScaleFactor = 0.3f

    /** Easing applied to the scale factor at the top part of the LazyColumn. */
    private val topScaleEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Easing applied to the scale factor at the bottom part of the LazyColumn. */
    private val bottomScaleEasing = CubicBezierEasing(0f, 0f, 0.6f, 1f)

    // Alpha

    /** Alpha applied to the content of the item at the top edge of the LazyColumn. */
    private val topEdgeContentAlpha = 0.33f

    /** Alpha applied to the content of the item at the bottom edge of the LazyColumn. */
    private val bottomEdgeContentAlpha = 0f

    /** Alpha easing applied to the content of the item at the bottom edge of the LazyColumn. */
    private val bottomContentAlphaEasing = CubicBezierEasing(0.6f, 0f, 0.4f, 1f)

    /** Alpha applied to the background of the item at the top edge of the LazyColumn. */
    private val topEdgeBackgroundAlpha = 0.3f

    /** Alpha applied to the background of the item at the bottom edge of the LazyColumn. */
    private val bottomEdgeBackgroundAlpha = 0.15f

    // Morphing

    /** Multiplier used to drift the item's bottom edge around sticky bottom line. */
    private val driftFactor = 0.5f

    /**
     * Line to which the item's bottom edge will stick, as a percentage of the screen height, while
     * the rest of the content is morphing.
     */
    private val stickyBottomFlippedOffsetPercentage = 0.09f

    /** Final value of the width morphing as percentage of the width. */
    private val morphWidthTargetPercentage = 1f

    private val widthMorphEasing: CubicBezierEasing
        get() = bottomScaleEasing

    /** Height of an item before scaling is applied. */
    fun TransformingLazyColumnItemScrollProgress.morphedHeight(contentHeight: Float): Float =
        morphingMinHeight()?.let {
            val driftingBottomFraction =
                stickyBottomFlippedOffsetPercentage + (flippedBottomOffsetFraction * driftFactor)
            if (flippedBottomOffsetFraction < driftingBottomFraction) {
                val newHeight =
                    contentHeight * (flippedTopOffsetFraction - driftingBottomFraction) /
                            (flippedTopOffsetFraction - flippedBottomOffsetFraction)
                return maxOf(it, newHeight)
            } else {
                return@let contentHeight
            }
        } ?: contentHeight

    /** Height of an item after all effects are applied. */
    fun TransformingLazyColumnItemScrollProgress.placementHeight(contentHeight: Float): Float =
        morphedHeight(contentHeight) * scale

    private val TransformingLazyColumnItemScrollProgress.flippedTopOffsetFraction: Float
        get() = 1f - topOffsetFraction

    private val TransformingLazyColumnItemScrollProgress.flippedBottomOffsetFraction: Float
        get() = 1f - bottomOffsetFraction

    val TransformingLazyColumnItemScrollProgress.scale: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.5f ->
                    lerp(
                        bottomEdgeScaleFactor,
                        1f,
                        bottomScaleEasing.transform(
                            (0f..0.5f).progression(flippedTopOffsetFraction)
                        )
                    )
                flippedBottomOffsetFraction > 0.5f ->
                    lerp(
                        1f,
                        topEdgeScalingFactor,
                        topScaleEasing.transform(
                            (0.5f..1f).progression(flippedBottomOffsetFraction)
                        )
                    )
                else -> 1f
            }

    val TransformingLazyColumnItemScrollProgress.backgroundXOffsetFraction: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.3f ->
                    lerp(
                        morphWidthTargetPercentage,
                        1f,
                        widthMorphEasing.transform((0f..0.3f).progression(flippedTopOffsetFraction))
                    )
                else -> 1f
            }

    val TransformingLazyColumnItemScrollProgress.contentXOffsetFraction: Float
        get() = 1f - backgroundXOffsetFraction

    val TransformingLazyColumnItemScrollProgress.contentAlpha: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.03f -> 0f
                flippedTopOffsetFraction < 0.15f ->
                    lerp(
                        bottomEdgeContentAlpha,
                        1f,
                        bottomContentAlphaEasing.transform(
                            (0.03f..0.15f).progression(flippedTopOffsetFraction)
                        )
                    )
                flippedBottomOffsetFraction > 0.7f ->
                    lerp(
                        1f,
                        topEdgeContentAlpha,
                        (0.7f..1f).progression(flippedBottomOffsetFraction)
                    )
                else -> 1f
            }

    val TransformingLazyColumnItemScrollProgress.backgroundAlpha: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.3f ->
                    lerp(
                        bottomEdgeBackgroundAlpha,
                        1f,
                        (0f..0.3f).progression(flippedTopOffsetFraction)
                    )
                flippedBottomOffsetFraction > 0.6f ->
                    lerp(
                        1f,
                        topEdgeBackgroundAlpha,
                        (0.6f..1f).progression(flippedBottomOffsetFraction)
                    )
                else -> 1f
            }

    private fun ClosedRange<Float>.progression(value: Float) =
        ((value - start) / (endInclusive - start)).coerceIn(0f..1f)
}


private class ScalingMorphingBackgroundPainter(
    private val spec: LazyColumnScrollTransformBehavior,
    private val shape: Shape,
    private val border: BorderStroke?,
    private val backgroundPainter: Painter,
    private val progress: DrawScope.() -> TransformingLazyColumnItemScrollProgress?
) : Painter() {
    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        with(spec) {
            progress()?.let {
                val contentWidth =
                    (1f - 2 * (1f - it.backgroundXOffsetFraction)) * size.width * it.scale
                val xOffset = (size.width - contentWidth) / 2f

                translate(xOffset, 0f) {
                    val placementHeight = it.placementHeight(size.height)
                    val shapeOutline =
                        shape.createOutline(
                            Size(contentWidth, placementHeight),
                            layoutDirection,
                            this@onDraw
                        )

                    // TODO: b/376693576 - cache the path.
                    clipPath(Path().apply { addOutline(shapeOutline) }) {
                        if (border != null) {
                            drawOutline(
                                outline = shapeOutline,
                                brush = border.brush,
                                alpha = it.backgroundAlpha,
                                style = Stroke(border.width.toPx())
                            )
                        }
                        with(backgroundPainter) { draw(Size(contentWidth, placementHeight)) }
                    }
                }
            }
        }
    }
}

internal const val TargetMorphingHeightTraversalKey = "TargetMorphingHeight"

internal class TargetMorphingHeightConsumerModifierNode(
    var onMinMorphingHeightChanged: (Int?) -> Unit
) : Modifier.Node(), TraversableNode {
    override val traverseKey = TargetMorphingHeightTraversalKey
}

internal class TargetMorphingHeightConsumerModifierElement(
    val onMinMorphingHeightChanged: (Int?) -> Unit
) : ModifierNodeElement<TargetMorphingHeightConsumerModifierNode>() {
    override fun create() = TargetMorphingHeightConsumerModifierNode(onMinMorphingHeightChanged)

    override fun update(node: TargetMorphingHeightConsumerModifierNode) {
        node.onMinMorphingHeightChanged = onMinMorphingHeightChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "TargetMorphingHeightConsumerModifierElement"
        properties["onMinMorphingHeightChanged"] = onMinMorphingHeightChanged
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TargetMorphingHeightConsumerModifierElement) return false
        return onMinMorphingHeightChanged === other.onMinMorphingHeightChanged
    }

    override fun hashCode(): Int {
        return onMinMorphingHeightChanged.hashCode()
    }
}

internal fun GraphicsLayerScope.contentTransformation(
    spec: LazyColumnScrollTransformBehavior,
    scrollProgress: () -> TransformingLazyColumnItemScrollProgress?
) =
    with(spec) {
        scrollProgress()?.let {
            clip = true
            shape =
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline =
                        Outline.Rounded(
                            RoundRect(
                                rect =
                                    Rect(
                                        left = 0f,
                                        top = 0f,
                                        right =
                                            size.width -
                                                    2f * size.width * it.contentXOffsetFraction,
                                        bottom = it.morphedHeight(size.height)
                                    ),
                            )
                        )
                }
            translationX = size.width * it.contentXOffsetFraction * it.scale
            translationY = -1f * size.height * (1f - it.scale) / 2f
            alpha = it.contentAlpha.coerceAtMost(0.99f) // Alpha hack.
            scaleX = it.scale
            scaleY = it.scale
        }
    }