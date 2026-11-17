package com.rkjewellery.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.rkjewellery.app.ui.theme.Ink
import com.rkjewellery.app.ui.theme.OutlineSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = OutlineSoft,
            cursorColor = Ink,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Numeric-only text field helper. */
fun String.toLongOrZero(): Long = this.filter { it.isDigit() }.toLongOrNull() ?: 0L
fun String.toDoubleOrZero(): Double = this.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
