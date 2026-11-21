package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Print
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.ProductCategory

fun ProductCategory.icon(): ImageVector = when (this) {
    ProductCategory.PRINTER -> Icons.Rounded.Print
    ProductCategory.TEXTBOOK -> Icons.AutoMirrored.Rounded.MenuBook
    ProductCategory.EXERCISE_BOOK -> Icons.Rounded.Book
    ProductCategory.TYPING_SHEET -> Icons.Rounded.Description
    ProductCategory.PEN -> Icons.Rounded.Edit
    ProductCategory.PENCIL -> Icons.Rounded.Create
    ProductCategory.FOLDER -> Icons.Rounded.Folder
    ProductCategory.INK -> Icons.Rounded.Opacity
    ProductCategory.STATIONARY -> Icons.Rounded.Inventory2
    ProductCategory.OTHER -> Icons.Rounded.Category
}
