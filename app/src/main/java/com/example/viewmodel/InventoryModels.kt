package com.example.viewmodel

import com.example.data.AppSettings
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.util.CsvSkippedRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class SortOrder(val label: String) {
    LAST_MODIFIED("Last modified"),
    LAST_ADDED("Last added"),
    NAME("Name"),
    LOW_STOCK("Low stock"),
    HIGHEST_MARGIN("Highest margin"),
}

enum class AppTab(val label: String) {
    CATALOG("Catalog"),
    ORDER("Order & Receipt"),
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
    val category: ProductCategory = ProductCategory.DEFAULT,
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

/**
 * Normalized stroke point (0f..1f) so touch-drawn signatures scale cleanly between UI pads and A4 PDFs.
 */
data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

data class SignatureStroke(
    val points: List<NormalizedPoint>,
)

data class OrderLineItem(
    val lineId: Long,
    val productId: Long? = null,
    val name: String,
    val category: ProductCategory = ProductCategory.DEFAULT,
    val quantity: Int = 1,
    val unitPriceXaf: Double,
    val maxAvailableStock: Int? = null,
) {
    val lineTotalXaf: Double
        get() = quantity * unitPriceXaf
}

data class OrderFormState(
    val customerName: String = "",
    val customerAddress: String = "",
    val orderDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
    val items: List<OrderLineItem> = emptyList(),
    val dealerStrokes: List<SignatureStroke> = emptyList(),
    val customerStrokes: List<SignatureStroke> = emptyList(),
    val deductStockOnGenerate: Boolean = false,
    val customerNameError: String? = null,
    val itemsError: String? = null,
    val isCatalogPickerOpen: Boolean = false,
    val isCustomItemDialogOpen: Boolean = false,
    val isGeneratingPdf: Boolean = false,
) {
    val totalXaf: Double
        get() = items.sumOf { it.lineTotalXaf }

    val totalUnits: Int
        get() = items.sumOf { it.quantity }
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
    val activeTab: AppTab = AppTab.CATALOG,
    val listState: InventoryListState = InventoryListState.Loading,
    val allCatalogProducts: List<Product> = emptyList(),
    val summary: InventorySummary = InventorySummary(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.LAST_MODIFIED,
    val settings: AppSettings = AppSettings(),
    val formState: ProductFormState? = null,
    val orderState: OrderFormState = OrderFormState(),
    val isSettingsOpen: Boolean = false,
    val importReport: ImportReportState? = null,
)

sealed interface InventorySnackbarEvent {
    data class ProductDeleted(val product: Product) : InventorySnackbarEvent
    data class Message(val text: String) : InventorySnackbarEvent
}
