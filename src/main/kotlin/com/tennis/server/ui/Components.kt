package com.tennis.server.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Debemos hacer esta función usando genéricos para no perder el objeto que se selecciona.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorOpcionesGenerico(
    label: String,
    opciones: List<T>,
    itemTexto: (T) -> String, // Función para saber qué texto mostrar de cada objeto
    seleccionado: T?,
    onOptionSelected: (T) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            // Si hay algo seleccionado, usamos itemTexto para mostrar su nombre
            value = seleccionado?.let { itemTexto(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(itemTexto(opcion)) }, // Mostramos el nombre
                    onClick = {
                        onOptionSelected(opcion) // DEVOLVEMOS EL OBJETÓ COMPLETO
                        expandido = false
                    }
                )
            }
        }
    }
}