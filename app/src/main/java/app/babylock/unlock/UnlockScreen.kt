package app.babylock.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun UnlockScreen(
    canUseSystemAuth: Boolean,
    hasPin: Boolean,
    challenge: Pair<Int, Int>,
    error: String?,
    onSystemAuth: () -> Unit,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(Modifier.padding(24.dp).widthIn(max = 380.dp).fillMaxWidth()) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Text("Desbloquear", style = MaterialTheme.typography.titleLarge)
                if (canUseSystemAuth) {
                    Button(onClick = onSystemAuth, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Fingerprint, null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Usar digital ou rosto")
                    }
                }
                val label = if (hasPin) {
                    "PIN do BabyLock"
                } else {
                    "Quanto é ${challenge.first} × ${challenge.second}?"
                }
                if (hasPin || !canUseSystemAuth) {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { v -> answer = v.filter(Char::isDigit).take(6) },
                        label = { Text(label) },
                        singleLine = true,
                        visualTransformation = if (hasPin) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { onSubmit(answer); answer = "" },
                        enabled = answer.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Confirmar") }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onCancel) { Text("Continuar bloqueado") }
            }
        }
    }
}
