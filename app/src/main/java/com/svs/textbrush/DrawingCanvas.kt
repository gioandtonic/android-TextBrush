package com.svs.textbrush

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.svs.textbrush.ui.theme.TextBrushTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas() {
    // State to hold the drawing path
    var points by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val pathsSaved = remember { mutableStateListOf<Path>() }
    val textStyle = TextStyle.Default.copy(color = Color.White, fontSize = 24.sp)
    val textSizePx = textStyle.fontSize.value * LocalDensity.current.density
    val textToDraw = "Text Brush ".toCharArray()
    val paint = configurePaintFromTextStyle(textStyle)
    val letterSpacingPx =
        if ((textStyle.letterSpacing.value * LocalDensity.current.density).isNaN()) 0f
        else
            textStyle.letterSpacing.value * LocalDensity.current.density
    val imagePainter = painterResource(id = R.drawable.background)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = imagePainter, contentDescription = "Background",
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.Center),
            contentScale = ContentScale.FillWidth
        )
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(remember { MutableInteractionSource() }) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Start a new path or subpath
                        points = listOf(offset)
                    }, onDrag = { change, _ ->
                        change.consume()
                        val pointsFromHistory = change.historical
                            .map { it.position }
                            .toTypedArray()
                        val newPoints = listOf(*pointsFromHistory, change.position)
                        points = points + newPoints
                    }, onDragEnd = {
                        if (points.isNotEmpty()) {
                            val path = Path().apply {
                                val firstPoint = points.first()
                                val remaining = points.subList(1, points.size - 1)

                                moveTo(firstPoint.x, firstPoint.y)
                                remaining.forEach {
                                    lineTo(it.x, it.y)
                                }
                            }
                            pathsSaved.add(path)
                        }
                    })
            }) {
            // Draw the path
            pathsSaved.forEach { path ->
                //Uncomment this to verify the placement of text across the path
                //drawPath(path, color = Color.Black, style = Stroke(width = 2f))

                val pathMeasure = PathMeasure()
                pathMeasure.setPath(path, false)
                val pathLength = pathMeasure.length

                var currentPathPosition = 0f
                val drawTimes = getTextDrawTimes(textToDraw, textSizePx, letterSpacingPx)

                for (i in 1..drawTimes) {
                    var prevPosition = Offset.Unspecified

                    textToDraw.forEach innerLoop@{
                        if (currentPathPosition >= pathLength)
                            return@innerLoop
                        val position = pathMeasure.getPosition(currentPathPosition)


                        currentPathPosition += (textSizePx + letterSpacingPx)
                        val nextPosition = pathMeasure.getPosition(currentPathPosition)
                        val isFirst = prevPosition == Offset.Unspecified
                        val isLast =
                            nextPosition == Offset.Unspecified && position != Offset.Unspecified

                        //calculate tangent
                        val firstPoint: Offset
                        val lastPoint: Offset

                        when {
                            isFirst -> {
                                firstPoint = position
                                lastPoint = nextPosition
                            }

                            isLast -> {
                                firstPoint = prevPosition
                                lastPoint = position
                            }

                            else -> {
                                firstPoint = prevPosition
                                lastPoint = nextPosition
                            }
                        }

                        val tangent = calculateTangent(Pair(firstPoint, lastPoint))

                        drawChar(
                            char = it,
                            charPosition = tangent + position,
                            charRotation = calculateRotationAngle(tangent).toFloat(),
                            paint = paint
                        )

                        prevPosition = position
                    }
                }
            }
        }

        Button(
            onClick = {
                removeLast(pathsSaved)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)// Aligns the button to the bottom-end corner
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack, // Use the appropriate icon
                contentDescription = "Undo", // Provide a content description for accessibility
                modifier = Modifier // Apply any required Modifier
            )
        }
    }
}

fun removeLast(paths: SnapshotStateList<Path>) {
    if (paths.isNotEmpty())
        paths.removeLast()
}

fun calculateRotationAngle(tangent: Offset): Double {
    val angleRadians = atan2(tangent.y, tangent.x)

    return angleRadians * (180.0 / PI)
}

fun calculateTangent(points: Pair<Offset, Offset>): Offset {
    // Calculate the difference between the points
    val dx = points.second.x - points.first.x
    val dy = points.second.y - points.first.y

    // Normalize the tangent vector
    val length = sqrt(dx * dx + dy * dy.toDouble()).toFloat()
    val normalizedDx = dx / length
    val normalizedDy = dy / length

    return Offset(normalizedDx, normalizedDy)
}

fun getTextDrawTimes(text: CharArray, textSizePx: Float, letterSpacingPx: Float) =
    text.size * (textSizePx + letterSpacingPx).roundToInt()

fun DrawScope.drawChar(char: Char, charPosition: Offset, charRotation: Float, paint: Paint) {

    withTransform(
        transformBlock = {
            rotate(degrees = charRotation, pivot = charPosition)
        }
    ) {
        drawContext.canvas.nativeCanvas.drawText(
            char.toString(),
            charPosition.x,
            charPosition.y,
            paint
        )
    }
}

@Composable
fun configurePaintFromTextStyle(textStyle: TextStyle): Paint {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textStyle.color.toArgb()
        textSize = textStyle.fontSize.value * LocalDensity.current.density

        // Convert Compose's TextStyle to Android Typeface
        typeface = textStyle.fontFamily?.toTypeface(
            fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
            fontStyle = textStyle.fontStyle
        ) ?: Typeface.DEFAULT

        // Letter Spacing
        letterSpacing =
            if ((textStyle.letterSpacing.value * LocalDensity.current.density).isNaN()) 0f else textStyle.letterSpacing.value * LocalDensity.current.density


        // Text Decoration
        if (textStyle.textDecoration == TextDecoration.Underline) {
            isUnderlineText = true
        }
        if (textStyle.textDecoration == TextDecoration.LineThrough) {
            isStrikeThruText = true
        }
    }
    return paint
}


fun FontFamily?.toTypeface(
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle? = null
): Typeface {
    // Handle system font families first
    val defaultTypeface = when (this) {
        FontFamily.SansSerif -> Typeface.SANS_SERIF
        FontFamily.Serif -> Typeface.SERIF
        FontFamily.Monospace -> Typeface.MONOSPACE
        else -> null // Placeholder for custom fonts
    }

    // If it's a system font, return it
    defaultTypeface?.let { return it }

    return Typeface.DEFAULT
}

@Preview(showBackground = true)
@Composable
fun DrawingCanvasPreview() {
    TextBrushTheme {
        DrawingCanvas()
    }
}