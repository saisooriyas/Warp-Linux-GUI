package presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class MainViewModel {

    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting = _isConnecting.asStateFlow()

    val _accountType = MutableStateFlow("Free")
    val accountType = _accountType.asStateFlow()

    val _quota = MutableStateFlow("0 GB")
    val quota = _quota.asStateFlow()

    val _dataAvailable = MutableStateFlow("0 GB")
    val dataAvailable = _dataAvailable.asStateFlow()

    private val _currentMode = MutableStateFlow("1.1.1.1 with Warp")
    val currentMode = _currentMode.asStateFlow()

    private val _currentModeUi = MutableStateFlow("WARP")
    val currentModeUi = _currentModeUi.asStateFlow()

    var errorMessage by mutableStateOf<String?>(null)

    init {
        switchTo1111WithWarp()
        checkStatus()
    }

    fun setKey(key: String) {
        viewModelScope.launch {
            executeCommand("warp-cli registration license $key")
        }
    }

    fun toggleConnection() {
        viewModelScope.launch {
            _isConnecting.value = true
            var isConnected = false
            var retryCount = 0
            if (_isConnected.value) {
                executeCommand("warp-cli disconnect")
                _isConnected.value = false
            } else {
                while(!isConnected && retryCount < 6) {
                    executeCommand("warp-cli connect")
                    delay(2000) // Wait for 2 seconds
                    isConnected = checkConnectionStatus()
                    _isConnected.value = isConnected
                    retryCount++
                }

            }
            val (quota, data, type) = updateAccountInfo()
            _quota.value = formatSize(quota)
            _dataAvailable.value = formatSize(data)
            _accountType.value = type
            _isConnecting.value = false
            updateUIState(
                { _isConnected.value = it },
                { _accountType.value = it },
                { _quota.value = it },
                { _dataAvailable.value = it }
            )
        }
    }

    private suspend fun checkConnectionStatus(): Boolean = withContext(Dispatchers.IO) {
        val status = executeCommand("warp-cli status")
        "Connected" in status
    }

    private suspend fun updateAccountInfo(): Triple<Long, Long, String> =
        withContext(Dispatchers.IO) {
            val accountInfo = executeCommand("warp-cli account")
            var quota = 0L
            var premiumData = 0L
            var accountType = ""

            accountInfo.lines().forEach { line ->
                when {
                    "Quota" in line -> quota = line.substringAfter(":").trim().toLong()
                    "Premium Data" in line -> premiumData = line.substringAfter(":").trim().toLong()
                    "Account type" in line -> accountType = line.substringAfter(":").trim()
                }
            }

            return@withContext Triple(quota, premiumData, accountType)

        }

    private suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(command)
            process.waitFor(5, TimeUnit.SECONDS) // Set a timeout
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // Log the error or handle it appropriately
            ""
        }
    }

    private fun formatSize(sizeInBytes: Long): String {
        val units = listOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
        var size = sizeInBytes.toDouble()
        var unitIndex = 0
        while (size >= 1000 && unitIndex < units.size - 1) {
            size /= 1000
            unitIndex++
        }
        return "%.2f %s".format(size, units[unitIndex])
    }

    suspend fun updateUIState(
        updateConnected: (Boolean) -> Unit,
        updateAccountType: (String) -> Unit,
        updateQuota: (String) -> Unit,
        updateDataAvailable: (String) -> Unit
    ) {
        coroutineScope {
            updateConnected(checkConnectionStatus())

            val (quotaValue, premiumDataValue, accountTypeValue) = updateAccountInfo()
            updateQuota(formatSize(quotaValue))
            updateDataAvailable(formatSize(premiumDataValue))
            updateAccountType(accountTypeValue)
        }
    }

    private fun checkStatus() {
        viewModelScope.launch {
            _isConnecting.value = true
            try {
                val status = withContext(Dispatchers.IO) {
                    executeCommand("warp-cli status")
                }
                _isConnected.value = "Connected" in status
            } catch (e: Exception) {
                // Log the error or handle it appropriately
                _isConnected.value = false
            }
            finally {
                _isConnecting.value = false
            }
        }
    }

    fun setWarpSettings(server: WarpServer) {
        viewModelScope.launch {
            when (server) {
                WarpServer.OneDotOneDotOneDotOne -> switchTo1111()
                WarpServer.OneDotOneDotOneDotOneWithWarp -> switchTo1111WithWarp()
                WarpServer.Preferences -> { /* Implemented in App Composable */ }
            }
        }
    }

    private fun switchTo1111() {
        viewModelScope.launch {
            executeCommand("warp-cli mode proxy")
            _currentMode.value = "1.1.1.1"
            _currentModeUi.value = "Warp"
            checkStatus()
            updateUIState(
                { _isConnected.value = it },
                { _accountType.value = it },
                { _quota.value = it },
                { _dataAvailable.value = it }
            )
        }
    }

    private fun switchTo1111WithWarp() {
        viewModelScope.launch {
            executeCommand("warp-cli mode warp")
            _currentMode.value = "1.1.1.1 with Warp"
            _currentModeUi.value = "WARP"
            checkStatus()
            updateUIState(
                { _isConnected.value = it },
                { _accountType.value = it },
                { _quota.value = it },
                { _dataAvailable.value = it }
            )
        }
    }
}