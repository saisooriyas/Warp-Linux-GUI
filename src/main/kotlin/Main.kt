import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import presentation.App
import presentation.MainViewModel
import presentation.SettingsPage
import java.awt.Dimension

val width = 450.dp
val height = 650.dp
fun main() {

    try {
        Logger.log("Application starting...")
        Logger.log("App directory: ${AppConfig.appDir.absolutePath}")
        Logger.log("Log file: ${AppConfig.logFile.absolutePath}")
        Logger.log("Database file: ${AppConfig.dbFile.absolutePath}")


        val databaseInitialized = initDatabase()
        Logger.log("Database initialization result: $databaseInitialized")
        application {
            Logger.log("Setting up Compose application window...")
            val viewModel = MainViewModel()
            Window(
                onCloseRequest = {
                    Logger.log("Application exiting...")
                    exitApplication()
                },
                title = "Cloudflare Warp"
            ) {
                setInitialAndMinimumSize(width, height)
                val screenStack = remember { mutableStateListOf(Screen.App) }
                val currentScreen = screenStack.last()
                when (currentScreen) {
                    Screen.App -> App(
                        viewModel,
                        onNavigate = { screenStack.add(it) }
                    )

                    Screen.Settings -> SettingsPage(
                        viewModel,
                        onBack = { if (screenStack.size > 1) screenStack.removeLast() },
                        databaseInitialized
                    )
                }
            }
        }
    } catch (e: Exception) {
        Logger.log("Unhandled exception in main: ${e.message}")
        e.printStackTrace()
    }
}

@Composable
fun FrameWindowScope.setInitialAndMinimumSize(
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
) {
    val density = LocalDensity.current
    LaunchedEffect(density) {
        window.size = with(density) {
            Dimension(width.toPx().toInt(), height.toPx().toInt())
        }
        window.minimumSize = with(density) {
            Dimension(width.toPx().toInt(), height.toPx().toInt())
        }
    }
}