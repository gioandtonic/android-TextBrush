package com.svs.textbrush

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import com.svs.textbrush.ui.theme.TextBrushTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas() {
    // State to hold the drawing path
    var points by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var pathsSaved by remember { mutableStateOf<List<Path>>(emptyList()) }


    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(remember { MutableInteractionSource() }) {
            detectDragGestures(
                onDragStart = { offset ->
                    // Start a new path or subpath
                    points = listOf(offset)
                }, onDrag = { change, dragAmmount ->
                    change.consume()
                    val pointsFromHistory = change.historical
                        .map { it.position }
                        .toTypedArray()
                    val newPoints = listOf(*pointsFromHistory, change.position)
                    points = points + newPoints
                }, onDragEnd = {
                    Log.i("POINTS2", "${points.size}")

                    if (points.isNotEmpty()) {
                        val path = Path().apply {
                            val firstPoint = points.first()
                            val remaining = points.subList(1, points.size - 1)

                            moveTo(firstPoint.x, firstPoint.y)
                            remaining.forEach {
                                lineTo(it.x, it.y)
                            }
                        }
                        pathsSaved = pathsSaved + path
                    }
                })
        }) {
        // Draw the path
        pathsSaved.forEach { path ->
            drawPath(path, color = Color.Black, style = Stroke(width = 2f))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DrawingCanvasPreview() {
    TextBrushTheme {
        DrawingCanvas()
    }
}