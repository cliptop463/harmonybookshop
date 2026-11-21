package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalMarginDeskColors
import com.example.viewmodel.NormalizedPoint
import com.example.viewmodel.SignatureStroke

/**
 * Touch-drawable signature pad capturing normalized strokes (0f..1f) so signatures render
 * cleanly both on-screen and inside generated A4 PDF receipts.
 */
@Composable
fun SignaturePadCard(
    title: String,
    subtitle: String,
    strokes: List<SignatureStroke>,
    onStrokeCompleted: (SignatureStroke) -> Unit,
    onClear: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalMarginDeskColors.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val activeStrokePoints = remember { mutableStateListOf<NormalizedPoint>() }
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val baselineColor = semanticColors.cardBorder

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onClear,
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier.testTag("${testTagPrefix}_clear_button"),
                ) {
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = semanticColors.surfaceElevated,
                border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .testTag("${testTagPrefix}_canvas"),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (strokes.isEmpty() && activeStrokePoints.isEmpty()) {
                        Text(
                            text = "Sign here with finger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    val width = canvasSize.width.coerceAtLeast(1).toFloat()
                                    val height = canvasSize.height.coerceAtLeast(1).toFloat()

                                    activeStrokePoints.clear()
                                    activeStrokePoints.add(
                                        NormalizedPoint(
                                            x = (down.position.x / width).coerceIn(0f, 1f),
                                            y = (down.position.y / height).coerceIn(0f, 1f),
                                        )
                                    )

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                            ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }
                                        change.consume()
                                        activeStrokePoints.add(
                                            NormalizedPoint(
                                                x = (change.position.x / width).coerceIn(0f, 1f),
                                                y = (change.position.y / height).coerceIn(0f, 1f),
                                            )
                                        )
                                    }

                                    if (activeStrokePoints.isNotEmpty()) {
                                        onStrokeCompleted(SignatureStroke(activeStrokePoints.toList()))
                                        activeStrokePoints.clear()
                                    }
                                }
                            },
                    ) {
                        val baselineY = size.height * 0.78f
                        drawLine(
                            color = baselineColor.copy(alpha = 0.65f),
                            start = Offset(16f, baselineY),
                            end = Offset(size.width - 16f, baselineY),
                            strokeWidth = 2f,
                        )

                        val allStrokes = if (activeStrokePoints.isNotEmpty()) {
                            strokes + SignatureStroke(activeStrokePoints.toList())
                        } else {
                            strokes
                        }

                        allStrokes.forEach { stroke ->
                            if (stroke.points.size == 1) {
                                val p = stroke.points.first()
                                drawCircle(
                                    color = strokeColor,
                                    radius = 3.5f,
                                    center = Offset(p.x * size.width, p.y * size.height),
                                )
                            } else if (stroke.points.size > 1) {
                                val path = Path().apply {
                                    val first = stroke.points.first()
                                    moveTo(first.x * size.width, first.y * size.height)
                                    for (i in 1 until stroke.points.size) {
                                        val pt = stroke.points[i]
                                        lineTo(pt.x * size.width, pt.y * size.height)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(
                                        width = 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
