package presentation

import Screen
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun App(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.updateUIState(
            { viewModel._isConnected.value = it },
            { viewModel._accountType.value = it },
            { viewModel._quota.value = it },
            { viewModel._dataAvailable.value = it }
        )
    }

    val isConnected by viewModel.isConnected.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val accountType by viewModel.accountType.collectAsState()
    val quota by viewModel.quota.collectAsState()
    val availableData by viewModel.dataAvailable.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val toggleButtonIndicator = remember { mutableStateOf(isConnected) }

    val mainText by viewModel.currentModeUi.collectAsState()

    MaterialTheme(
        colors = MaterialTheme.colors.copy(primary = Color(0xFFF6821F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(48.dp))  // Approximate width of the settings icon

                    Text(
                        text = mainText,
                        style = MaterialTheme.typography.h4,
                        color = Color(0xFFFFA500),  // Orange color as shown in the image
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    SettingsButton(
                        viewModel,
                        onNavigate
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Connection Status
                ConnectionStatus(isConnected, isConnecting)

                Spacer(modifier = Modifier.height(32.dp))

                // Toggle Button
                PrimaryButton(
                    isOn = toggleButtonIndicator.value ,
                    onToggle = {
                        viewModel.toggleConnection()
                        toggleButtonIndicator.value = !toggleButtonIndicator.value
                    },
                    enabled = !isConnecting
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Account Info
                AccountInfo(accountType, quota, availableData)

                Spacer(modifier = Modifier.weight(1f))

                // Current Mode
                Text(
                    text = "Current mode: $currentMode",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ConnectionStatus(isConnected: Boolean, isConnecting: Boolean) {
    val statusColor = when {
        isConnecting -> Color.Yellow
        isConnected -> Color.Green
        else -> Color.Red
    }
    val statusText = when {
        isConnecting -> "Connecting..."
        isConnected -> "Connected"
        else -> "Disconnected"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(4.dp, statusColor, CircleShape)
                .padding(4.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.h6,
            color = statusColor
        )
    }
}

@Composable
fun PrimaryButton(
    isOn: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean
) {
    val thumbColor = if (isOn) Color.White else Color.LightGray
    val trackColor = if (isOn) Color(0xFFF6821F) else Color.LightGray.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .height(52.dp)
            .width(100.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(trackColor)
            .clickable(enabled = enabled) { onToggle() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(if (isOn) Alignment.CenterEnd else Alignment.CenterStart)
                .offset(x = if (isOn) (-8).dp else 8.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
fun AccountInfo(accountType: String, quota: String, availableData: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoRow("Account type", accountType)
        InfoRow("Quota", quota)
        InfoRow("Data Available", availableData)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsButton(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit
) {
    var isContextMenuVisible by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isContextMenuVisible = true }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.Gray
            )
        }

        DropdownMenu(
            expanded = isContextMenuVisible,
            onDismissRequest = { isContextMenuVisible = false }
        ) {
            DropdownMenuItem(onClick = {
                viewModel.setWarpSettings(WarpServer.OneDotOneDotOneDotOne)
                isContextMenuVisible = false
            }) {
                Text("1.1.1.1")
            }
            DropdownMenuItem(onClick = {
                viewModel.setWarpSettings(WarpServer.OneDotOneDotOneDotOne)
                isContextMenuVisible = false
            }) {
                Text("1.1.1.1 with Warp")
            }
            DropdownMenuItem(onClick = {
                run { onNavigate(Screen.Settings) }
                isContextMenuVisible = false
            }) {
                Text("Preferences")
            }
        }
    }
}

@Composable
@Preview()
fun AppPreview() {
    App(
        MainViewModel(),
        {}
    )
}
