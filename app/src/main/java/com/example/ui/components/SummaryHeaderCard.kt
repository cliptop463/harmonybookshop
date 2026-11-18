package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.ui.theme.LocalMarginDeskColors
import com.example.ui.theme.MonoMetricLarge
import com.example.util.CurrencyFormatter
import com.example.viewmodel.InventorySummary

@Composable
fun SummaryHeaderCard(
    summary: InventorySummary,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalMarginDeskColors.current
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    val animatedValue by animateFloatAsState(
        targetValue = summary.totalInventoryValue.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "inventory_value_anim",
    )
    val animatedAvgMargin by animateFloatAsState(
        targetValue = summary.averageProfitMargin.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "avg_margin_anim",
    )

    val marginColor by animateColorAsState(
        targetValue = when {
            summary.totalActiveItems == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
            summary.averageProfitMargin > 0.0 -> semanticColors.profitContent
            summary.averageProfitMargin < 0.0 -> semanticColors.lossContent
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "summary_margin_color",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_header_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.7f)),
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.15f),
                            radius = size.width * 0.6f,
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.15f),
                        radius = size.width * 0.6f,
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryMetricColumn(
                    label = "ACTIVE ITEMS",
                    value = summary.totalActiveItems.toString(),
                    sublabel = "${summary.totalUnits} units",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.9f),
                )
                SummaryMetricColumn(
                    label = "INVENTORY VALUE",
                    value = CurrencyFormatter.formatPrice(
                        amount = animatedValue.toDouble(),
                        currencySymbol = settings.currencySymbol,
                        decimalPlaces = settings.decimalPlaces,
                    ),
                    sublabel = "At cost",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.25f),
                )
                SummaryMetricColumn(
                    label = "AVG MARGIN",
                    value = if (summary.totalActiveItems == 0) {
                        "0.0%"
                    } else {
                        CurrencyFormatter.formatMarginPercent(animatedAvgMargin.toDouble())
                    },
                    sublabel = "Per unit avg",
                    valueColor = marginColor,
                    alignment = Alignment.End,
                    modifier = Modifier.weight(0.95f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricColumn(
    label: String,
    value: String,
    sublabel: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "summary_metric_text",
        ) { targetText ->
            Text(
                text = targetText,
                style = MonoMetricLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
