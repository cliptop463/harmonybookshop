package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.ProductCategory
import com.example.ui.theme.LocalMarginDeskColors
import com.example.ui.theme.MonoMetricLarge
import com.example.util.CurrencyFormatter
import com.example.viewmodel.ProductFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormBottomSheet(
    formState: ProductFormState,
    settings: AppSettings,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCostPriceChange: (String) -> Unit,
    onSellingPriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semanticColors = LocalMarginDeskColors.current
    var categoryExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("product_form_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = if (formState.isEditing) "Edit Product" else "Add Product",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Prices use your active currency (${settings.currencySymbol}).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Profit & Margin Preview Card
            val isProfit = formState.previewProfit > 0.0
            val isLoss = formState.previewProfit < 0.0
            val previewBg by animateColorAsState(
                targetValue = when {
                    !formState.hasLivePreview -> semanticColors.surfaceElevated
                    isProfit -> semanticColors.profitContainer
                    isLoss -> semanticColors.lossContainer
                    else -> semanticColors.surfaceElevated
                },
                label = "form_preview_bg",
            )
            val previewAccent by animateColorAsState(
                targetValue = when {
                    !formState.hasLivePreview -> MaterialTheme.colorScheme.onSurfaceVariant
                    isProfit -> semanticColors.profitContent
                    isLoss -> semanticColors.lossContent
                    else -> MaterialTheme.colorScheme.onSurface
                },
                label = "form_preview_fg",
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_margin_preview"),
                shape = RoundedCornerShape(16.dp),
                color = previewBg,
                border = BorderStroke(1.dp, previewAccent.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "LIVE PROFIT / UNIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (formState.hasLivePreview) {
                                CurrencyFormatter.formatSignedProfit(
                                    profit = formState.previewProfit,
                                    currencySymbol = settings.currencySymbol,
                                    decimalPlaces = settings.decimalPlaces,
                                )
                            } else {
                                "--"
                            },
                            style = MonoMetricLarge,
                            color = previewAccent,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PROFIT MARGIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (formState.hasLivePreview) {
                                CurrencyFormatter.formatMarginPercent(formState.previewMargin)
                            } else {
                                "--%"
                            },
                            style = MonoMetricLarge,
                            color = previewAccent,
                        )
                    }
                }
            }

            // Red warning banner if sellingPrice < costPrice (saving remains allowed)
            AnimatedVisibility(visible = formState.isSellingBelowCost) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("loss_warning_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = semanticColors.lossContainer,
                    border = BorderStroke(1.dp, semanticColors.lossContent.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Loss warning",
                            tint = semanticColors.lossContent,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Selling price is lower than cost price. Saving is allowed, but this item will sell at a loss.",
                            style = MaterialTheme.typography.bodySmall,
                            color = semanticColors.lossContent,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChange,
                label = { Text("Product Name *") },
                placeholder = { Text("e.g., 200-Page Exercise Book") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { error -> { Text(error) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_name_input"),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier
                        .weight(1.35f)
                        .testTag("form_category_dropdown"),
                ) {
                    OutlinedTextField(
                        value = formState.category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            Icon(
                                imageVector = formState.category.icon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        ProductCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = {
                                    onCategoryChange(category)
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("category_option_${category.name.lowercase()}"),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = formState.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Quantity *") },
                    isError = formState.quantityError != null,
                    supportingText = formState.quantityError?.let { error -> { Text(error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("form_quantity_input"),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = formState.costPriceText,
                    onValueChange = onCostPriceChange,
                    label = { Text("Cost Price *") },
                    prefix = { Text("${settings.currencySymbol} ") },
                    isError = formState.costPriceError != null,
                    supportingText = formState.costPriceError?.let { error -> { Text(error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_cost_input"),
                )

                OutlinedTextField(
                    value = formState.sellingPriceText,
                    onValueChange = onSellingPriceChange,
                    label = { Text("Selling Price *") },
                    prefix = { Text("${settings.currencySymbol} ") },
                    isError = formState.sellingPriceError != null,
                    supportingText = formState.sellingPriceError?.let { error -> { Text(error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_selling_input"),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("form_cancel_button"),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("form_save_button"),
                ) {
                    Text(if (formState.isEditing) "Save Changes" else "Save Product")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
