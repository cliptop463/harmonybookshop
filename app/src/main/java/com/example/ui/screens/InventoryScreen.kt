package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppSettings
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.ui.components.CsvImportReportDialog
import com.example.ui.components.ProductCardItem
import com.example.ui.components.ProductFormBottomSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SummaryHeaderCard
import com.example.ui.theme.LocalMarginDeskColors
import com.example.viewmodel.AppTab
import com.example.viewmodel.InventoryListState
import com.example.viewmodel.InventoryUiState
import com.example.viewmodel.SignatureStroke
import com.example.viewmodel.SortOrder

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    snackbarHostState: SnackbarHostState,
    onTabSelected: (AppTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit,
    onIncrementQuantity: (Product) -> Unit,
    onDecrementQuantity: (Product) -> Unit,
    onAddProductToOrder: (Product, Boolean) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onOpenAddSheet: () -> Unit,
    onOpenEditSheet: (Product) -> Unit,
    onDismissSheet: () -> Unit,
    onFormNameChange: (String) -> Unit,
    onFormCategoryChange: (ProductCategory) -> Unit,
    onFormQuantityChange: (String) -> Unit,
    onFormCostPriceChange: (String) -> Unit,
    onFormSellingPriceChange: (String) -> Unit,
    onSubmitForm: () -> Unit,
    onOrderCustomerNameChange: (String) -> Unit,
    onOrderCustomerAddressChange: (String) -> Unit,
    onOrderDateChange: (String) -> Unit,
    onOpenCatalogPicker: () -> Unit,
    onDismissCatalogPicker: () -> Unit,
    onOpenCustomItemDialog: () -> Unit,
    onDismissCustomItemDialog: () -> Unit,
    onAddCustomOrderItem: (String, ProductCategory, Int, Double) -> Unit,
    onIncrementOrderItemQty: (Long) -> Unit,
    onDecrementOrderItemQty: (Long) -> Unit,
    onRemoveOrderItem: (Long) -> Unit,
    onUpdateOrderItemPrice: (Long, Double) -> Unit,
    onAddDealerStroke: (SignatureStroke) -> Unit,
    onClearDealerSignature: () -> Unit,
    onAddCustomerStroke: (SignatureStroke) -> Unit,
    onClearCustomerSignature: () -> Unit,
    onToggleDeductStock: (Boolean) -> Unit,
    onClearOrder: () -> Unit,
    onSaveOrderPdfClick: () -> Unit,
    onShareOrderPdfClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onExportCsvClick: () -> Unit,
    onImportCsvClick: () -> Unit,
    onDismissImportReport: () -> Unit,
) {
    val semanticColors = LocalMarginDeskColors.current
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = uiState.activeTab != AppTab.CATALOG) {
        onTabSelected(AppTab.CATALOG)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.CATALOG,
                    onClick = { onTabSelected(AppTab.CATALOG) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == AppTab.CATALOG) {
                                Icons.Rounded.Inventory2
                            } else {
                                Icons.Outlined.Inventory2
                            },
                            contentDescription = "Catalog",
                        )
                    },
                    label = { Text(AppTab.CATALOG.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.testTag("nav_tab_catalog"),
                )

                val orderCount = uiState.orderState.totalUnits
                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.ORDER,
                    onClick = { onTabSelected(AppTab.ORDER) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (orderCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Text(orderCount.toString())
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (uiState.activeTab == AppTab.ORDER) {
                                    Icons.Rounded.ReceiptLong
                                } else {
                                    Icons.Outlined.ReceiptLong
                                },
                                contentDescription = "Order & Receipt",
                            )
                        }
                    },
                    label = { Text(AppTab.ORDER.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.testTag("nav_tab_order"),
                )
            }
        },
        floatingActionButton = {
            if (uiState.activeTab == AppTab.CATALOG) {
                FloatingActionButton(
                    onClick = onOpenAddSheet,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_product_fab"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_product_cd),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (uiState.activeTab) {
            AppTab.CATALOG -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Prominent Search Bar at the very top + Settings action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.search_placeholder),
                                        maxLines = 1,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = onClearSearch,
                                            modifier = Modifier.testTag("clear_search_button"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.clear_search_cd),
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = semanticColors.cardBorder.copy(alpha = 0.7f),
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_bar_input"),
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.7f)),
                            ) {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(52.dp)
                                        .testTag("settings_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = stringResource(R.string.settings_cd),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SummaryHeaderCard(
                            summary = uiState.summary,
                            settings = uiState.settings,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sort chips row (Last modified, Last added, Name, Low stock, Highest margin)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SortOrder.entries.forEach { order ->
                                val selected = uiState.sortOrder == order
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSortOrderSelected(order) },
                                    label = { Text(order.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = semanticColors.cardBorder.copy(alpha = 0.7f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier.testTag("sort_chip_${order.name.lowercase()}"),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (val listState = uiState.listState) {
                            is InventoryListState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is InventoryListState.Ready -> {
                                when {
                                    listState.totalCatalogCount == 0 -> {
                                        EmptyCatalogState(
                                            onAddFirstItem = onOpenAddSheet,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }

                                    listState.items.isEmpty() -> {
                                        EmptySearchState(
                                            query = uiState.searchQuery,
                                            onClearSearch = onClearSearch,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }

                                    else -> {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .testTag("product_list"),
                                            contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            items(
                                                items = listState.items,
                                                key = { it.id },
                                            ) { product ->
                                                ProductCardItem(
                                                    product = product,
                                                    settings = uiState.settings,
                                                    onCardClick = onOpenEditSheet,
                                                    onIncrement = onIncrementQuantity,
                                                    onDecrement = onDecrementQuantity,
                                                    onAddToOrder = { prod -> onAddProductToOrder(prod, false) },
                                                    onDelete = onDeleteProduct,
                                                    modifier = Modifier.animateItem(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AppTab.ORDER -> {
                OrderReceiptScreen(
                    orderState = uiState.orderState,
                    catalogProducts = uiState.allCatalogProducts,
                    settings = uiState.settings,
                    onCustomerNameChange = onOrderCustomerNameChange,
                    onCustomerAddressChange = onOrderCustomerAddressChange,
                    onOrderDateChange = onOrderDateChange,
                    onOpenCatalogPicker = onOpenCatalogPicker,
                    onDismissCatalogPicker = onDismissCatalogPicker,
                    onOpenCustomItemDialog = onOpenCustomItemDialog,
                    onDismissCustomItemDialog = onDismissCustomItemDialog,
                    onAddProductToOrder = { prod -> onAddProductToOrder(prod, true) },
                    onAddCustomItem = onAddCustomOrderItem,
                    onIncrementItemQty = onIncrementOrderItemQty,
                    onDecrementItemQty = onDecrementOrderItemQty,
                    onRemoveItem = onRemoveOrderItem,
                    onUpdateItemPrice = onUpdateOrderItemPrice,
                    onAddDealerStroke = onAddDealerStroke,
                    onClearDealerSignature = onClearDealerSignature,
                    onAddCustomerStroke = onAddCustomerStroke,
                    onClearCustomerSignature = onClearCustomerSignature,
                    onToggleDeductStock = onToggleDeductStock,
                    onClearOrder = onClearOrder,
                    onSavePdfClick = onSaveOrderPdfClick,
                    onSharePdfClick = onShareOrderPdfClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }

    uiState.formState?.let { form ->
        ProductFormBottomSheet(
            formState = form,
            settings = uiState.settings,
            onNameChange = onFormNameChange,
            onCategoryChange = onFormCategoryChange,
            onQuantityChange = onFormQuantityChange,
            onCostPriceChange = onFormCostPriceChange,
            onSellingPriceChange = onFormSellingPriceChange,
            onSave = onSubmitForm,
            onDismiss = onDismissSheet,
        )
    }

    if (uiState.isSettingsOpen) {
        SettingsDialog(
            currentSettings = uiState.settings,
            onDismiss = onDismissSettings,
            onSave = onSaveSettings,
            onExportCsvClick = onExportCsvClick,
            onImportCsvClick = onImportCsvClick,
        )
    }

    uiState.importReport?.let { report ->
        CsvImportReportDialog(
            report = report,
            onDismiss = onDismissImportReport,
        )
    }
}

@Composable
private fun EmptyCatalogState(
    onAddFirstItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_catalog_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_catalog_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddFirstItem,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("add_first_item_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_first_item))
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.no_search_results_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "No items matched \"$query\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onClearSearch,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp),
        ) {
            Text(stringResource(R.string.clear_search_button))
        }
    }
}
