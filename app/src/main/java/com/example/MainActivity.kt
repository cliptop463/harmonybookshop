package com.example

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
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::onClearSearch,
                    onSortOrderSelected = viewModel::onSortOrderSelected,
                    onIncrementQuantity = viewModel::onIncrementQuantity,
                    onDecrementQuantity = viewModel::onDecrementQuantity,
                    onDeleteProduct = viewModel::onDeleteProduct,
                    onOpenAddSheet = viewModel::onOpenAddSheet,
                    onOpenEditSheet = viewModel::onOpenEditSheet,
                    onDismissSheet = viewModel::onDismissSheet,
                    onFormNameChange = viewModel::onFormNameChange,
                    onFormCodeChange = viewModel::onFormCodeChange,
                    onFormQuantityChange = viewModel::onFormQuantityChange,
                    onFormCostPriceChange = viewModel::onFormCostPriceChange,
                    onFormSellingPriceChange = viewModel::onFormSellingPriceChange,
                    onSubmitForm = viewModel::onSubmitForm,
                    onOpenSettings = viewModel::onOpenSettings,
                    onDismissSettings = viewModel::onDismissSettings,
                    onSaveSettings = viewModel::onSaveSettings,
                    onExportCsvClick = {
                        val fileName = "margindesk_catalog_${LocalDate.now()}.csv"
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
