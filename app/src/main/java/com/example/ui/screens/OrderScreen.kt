package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.ui.components.SignaturePadCard
import com.example.ui.components.icon
import com.example.ui.theme.LocalMarginDeskColors
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.util.CurrencyFormatter
import com.example.viewmodel.OrderFormState
import com.example.viewmodel.OrderLineItem
import com.example.viewmodel.SignatureStroke
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderReceiptScreen(
    orderState: OrderFormState,
    catalogProducts: List<Product>,
    settings: AppSettings,
    onCustomerNameChange: (String) -> Unit,
    onCustomerAddressChange: (String) -> Unit,
    onOrderDateChange: (String) -> Unit,
    onOpenCatalogPicker: () -> Unit,
    onDismissCatalogPicker: () -> Unit,
    onOpenCustomItemDialog: () -> Unit,
    onDismissCustomItemDialog: () -> Unit,
    onAddProductToOrder: (Product) -> Unit,
    onAddCustomItem: (String, ProductCategory, Int, Double) -> Unit,
    onIncrementItemQty: (Long) -> Unit,
    onDecrementItemQty: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onUpdateItemPrice: (Long, Double) -> Unit,
    onAddDealerStroke: (SignatureStroke) -> Unit,
    onClearDealerSignature: () -> Unit,
    onAddCustomerStroke: (SignatureStroke) -> Unit,
    onClearCustomerSignature: () -> Unit,
    onToggleDeductStock: (Boolean) -> Unit,
    onClearOrder: () -> Unit,
    onSavePdfClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalMarginDeskColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    var editingPriceItem by remember { mutableStateOf<OrderLineItem?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp)
                .testTag("order_receipt_list"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Official Harmony Bookshop Receipt Heading Preview
            item(key = "receipt_heading") {
                ReceiptHeaderBanner(
                    onClearOrder = onClearOrder,
                    hasOrderContent = orderState.items.isNotEmpty() ||
                        orderState.customerName.isNotBlank() ||
                        orderState.customerAddress.isNotBlank() ||
                        orderState.dealerStrokes.isNotEmpty() ||
                        orderState.customerStrokes.isNotEmpty(),
                )
            }

            // Customer Info & Date Card
            item(key = "customer_info") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Customer & Receipt Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        OutlinedTextField(
                            value = orderState.customerName,
                            onValueChange = onCustomerNameChange,
                            label = { Text("Customer Name *") },
                            placeholder = { Text("e.g., St. Augustine College / John Doe") },
                            isError = orderState.customerNameError != null,
                            supportingText = orderState.customerNameError?.let { err -> { Text(err) } },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_customer_name_input"),
                        )

                        OutlinedTextField(
                            value = orderState.customerAddress,
                            onValueChange = onCustomerAddressChange,
                            label = { Text("Address") },
                            placeholder = { Text("e.g., Tobin, Kumbo") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_customer_address_input"),
                        )

                        OutlinedTextField(
                            value = orderState.orderDate,
                            onValueChange = onOrderDateChange,
                            label = { Text("Date") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.testTag("order_date_picker_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "Select date",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_date_input"),
                        )
                    }
                }
            }

            // Order Items Header & Action Buttons
            item(key = "items_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Order Items (${orderState.items.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${orderState.totalUnits} total units",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onOpenCatalogPicker,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("order_add_from_catalog_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add from Catalog")
                        }

                        OutlinedButton(
                            onClick = onOpenCustomItemDialog,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("order_add_custom_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Custom Item")
                        }
                    }

                    AnimatedVisibility(visible = orderState.itemsError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = semanticColors.lossContainer,
                            border = BorderStroke(1.dp, semanticColors.lossContent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = orderState.itemsError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = semanticColors.lossContent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }

            if (orderState.items.isEmpty()) {
                item(key = "empty_order_items") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No items in this order yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap \"Add from Catalog\" or \"Order\" on any product card to build the receipt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = orderState.items,
                    key = { it.lineId },
                ) { line ->
                    OrderLineItemCard(
                        item = line,
                        decimalPlaces = settings.decimalPlaces,
                        onIncrement = { onIncrementItemQty(line.lineId) },
                        onDecrement = { onDecrementItemQty(line.lineId) },
                        onRemove = { onRemoveItem(line.lineId) },
                        onEditPrice = { editingPriceItem = line },
                    )
                }
            }

            // Grand Total in XAF & Stock Deduction Toggle
            item(key = "order_total_card") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = semanticColors.profitContainer,
                    border = BorderStroke(1.dp, semanticColors.profitAccent.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_total_xaf_card"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL AMOUNT (XAF)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = semanticColors.profitContent,
                                )
                                Text(
                                    text = "${orderState.items.size} line(s) • ${orderState.totalUnits} unit(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semanticColors.profitContent.copy(alpha = 0.8f),
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatPrice(
                                    amount = orderState.totalXaf,
                                    currencySymbol = "XAF",
                                    decimalPlaces = settings.decimalPlaces,
                                ),
                                style = MonoMetricLarge,
                                color = semanticColors.profitContent,
                            )
                        }

                        HorizontalDivider(color = semanticColors.profitAccent.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Deduct sold quantities from stock",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Updates inventory counts when exporting PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = orderState.deductStockOnGenerate,
                                onCheckedChange = onToggleDeductStock,
                                modifier = Modifier.testTag("order_deduct_stock_switch"),
                            )
                        }
                    }
                }
            }

            // Customer & Dealer Signatures
            item(key = "customer_signature") {
                SignaturePadCard(
                    title = "Customer Signature",
                    subtitle = orderState.customerName.trim().ifEmpty { "Sign below or leave blank to sign on paper" },
                    strokes = orderState.customerStrokes,
                    onStrokeCompleted = onAddCustomerStroke,
                    onClear = onClearCustomerSignature,
                    testTagPrefix = "customer_signature",
                )
            }

            item(key = "dealer_signature") {
                SignaturePadCard(
                    title = "Dealer Signature",
                    subtitle = "Harmony Bookshop Authorized Representative",
                    strokes = orderState.dealerStrokes,
                    onStrokeCompleted = onAddDealerStroke,
                    onClear = onClearDealerSignature,
                    testTagPrefix = "dealer_signature",
                )
            }

            // PDF Generation Actions
            item(key = "pdf_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onSharePdfClick,
                        enabled = !orderState.isGeneratingPdf,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("order_share_pdf_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview / Share")
                    }

                    Button(
                        onClick = onSavePdfClick,
                        enabled = !orderState.isGeneratingPdf,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(52.dp)
                            .testTag("order_save_pdf_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF Receipt")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onOrderDateChange(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (orderState.isCatalogPickerOpen) {
        CatalogProductPickerSheet(
            products = catalogProducts,
            decimalPlaces = settings.decimalPlaces,
            onSelectProduct = onAddProductToOrder,
            onDismiss = onDismissCatalogPicker,
        )
    }

    if (orderState.isCustomItemDialogOpen) {
        CustomOrderItemDialog(
            onAdd = onAddCustomItem,
            onDismiss = onDismissCustomItemDialog,
        )
    }

    editingPriceItem?.let { item ->
        EditOrderPriceDialog(
            item = item,
            onSave = { newPrice ->
                onUpdateItemPrice(item.lineId, newPrice)
                editingPriceItem = null
            },
            onDismiss = { editingPriceItem = null },
        )
    }
}

@Composable
private fun ReceiptHeaderBanner(
    onClearOrder: () -> Unit,
    hasOrderContent: Boolean,
) {
    val semanticColors = LocalMarginDeskColors.current
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.75f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receipt_heading_banner"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height * 0.15f),
                            radius = size.width * 0.6f,
                        ),
                        center = Offset(size.width * 0.9f, size.height * 0.15f),
                        radius = size.width * 0.6f,
                    )
                }
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "HARMONY BOOKSHOP",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (hasOrderContent) {
                        IconButton(
                            onClick = onClearOrder,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_order_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Reset order",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shed 373 Kumbo Main Market",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Dealers in all Textbooks, General Stationary and General Commerce",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tel: 677889888",
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun OrderLineItemCard(
    item: OrderLineItem,
    decimalPlaces: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEditPrice: () -> Unit,
) {
    val semanticColors = LocalMarginDeskColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_line_item_${item.lineId}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.category.displayName} • Unit: ${
                            CurrencyFormatter.formatPrice(item.unitPriceXaf, "XAF", decimalPlaces)
                        } (tap to edit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onEditPrice),
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove ${item.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semanticColors.surfaceElevated,
                    border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.7f)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp)
                                .clip(CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease quantity",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text = item.quantity.toString(),
                            style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 24.dp),
                        )
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp)
                                .clip(CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase quantity",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SUBTOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.formatPrice(item.lineTotalXaf, "XAF", decimalPlaces),
                        style = MonoMetricMedium,
                        color = semanticColors.profitContent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogProductPickerSheet(
    products: List<Product>,
    decimalPlaces: Int,
    onSelectProduct: (Product) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickerQuery by remember { mutableStateOf("") }
    val filtered = remember(products, pickerQuery) {
        val q = pickerQuery.trim()
        if (q.isEmpty()) {
            products
        } else {
            products.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.category.displayName.contains(q, ignoreCase = true)
            }
        }
    }
    val semanticColors = LocalMarginDeskColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = "Select Catalog Item",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = pickerQuery,
                onValueChange = { pickerQuery = it },
                placeholder = { Text("Search name or category…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filtered, key = { it.id }) { product ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = semanticColors.surfaceElevated,
                        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectProduct(product) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${product.category.displayName} • Stock: ${product.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = CurrencyFormatter.formatPrice(
                                    product.sellingPrice,
                                    "XAF",
                                    decimalPlaces,
                                ),
                                style = MonoMetricMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomOrderItemDialog(
    onAdd: (String, ProductCategory, Int, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProductCategory.DEFAULT) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Custom Order Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        ProductCategory.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.displayName) },
                                onClick = {
                                    category = entry
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it; errorText = null },
                        label = { Text("Qty *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f),
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it; errorText = null },
                        label = { Text("Unit Price (XAF) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f),
                    )
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val qty = qtyText.trim().toIntOrNull()
                    val price = priceText.trim().toDoubleOrNull()
                    when {
                        trimmedName.isEmpty() -> errorText = "Item name is required"
                        qty == null || qty <= 0 -> errorText = "Quantity must be at least 1"
                        price == null || !price.isFinite() || price < 0.0 ->
                            errorText = "Enter a valid XAF unit price"
                        else -> onAdd(trimmedName, category, qty, price)
                    }
                },
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EditOrderPriceDialog(
    item: OrderLineItem,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var priceText by remember(item) {
        mutableStateOf(CurrencyFormatter.formatEditableNumber(item.unitPriceXaf))
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Unit Price") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; errorText = null },
                    label = { Text("Unit Price (XAF)") },
                    isError = errorText != null,
                    supportingText = errorText?.let { err -> { Text(err) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = priceText.trim().toDoubleOrNull()
                    if (parsed == null || !parsed.isFinite() || parsed < 0.0) {
                        errorText = "Enter a valid non-negative XAF price"
                    } else {
                        onSave(parsed)
                    }
                },
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
