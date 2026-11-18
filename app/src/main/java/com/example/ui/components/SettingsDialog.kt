package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.ui.theme.LocalMarginDeskColors
import com.example.viewmodel.ImportReportState

private val CurrencyPresets = listOf("$", "€", "£", "XAF", "₦", "¥")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onExportCsvClick: () -> Unit,
    onImportCsvClick: () -> Unit,
) {
    var currencyInput by remember(currentSettings) {
        mutableStateOf(currentSettings.currencySymbol)
    }
    var decimalPlaces by remember(currentSettings) {
        mutableIntStateOf(currentSettings.decimalPlaces)
    }
    var thresholdInput by remember(currentSettings) {
        mutableStateOf(currentSettings.lowStockThreshold.toString())
    }
    var dynamicColor by remember(currentSettings) {
        mutableStateOf(currentSettings.dynamicColor)
    }
    var thresholdError by remember { mutableStateOf<String?>(null) }

    val semanticColors = LocalMarginDeskColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = currencyInput,
                    onValueChange = { currencyInput = it.take(8) },
                    label = { Text("Currency Symbol") },
                    placeholder = { Text("e.g., $, XAF, €") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_currency_input"),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CurrencyPresets.forEach { preset ->
                        FilterChip(
                            selected = currencyInput.trim() == preset,
                            onClick = { currencyInput = preset },
                            label = { Text(preset) },
                        )
                    }
                }

                Column {
                    Text(
                        text = "Decimal Places",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 1, 2).forEach { count ->
                            FilterChip(
                                selected = decimalPlaces == count,
                                onClick = { decimalPlaces = count },
                                label = { Text("$count dec") },
                                modifier = Modifier.testTag("settings_decimals_$count"),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { raw ->
                        thresholdInput = raw
                        val parsed = raw.trim().toIntOrNull()
                        thresholdError = when {
                            raw.isBlank() -> "Threshold is required"
                            parsed == null || parsed < 0 -> "Enter a whole number ≥ 0"
                            else -> null
                        }
                    },
                    label = { Text("Low-Stock Threshold") },
                    isError = thresholdError != null,
                    supportingText = thresholdError?.let { err -> { Text(err) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_threshold_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Color",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Adapt accent colors to wallpaper"
                            } else {
                                "Requires Android 12 or higher"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = { dynamicColor = it },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        modifier = Modifier.testTag("settings_dynamic_color_switch"),
                    )
                }

                HorizontalDivider(color = semanticColors.cardBorder.copy(alpha = 0.6f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CSV Catalog Backup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onExportCsvClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_export_csv_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export")
                        }
                        OutlinedButton(
                            onClick = onImportCsvClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_import_csv_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdInput.trim().toIntOrNull()
                    if (threshold == null || threshold < 0) {
                        thresholdError = "Enter a whole number ≥ 0"
                        return@Button
                    }
                    onSave(
                        AppSettings(
                            currencySymbol = currencyInput.trim().ifEmpty { "$" },
                            decimalPlaces = decimalPlaces,
                            lowStockThreshold = threshold,
                            dynamicColor = dynamicColor,
                        ),
                    )
                },
                modifier = Modifier.testTag("settings_save_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("settings_cancel_button"),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun CsvImportReportDialog(
    report: ImportReportState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "CSV Import Complete",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Imported ${report.importedCount} product(s). Skipped ${report.skippedCount} invalid row(s).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (report.skippedRows.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Skipped Rows",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    report.skippedRows.take(10).forEach { skipped ->
                        Text(
                            text = "• Row ${skipped.rowNumber}: ${skipped.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (report.skippedRows.size > 10) {
                        Text(
                            text = "…and ${report.skippedRows.size - 10} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("import_report_ok_button"),
            ) {
                Text("Done")
            }
        },
    )
}
