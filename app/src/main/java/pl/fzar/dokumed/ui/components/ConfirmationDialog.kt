package pl.fzar.dokumed.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.fzar.dokumed.R

@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String = stringResource(R.string.delete_button_text),
    dismissButtonText: String = stringResource(R.string.cancel_button_text)
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismissRequest() // Dismiss after confirm
                    }
                ) {
                    Text(confirmButtonText, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(dismissButtonText)
                }
            }
        )
    }
}
