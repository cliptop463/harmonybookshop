package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.InventoryScreen
import com.example.ui.theme.MarginDeskTheme
import com.example.viewmodel.InventorySnackbarEvent
import com.example.viewmodel.InventoryViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SalesApplication).container

        setContent {
            val viewModel: InventoryViewModel = viewModel(
                factory = appContainer.inventoryViewModelFactory,
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            val exportCsvLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/csv"),
            ) { uri ->
                if (uri != null) {
                    viewModel.onExportCsv(context.contentResolver, uri)
                }
            }

            val importCsvLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    viewModel.onImportCsv(context.contentResolver, uri)
                }
            }

            val exportPdfLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/pdf"),
            ) { uri ->
                if (uri != null) {
                    viewModel.onSaveOrderPdf(context.contentResolver, uri)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.snackbarEvents.collect { event ->
                    snackbarHostState.currentSnackbarData?.dismiss()
                    when (event) {
                        is InventorySnackbarEvent.ProductDeleted -> {
                            val result = snackbarHostState.showSnackbar(
                                message = "Deleted \"${event.product.name}\"",
                                actionLabel = "Undo",
                                withDismissAction = true,
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.onUndoDelete(event.product)
                            }
                        }

                        is InventorySnackbarEvent.Message -> {
                            snackbarHostState.showSnackbar(
                                message = event.text,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                }
            }

            MarginDeskTheme(dynamicColor = uiState.settings.dynamicColor) {
                InventoryScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onTabSelected = viewModel::onTabSelected,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::onClearSearch,
                    onSortOrderSelected = viewModel::onSortOrderSelected,
                    onIncrementQuantity = viewModel::onIncrementQuantity,
                    onDecrementQuantity = viewModel::onDecrementQuantity,
                    onAddProductToOrder = viewModel::onAddProductToOrder,
                    onDeleteProduct = viewModel::onDeleteProduct,
                    onOpenAddSheet = viewModel::onOpenAddSheet,
                    onOpenEditSheet = viewModel::onOpenEditSheet,
                    onDismissSheet = viewModel::onDismissSheet,
                    onFormNameChange = viewModel::onFormNameChange,
                    onFormCategoryChange = viewModel::onFormCategoryChange,
                    onFormQuantityChange = viewModel::onFormQuantityChange,
                    onFormCostPriceChange = viewModel::onFormCostPriceChange,
                    onFormSellingPriceChange = viewModel::onFormSellingPriceChange,
                    onSubmitForm = viewModel::onSubmitForm,
                    onOrderCustomerNameChange = viewModel::onOrderCustomerNameChange,
                    onOrderCustomerAddressChange = viewModel::onOrderCustomerAddressChange,
                    onOrderDateChange = viewModel::onOrderDateChange,
                    onOpenCatalogPicker = viewModel::onOpenCatalogPicker,
                    onDismissCatalogPicker = viewModel::onDismissCatalogPicker,
                    onOpenCustomItemDialog = viewModel::onOpenCustomItemDialog,
                    onDismissCustomItemDialog = viewModel::onDismissCustomItemDialog,
                    onAddCustomOrderItem = viewModel::onAddCustomItemToOrder,
                    onIncrementOrderItemQty = viewModel::onIncrementOrderItemQty,
                    onDecrementOrderItemQty = viewModel::onDecrementOrderItemQty,
                    onRemoveOrderItem = viewModel::onRemoveOrderItem,
                    onUpdateOrderItemPrice = viewModel::onUpdateOrderItemPrice,
                    onAddDealerStroke = viewModel::onAddDealerStroke,
                    onClearDealerSignature = viewModel::onClearDealerSignature,
                    onAddCustomerStroke = viewModel::onAddCustomerStroke,
                    onClearCustomerSignature = viewModel::onClearCustomerSignature,
                    onToggleDeductStock = viewModel::onToggleDeductStock,
                    onClearOrder = viewModel::onClearOrder,
                    onSaveOrderPdfClick = {
                        if (viewModel.validateOrderForReceipt()) {
                            val safeName = uiState.orderState.customerName.trim()
                                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                                .take(24)
                                .ifEmpty { "customer" }
                            val fileName = "Harmony_Receipt_${safeName}_${LocalDate.now()}.pdf"
                            exportPdfLauncher.launch(fileName)
                        }
                    },
                    onShareOrderPdfClick = {
                        viewModel.onGenerateShareablePdf(context) { pdfUri ->
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(pdfUri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, pdfUri)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Harmony Bookshop Receipt - ${uiState.orderState.customerName.trim()}",
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            if (viewIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(Intent.createChooser(viewIntent, "Open or Share Receipt PDF"))
                            } else {
                                context.startActivity(Intent.createChooser(sendIntent, "Share Receipt PDF"))
                            }
                        }
                    },
                    onOpenSettings = viewModel::onOpenSettings,
                    onDismissSettings = viewModel::onDismissSettings,
                    onSaveSettings = viewModel::onSaveSettings,
                    onExportCsvClick = {
                        val fileName = "harmony_bookshop_catalog_${LocalDate.now()}.csv"
                        exportCsvLauncher.launch(fileName)
                    },
                    onImportCsvClick = {
                        importCsvLauncher.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*")
                        )
                    },
                    onDismissImportReport = viewModel::onDismissImportReport,
                )
            }
        }
    }
}
