package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.Product
import com.example.ui.theme.LocalMarginDeskColors
import com.example.ui.theme.MonoCodeBadge
import com.example.ui.theme.MonoMetricMedium
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductCardItem(
    product: Product,
    settings: AppSettings,
    onCardClick: (Product) -> Unit,
    onIncrement: (Product) -> Unit,
    onDecrement: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalMarginDeskColors.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            if (targetValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete(product)
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_swipe_${product.id}"),
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                },
                label = "swipe_delete_bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete product",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        val isProfit = product.profit > 0.0
        val isLoss = product.profit < 0.0
        val isLowStock = product.quantity <= settings.lowStockThreshold

        val badgeContainerColor by animateColorAsState(
            targetValue = when {
                isProfit -> semanticColors.profitContainer
                isLoss -> semanticColors.lossContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            animationSpec = tween(durationMillis = 220),
            label = "badge_container",
        )
        val badgeContentColor by animateColorAsState(
            targetValue = when {
                isProfit -> semanticColors.profitContent
                isLoss -> semanticColors.lossContent
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(durationMillis = 220),
            label = "badge_content",
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onCardClick(product) }
                .testTag("product_card_${product.id}"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (isLoss) {
                    semanticColors.lossContent.copy(alpha = 0.45f)
                } else {
                    semanticColors.cardBorder.copy(alpha = 0.65f)
                },
            ),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (!product.skuOrCode.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = semanticColors.surfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        semanticColors.cardBorder.copy(alpha = 0.7f),
                                    ),
                                ) {
                                    Text(
                                        text = product.skuOrCode,
                                        style = MonoCodeBadge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            if (isLowStock) {
                                val outOfStock = product.quantity == 0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (outOfStock) {
                                        semanticColors.lossContainer
                                    } else {
                                        semanticColors.lowStockContainer
                                    },
                                    modifier = Modifier.testTag("low_stock_badge_${product.id}"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = null,
                                            tint = if (outOfStock) {
                                                semanticColors.lossContent
                                            } else {
                                                semanticColors.lowStockContent
                                            },
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = if (outOfStock) "Out of Stock" else "Low Stock",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (outOfStock) {
                                                semanticColors.lossContent
                                            } else {
                                                semanticColors.lowStockContent
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Highlighted profit & margin badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeContainerColor,
                        border = BorderStroke(1.dp, badgeContentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("profit_badge_${product.id}"),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            AnimatedContent(
                                targetState = CurrencyFormatter.formatMarginPercent(product.profitMargin),
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "margin_badge_anim",
                            ) { marginText ->
                                Text(
                                    text = marginText,
                                    style = MonoMetricMedium,
                                    color = badgeContentColor,
                                )
                            }
                            AnimatedContent(
                                targetState = CurrencyFormatter.formatSignedProfit(
                                    profit = product.profit,
                                    currencySymbol = settings.currencySymbol,
                                    decimalPlaces = settings.decimalPlaces,
                                ),
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "profit_badge_anim",
                            ) { profitText ->
                                Text(
                                    text = profitText,
                                    style = MonoCodeBadge,
                                    color = badgeContentColor.copy(alpha = 0.9f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = semanticColors.cardBorder.copy(alpha = 0.45f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PriceLabelColumn(
                            label = "COST",
                            value = CurrencyFormatter.formatPrice(
                                amount = product.costPrice,
                                currencySymbol = settings.currencySymbol,
                                decimalPlaces = settings.decimalPlaces,
                            ),
                            isPrimary = false,
                        )
                        PriceLabelColumn(
                            label = "SELL PRICE",
                            value = CurrencyFormatter.formatPrice(
                                amount = product.sellingPrice,
                                currencySymbol = settings.currencySymbol,
                                decimalPlaces = settings.decimalPlaces,
                            ),
                            isPrimary = true,
                        )
                    }

                    // Inline Stock +/- Stepper
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = semanticColors.surfaceElevated,
                        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.7f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 2.dp),
                        ) {
                            IconButton(
                                onClick = { onDecrement(product) },
                                enabled = product.quantity > 0,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.32f,
                                    ),
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .testTag("decrement_qty_${product.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease stock for ${product.name}",
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            AnimatedContent(
                                targetState = product.quantity,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                                            (slideOutVertically { it / 2 } + fadeOut())
                                    } else {
                                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                            (slideOutVertically { -it / 2 } + fadeOut())
                                    }
                                },
                                label = "qty_counter_anim",
                            ) { qty ->
                                Text(
                                    text = qty.toString(),
                                    style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (qty <= settings.lowStockThreshold) {
                                        semanticColors.lowStockContent
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(min = 28.dp),
                                )
                            }

                            IconButton(
                                onClick = { onIncrement(product) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .testTag("increment_qty_${product.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase stock for ${product.name}",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceLabelColumn(
    label: String,
    value: String,
    isPrimary: Boolean,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MonoMetricMedium,
            color = if (isPrimary) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
