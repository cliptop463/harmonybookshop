package com.example.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.Product
import com.example.repository.ProductRepository
import com.example.repository.SettingsRepository
import com.example.util.CurrencyFormatter
import com.example.util.CsvHandler
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

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(SortOrder.NAME)
    private val formState = MutableStateFlow<ProductFormState?>(null)
    private val isSettingsOpen = MutableStateFlow(false)
    private val importReport = MutableStateFlow<ImportReportState?>(null)

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
            summary = summary,
            searchQuery = query,
            sortOrder = sort,
            settings = settings,
        )
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        baseDataFlow,
        formState,
        isSettingsOpen,
        importReport,
    ) { base, form, settingsOpen, report ->
        base.copy(
            formState = form,
            isSettingsOpen = settingsOpen,
            importReport = report,
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
        formState.value = ProductFormState()
    }

    fun onOpenEditSheet(product: Product) {
        formState.value = ProductFormState(
            editingProductId = product.id,
            name = product.name,
            skuOrCode = product.skuOrCode.orEmpty(),
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

    fun onFormCodeChange(value: String) {
        formState.update { current ->
            current?.copy(skuOrCode = value)
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
            skuOrCode = current.skuOrCode.trim().ifEmpty { null },
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
