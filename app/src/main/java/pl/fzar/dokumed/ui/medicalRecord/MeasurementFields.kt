package pl.fzar.dokumed.ui.medicalRecord

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.fzar.dokumed.data.model.Measurement

@Composable
fun MeasurementFields(
    measurement: Measurement,
    onMeasurementChange: (Measurement) -> Unit
) {
    OutlinedTextField(
        value = measurement.value?.toString() ?: "",
        onValueChange = { newValue ->
            onMeasurementChange(measurement.copy(value = newValue.toDoubleOrNull()))
        },
        label = { Text("Value") }, // TODO: translate
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = measurement.unit ?: "",
        onValueChange = { newUnit ->
            onMeasurementChange(measurement.copy(unit = newUnit.ifBlank { null }))
        },
        label = { Text("Jednostka") }, // TODO: Consider specific labels based on type
        modifier = Modifier.fillMaxWidth()
    )
}