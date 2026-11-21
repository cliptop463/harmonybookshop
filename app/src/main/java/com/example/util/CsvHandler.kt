package com.example.util

import com.example.data.Product
import com.example.data.ProductCategory

data class CsvSkippedRow(
    val rowNumber: Int,
    val reason: String,
)

data class CsvImportResult(
    val validProducts: List<Product>,
    val skippedRows: List<CsvSkippedRow>,
) {
    val importedCount: Int get() = validProducts.size
    val skippedCount: Int get() = skippedRows.size
}

/**
 * RFC-4180 compliant offline CSV serializer and validating parser for product catalogs.
 */
object CsvHandler {

    private const val HEADER = "name,category,quantity,costPrice,sellingPrice"

    fun exportToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        products.forEach { product ->
            sb.append(escapeField(product.name)).append(',')
            sb.append(escapeField(product.category.displayName)).append(',')
            sb.append(product.quantity).append(',')
            sb.append(CurrencyFormatter.formatEditableNumber(product.costPrice)).append(',')
            sb.append(CurrencyFormatter.formatEditableNumber(product.sellingPrice)).append('\n')
        }
        return sb.toString()
    }

    fun parseCsv(csvContent: String): CsvImportResult {
        val records = parseRecords(csvContent)
        if (records.isEmpty()) {
            return CsvImportResult(emptyList(), emptyList())
        }

        val validProducts = mutableListOf<Product>()
        val skippedRows = mutableListOf<CsvSkippedRow>()

        val startIndex = if (isHeaderRow(records.first())) 1 else 0
        val now = System.currentTimeMillis()

        for (index in startIndex until records.size) {
            val rowNumber = index + 1
            val columns = records[index]
            if (columns.all { it.isBlank() }) continue

            if (columns.size < 5) {
                skippedRows += CsvSkippedRow(
                    rowNumber = rowNumber,
                    reason = "Expected 5 columns (name, category, quantity, costPrice, sellingPrice)",
                )
                continue
            }

            val name = columns[0].trim()
            if (name.isEmpty()) {
                skippedRows += CsvSkippedRow(rowNumber, "Missing product name")
                continue
            }

            val category = ProductCategory.fromRaw(columns[1])

            val quantity = columns[2].trim().toIntOrNull()
            if (quantity == null || quantity < 0) {
                skippedRows += CsvSkippedRow(rowNumber, "Invalid quantity '${columns[2].trim()}'")
                continue
            }

            val costPrice = columns[3].trim().toDoubleOrNull()
            if (costPrice == null || !costPrice.isFinite() || costPrice < 0.0) {
                skippedRows += CsvSkippedRow(rowNumber, "Invalid cost price '${columns[3].trim()}'")
                continue
            }

            val sellingPrice = columns[4].trim().toDoubleOrNull()
            if (sellingPrice == null || !sellingPrice.isFinite() || sellingPrice < 0.0) {
                skippedRows += CsvSkippedRow(rowNumber, "Invalid selling price '${columns[4].trim()}'")
                continue
            }

            validProducts += Product(
                name = name,
                category = category,
                quantity = quantity,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                createdAt = now + index,
                updatedAt = now + index,
            )
        }

        return CsvImportResult(validProducts, skippedRows)
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        if (row.size < 5) return false
        val first = row[0].trim().lowercase()
        val third = row[2].trim().lowercase()
        val fourth = row[3].trim().lowercase()
        return first == "name" && (third == "quantity" || third == "qty") && fourth.contains("cost")
    }

    private fun escapeField(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun parseRecords(input: String): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        val records = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()

        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        records.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        records.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            records.add(currentRow.toList())
        }

        return records
    }
}
