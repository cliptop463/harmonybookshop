package com.example.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.repository.ProductRepository
import com.example.repository.SettingsRepository
import com.example.util.CurrencyFormatter
import com.example.util.CsvHandler
import com.example.util.PdfReceiptGenerator
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val activeTab = MutableStateFlow(AppTab.CATALOG)
    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(SortOrder.LAST_MODIFIED)
    private val formState = MutableStateFlow<ProductFormState?>(null)
    private val orderState = MutableStateFlow(OrderFormState())
    private val isSettingsOpen = MutableStateFlow(false)
    private val importReport = MutableStateFlow<ImportReportState?>(null)
    private val nextOrderLineId = AtomicLong(1L)

    private val _snackbarEvents = MutableSharedFlow<InventorySnackbarEvent>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<InventorySnackbarEvent> = _snackbarEvents.asSharedFlow()

    // Instant Room LIKE query backed directly by the search query StateFlow (no debounce).
    private val filteredProductsFlow = searchQuery.flatMapLatest { query ->
        productRepository.searchProducts(query)
    }

    private val baseDataFlow = combine(
        filteredProductsFlow,
        productRepository.observeAllProducts(),
        searchQuery,
        sortOrder,
        settingsRepository.settings,
    ) { filtered, allProducts, query, sort, settings ->
        val sortedItems = when (sort) {
            SortOrder.LAST_MODIFIED -> filtered.sortedWith(
                compareByDescending<Product> { it.updatedAt }.thenByDescending { it.id }
            )
            SortOrder.LAST_ADDED -> filtered.sortedWith(
                compareByDescending<Product> { it.createdAt }.thenByDescending { it.id }
            )
            SortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.LOW_STOCK -> filtered.sortedWith(
                compareBy<Product> { it.quantity }.thenBy { it.name.lowercase() }
            )
            SortOrder.HIGHEST_MARGIN -> filtered.sortedWith(
                compareByDescending<Product> { it.profitMargin }.thenBy { it.name.lowercase() }
            )
        }

        val summary = InventorySummary(
            totalActiveItems = allProducts.size,
            totalUnits = allProducts.sumOf { it.quantity },
            totalInventoryValue = allProducts.sumOf { it.quantity * it.costPrice },
            averageProfitMargin = if (allProducts.isEmpty()) {
                0.0
            } else {
                allProducts.sumOf { it.profitMargin } / allProducts.size
            },
        )

        InventoryUiState(
            listState = InventoryListState.Ready(
                items = sortedItems,
                totalCatalogCount = allProducts.size,
            ),
            allCatalogProducts = allProducts,
            summary = summary,
            searchQuery = query,
            sortOrder = sort,
            settings = settings,
        )
    }

    private val modalAndTabFlow = combine(
        activeTab,
        formState,
        orderState,
        isSettingsOpen,
        importReport,
    ) { tab, form, order, settingsOpen, report ->
        ModalAndTabHolder(tab, form, order, settingsOpen, report)
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        baseDataFlow,
        modalAndTabFlow,
    ) { base, holder ->
        base.copy(
            activeTab = holder.tab,
            formState = holder.form,
            orderState = holder.order,
            isSettingsOpen = holder.settingsOpen,
            importReport = holder.report,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InventoryUiState(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.seedInitialProductsIfNeeded()
        }
    }

    fun onTabSelected(tab: AppTab) {
        activeTab.value = tab
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onClearSearch() {
        searchQuery.value = ""
    }

    fun onSortOrderSelected(newSortOrder: SortOrder) {
        sortOrder.value = newSortOrder
    }

    fun onIncrementQuantity(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.incrementQuantity(product.id)
        }
    }

    fun onDecrementQuantity(product: Product) {
        if (product.quantity <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.decrementQuantity(product.id)
        }
    }

    fun onDeleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.deleteProduct(product)
            _snackbarEvents.emit(InventorySnackbarEvent.ProductDeleted(product))
        }
    }

    fun onUndoDelete(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.restoreProduct(product)
        }
    }

    fun onOpenAddSheet() {
        formState.value = ProductFormState(category = ProductCategory.DEFAULT)
    }

    fun onOpenEditSheet(product: Product) {
        formState.value = ProductFormState(
            editingProductId = product.id,
            name = product.name,
            category = product.category,
            quantityText = product.quantity.toString(),
            costPriceText = CurrencyFormatter.formatEditableNumber(product.costPrice),
            sellingPriceText = CurrencyFormatter.formatEditableNumber(product.sellingPrice),
        )
    }

    fun onDismissSheet() {
        formState.value = null
    }

    fun onFormNameChange(value: String) {
        formState.update { current ->
            current?.copy(
                name = value,
                nameError = if (value.isBlank()) "Product name is required" else null,
            )
        }
    }

    fun onFormCategoryChange(category: ProductCategory) {
        formState.update { current ->
            current?.copy(category = category)
        }
    }

    fun onFormQuantityChange(value: String) {
        formState.update { current ->
            current?.copy(
                quantityText = value,
                quantityError = validateQuantity(value),
            )
        }
    }

    fun onFormCostPriceChange(value: String) {
        formState.update { current ->
            current?.copy(
                costPriceText = value,
                costPriceError = validatePrice(value, "Cost price"),
            )
        }
    }

    fun onFormSellingPriceChange(value: String) {
        formState.update { current ->
            current?.copy(
                sellingPriceText = value,
                sellingPriceError = validatePrice(value, "Selling price"),
            )
        }
    }

    fun onSubmitForm() {
        val current = formState.value ?: return

        val trimmedName = current.name.trim()
        val nameError = if (trimmedName.isEmpty()) "Product name is required" else null
        val quantityError = validateQuantity(current.quantityText)
        val costError = validatePrice(current.costPriceText, "Cost price")
        val sellingError = validatePrice(current.sellingPriceText, "Selling price")

        if (nameError != null || quantityError != null || costError != null || sellingError != null) {
            formState.value = current.copy(
                nameError = nameError,
                quantityError = quantityError,
                costPriceError = costError,
                sellingPriceError = sellingError,
            )
            return
        }

        val product = Product(
            id = current.editingProductId ?: 0L,
            name = trimmedName,
            category = current.category,
            quantity = current.quantityText.trim().toInt(),
            costPrice = current.costPriceText.trim().toDouble(),
            sellingPrice = current.sellingPriceText.trim().toDouble(),
        )

        formState.value = null
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.saveProduct(product)
            val action = if (current.isEditing) "Updated" else "Added"
            _snackbarEvents.emit(InventorySnackbarEvent.Message("$action \"${product.name}\""))
        }
    }

    // --- Customer Order & PDF Receipt Operations ---

    fun onOrderCustomerNameChange(value: String) {
        orderState.update { current ->
            current.copy(
                customerName = value,
                customerNameError = if (value.isBlank()) "Customer name is required for receipt" else null,
            )
        }
    }

    fun onOrderCustomerAddressChange(value: String) {
        orderState.update { it.copy(customerAddress = value) }
    }

    fun onOrderDateChange(value: String) {
        orderState.update { it.copy(orderDate = value) }
    }

    fun onOpenCatalogPicker() {
        orderState.update { it.copy(isCatalogPickerOpen = true) }
    }

    fun onDismissCatalogPicker() {
        orderState.update { it.copy(isCatalogPickerOpen = false) }
    }

    fun onOpenCustomItemDialog() {
        orderState.update { it.copy(isCustomItemDialogOpen = true) }
    }

    fun onDismissCustomItemDialog() {
        orderState.update { it.copy(isCustomItemDialogOpen = false) }
    }

    fun onAddProductToOrder(product: Product, closePicker: Boolean = false) {
        orderState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.productId == product.id }
            val updatedItems = if (existingIndex >= 0) {
                current.items.mapIndexed { idx, item ->
                    if (idx == existingIndex) {
                        item.copy(quantity = item.quantity + 1)
                    } else {
                        item
                    }
                }
            } else {
                current.items + OrderLineItem(
                    lineId = nextOrderLineId.getAndIncrement(),
                    productId = product.id,
                    name = product.name,
                    category = product.category,
                    quantity = 1,
                    unitPriceXaf = product.sellingPrice,
                    maxAvailableStock = product.quantity,
                )
            }
            current.copy(
                items = updatedItems,
                itemsError = null,
                isCatalogPickerOpen = if (closePicker) false else current.isCatalogPickerOpen,
            )
        }
        viewModelScope.launch {
            _snackbarEvents.emit(InventorySnackbarEvent.Message("Added \"${product.name}\" to order"))
        }
    }

    fun onAddCustomItemToOrder(
        name: String,
        category: ProductCategory,
        quantity: Int,
        unitPriceXaf: Double,
    ) {
        orderState.update { current ->
            current.copy(
                items = current.items + OrderLineItem(
                    lineId = nextOrderLineId.getAndIncrement(),
                    productId = null,
                    name = name.trim(),
                    category = category,
                    quantity = quantity.coerceAtLeast(1),
                    unitPriceXaf = unitPriceXaf.coerceAtLeast(0.0),
                ),
                itemsError = null,
                isCustomItemDialogOpen = false,
            )
        }
    }

    fun onIncrementOrderItemQty(lineId: Long) {
        orderState.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.lineId == lineId) item.copy(quantity = item.quantity + 1) else item
                }
            )
        }
    }

    fun onDecrementOrderItemQty(lineId: Long) {
        orderState.update { current ->
            val updated = current.items.mapNotNull { item ->
                if (item.lineId == lineId) {
                    if (item.quantity <= 1) null else item.copy(quantity = item.quantity - 1)
                } else {
                    item
                }
            }
            current.copy(items = updated)
        }
    }

    fun onRemoveOrderItem(lineId: Long) {
        orderState.update { current ->
            current.copy(items = current.items.filterNot { it.lineId == lineId })
        }
    }

    fun onUpdateOrderItemPrice(lineId: Long, newPriceXaf: Double) {
        orderState.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.lineId == lineId) {
                        item.copy(unitPriceXaf = newPriceXaf.coerceAtLeast(0.0))
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun onAddDealerStroke(stroke: SignatureStroke) {
        orderState.update { current ->
            current.copy(dealerStrokes = current.dealerStrokes + stroke)
        }
    }

    fun onClearDealerSignature() {
        orderState.update { it.copy(dealerStrokes = emptyList()) }
    }

    fun onAddCustomerStroke(stroke: SignatureStroke) {
        orderState.update { current ->
            current.copy(customerStrokes = current.customerStrokes + stroke)
        }
    }

    fun onClearCustomerSignature() {
        orderState.update { it.copy(customerStrokes = emptyList()) }
    }

    fun onToggleDeductStock(deduct: Boolean) {
        orderState.update { it.copy(deductStockOnGenerate = deduct) }
    }

    fun onClearOrder() {
        orderState.value = OrderFormState()
    }

    fun validateOrderForReceipt(): Boolean {
        val current = orderState.value
        val nameError = if (current.customerName.isBlank()) {
            "Customer name is required for receipt"
        } else {
            null
        }
        val itemsError = if (current.items.isEmpty()) {
            "Add at least one item to generate a receipt"
        } else {
            null
        }
        if (nameError != null || itemsError != null) {
            orderState.update {
                it.copy(
                    customerNameError = nameError,
                    itemsError = itemsError,
                )
            }
            return false
        }
        return true
    }

    fun onSaveOrderPdf(contentResolver: ContentResolver, uri: Uri) {
        val snapshot = orderState.value
        if (!validateOrderForReceipt()) return
        val decimals = uiState.value.settings.decimalPlaces

        orderState.update { it.copy(isGeneratingPdf = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    PdfReceiptGenerator.generateReceipt(snapshot, decimals, out)
                } ?: error("Unable to open destination PDF file")

                if (snapshot.deductStockOnGenerate) {
                    deductOrderStock(snapshot)
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    orderState.update { it.copy(isGeneratingPdf = false) }
                }
                _snackbarEvents.emit(
                    InventorySnackbarEvent.Message("PDF receipt saved for ${snapshot.customerName.trim()}")
                )
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    orderState.update { it.copy(isGeneratingPdf = false) }
                }
                _snackbarEvents.emit(
                    InventorySnackbarEvent.Message("PDF save failed: ${error.localizedMessage ?: "Unknown error"}")
                )
            }
        }
    }

    fun onGenerateShareablePdf(
        context: Context,
        onUriReady: (Uri) -> Unit,
    ) {
        val snapshot = orderState.value
        if (!validateOrderForReceipt()) return
        val decimals = uiState.value.settings.decimalPlaces

        orderState.update { it.copy(isGeneratingPdf = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val safeCustomer = snapshot.customerName.trim()
                    .replace(Regex("[^A-Za-z0-9_-]"), "_")
                    .take(24)
                    .ifEmpty { "customer" }
                val pdfFile = File(receiptsDir, "Harmony_Receipt_${safeCustomer}_$stamp.pdf")
                pdfFile.outputStream().use { out ->
                    PdfReceiptGenerator.generateReceipt(snapshot, decimals, out)
                }
                if (snapshot.deductStockOnGenerate) {
                    deductOrderStock(snapshot)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile,
                )
            }.onSuccess { contentUri ->
                withContext(Dispatchers.Main) {
                    orderState.update { it.copy(isGeneratingPdf = false) }
                    onUriReady(contentUri)
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    orderState.update { it.copy(isGeneratingPdf = false) }
                }
                _snackbarEvents.emit(
                    InventorySnackbarEvent.Message("PDF failed: ${error.localizedMessage ?: "Unknown error"}")
                )
            }
        }
    }

    private suspend fun deductOrderStock(snapshot: OrderFormState) {
        snapshot.items.forEach { line ->
            val productId = line.productId
            if (productId != null) {
                productRepository.deductStock(productId, line.quantity)
            }
        }
    }

    // --- Settings & CSV Backup ---

    fun onOpenSettings() {
        isSettingsOpen.value = true
    }

    fun onDismissSettings() {
        isSettingsOpen.value = false
    }

    fun onSaveSettings(settings: AppSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSettings(settings)
            isSettingsOpen.value = false
            _snackbarEvents.emit(InventorySnackbarEvent.Message("Settings saved"))
        }
    }

    fun onExportCsv(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val products = productRepository.getAllProductsForExport()
                val csv = CsvHandler.exportToCsv(products)
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(csv)
                } ?: error("Unable to open destination file")
                products.size
            }.onSuccess { count ->
                _snackbarEvents.emit(
                    InventorySnackbarEvent.Message("Exported $count products to CSV")
                )
            }.onFailure { error ->
                _snackbarEvents.emit(
                    InventorySnackbarEvent.Message("Export failed: ${error.localizedMessage ?: "Unknown error"}")
                )
            }
        }
    }

    fun onImportCsv(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val content = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: error("Unable to read selected CSV file")
                CsvHandler.parseCsv(content)
            }.onSuccess { result ->
                if (result.validProducts.isNotEmpty()) {
                    productRepository.importProducts(result.validProducts)
                }
                withContext(Dispatchers.Main) {
                    isSettingsOpen.value = false
                    importReport.value = ImportReportState(
                        importedCount = result.importedCount,
                        skippedRows = result.skippedRows,
                    )
                }
            }.onFailure { error ->
                _snackbarEvents.emit(
                    InventorySnackbarEvent.Message("Import failed: ${error.localizedMessage ?: "Unknown error"}")
                )
            }
        }
    }

    fun onDismissImportReport() {
        importReport.value = null
    }

    private fun validateQuantity(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Quantity is required"
        val parsed = trimmed.toIntOrNull()
            ?: return "Must be a whole number (no decimals)"
        if (parsed < 0) return "Cannot be negative"
        return null
    }

    private fun validatePrice(raw: String, label: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "$label is required"
        val parsed = trimmed.toDoubleOrNull()
            ?: return "Enter a valid number"
        if (!parsed.isFinite()) return "Enter a valid number"
        if (parsed < 0.0) return "Cannot be negative"
        return null
    }
}

private data class ModalAndTabHolder(
    val tab: AppTab,
    val form: ProductFormState?,
    val order: OrderFormState,
    val settingsOpen: Boolean,
    val report: ImportReportState?,
)
