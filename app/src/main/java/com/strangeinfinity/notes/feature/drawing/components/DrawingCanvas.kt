package com.strangeinfinity.notes.feature.drawing.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.strangeinfinity.notes.core.model.DrawingPoint
import com.strangeinfinity.notes.core.model.DrawingStroke

@Composable
fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    currentStrokeColor: Long,
    currentStrokeWidth: Float,
    isEraser: Boolean,
    canvasBackgroundColor: Color,
    onStrokeFinished: (DrawingStroke) -> Unit,
    modifier: Modifier = Modifier
) {
    val livePoints = remember { mutableStateListOf<DrawingPoint>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBackgroundColor)
            .pointerInput(currentStrokeColor, currentStrokeWidth, isEraser) {
                detectDragGestures(
                    onDragStart = { offset ->
                        livePoints.clear()
                        livePoints.add(DrawingPoint(offset.x, offset.y))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        livePoints.add(DrawingPoint(change.position.x, change.position.y))
                    },
                    onDragEnd = {
                        if (livePoints.isNotEmpty()) {
                            val stroke = DrawingStroke(
                                points = livePoints.toList(),
                                color = if (isEraser) canvasBackgroundColor.value.toLong() else currentStrokeColor,
                                strokeWidth = if (isEraser) currentStrokeWidth * 2.5f else currentStrokeWidth,
                                isEraser = isEraser
                            )
                            onStrokeFinished(stroke)
                            livePoints.clear()
                        }
                    },
                    onDragCancel = {
                        livePoints.clear()
                    }
                )
            }
    ) {
        // Render completed strokes
        strokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                val path = Path()
                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    val p0 = stroke.points[i - 1]
                    val p1 = stroke.points[i]
                    // Smooth midpoint quadratic bézier
                    val midX = (p0.x + p1.x) / 2
                    val midY = (p0.y + p1.y) / 2
                    path.quadraticTo(p0.x, p0.y, midX, midY)
                }
                val lastPoint = stroke.points.last()
                path.lineTo(lastPoint.x, lastPoint.y)

                drawPath(
                    path = path,
                    color = if (stroke.isEraser) canvasBackgroundColor else Color(stroke.color),
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (stroke.points.size == 1) {
                drawCircle(
                    color = if (stroke.isEraser) canvasBackgroundColor else Color(stroke.color),
                    radius = stroke.strokeWidth / 2,
                    center = Offset(stroke.points[0].x, stroke.points[0].y)
                )
            }
        }

        // Render live stroke being drawn
        if (livePoints.size > 1) {
            val livePath = Path()
            livePath.moveTo(livePoints[0].x, livePoints[0].y)
            for (i in 1 until livePoints.size) {
                val p0 = livePoints[i - 1]
                val p1 = livePoints[i]
                val midX = (p0.x + p1.x) / 2
                val midY = (p0.y + p1.y) / 2
                livePath.quadraticTo(p0.x, p0.y, midX, midY)
            }
            val last = livePoints.last()
            livePath.lineTo(last.x, last.y)

            drawPath(
                path = livePath,
                color = if (isEraser) canvasBackgroundColor else Color(currentStrokeColor),
                style = Stroke(
                    width = if (isEraser) currentStrokeWidth * 2.5f else currentStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
