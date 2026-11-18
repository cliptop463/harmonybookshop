package com.example.viewmodel

import com.example.data.AppSettings
import com.example.data.Product
import com.example.util.CsvSkippedRow

enum class SortOrder(val label: String) {
    NAME("Name"),
    LOW_STOCK("Low stock"),
    HIGHEST_MARGIN("Highest margin"),
}

data class InventorySummary(
    val totalActiveItems: Int = 0,
    val totalUnits: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val averageProfitMargin: Double = 0.0,
)

/**
 * State for the Add/Edit Product bottom sheet with live profit/margin preview and inline validation.
 */
data class ProductFormState(
    val editingProductId: Long? = null,
    val name: String = "",
    val skuOrCode: String = "",
    val quantityText: String = "1",
    val costPriceText: String = "",
    val sellingPriceText: String = "",
    val nameError: String? = null,
    val quantityError: String? = null,
    val costPriceError: String? = null,
    val sellingPriceError: String? = null,
) {
    val isEditing: Boolean
        get() = editingProductId != null

    val parsedCost: Double?
        get() = costPriceText.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

    val parsedSelling: Double?
        get() = sellingPriceText.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

    val hasLivePreview: Boolean
        get() = parsedCost != null && parsedSelling != null

    val previewProfit: Double
        get() {
            val cost = parsedCost ?: return 0.0
            val selling = parsedSelling ?: return 0.0
            return selling - cost
        }

    val previewMargin: Double
        get() {
            val selling = parsedSelling ?: return 0.0
            if (selling == 0.0) return 0.0
            return (previewProfit / selling) * 100.0
        }

    /** Red warning shown in form when selling below cost (saving remains allowed). */
    val isSellingBelowCost: Boolean
        get() {
            val cost = parsedCost ?: return false
            val selling = parsedSelling ?: return false
            return selling < cost
        }
}

data class ImportReportState(
    val importedCount: Int,
    val skippedRows: List<CsvSkippedRow>,
) {
    val skippedCount: Int get() = skippedRows.size
}

sealed interface InventoryListState {
    data object Loading : InventoryListState
    data class Ready(
        val items: List<Product>,
        val totalCatalogCount: Int,
    ) : InventoryListState
}

data class InventoryUiState(
    val listState: InventoryListState = InventoryListState.Loading,
    val summary: InventorySummary = InventorySummary(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME,
    val settings: AppSettings = AppSettings(),
    val formState: ProductFormState? = null,
    val isSettingsOpen: Boolean = false,
    val importReport: ImportReportState? = null,
)

sealed interface InventorySnackbarEvent {
    data class ProductDeleted(val product: Product) : InventorySnackbarEvent
    data class Message(val text: String) : InventorySnackbarEvent
}
