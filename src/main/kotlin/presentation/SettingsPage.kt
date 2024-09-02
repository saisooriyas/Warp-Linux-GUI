package presentation

import Key
import addKey
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import deleteKey
import getAllKeys
import kotlinx.coroutines.launch

enum class SettingsPage {
    Key,
    Account
}

@Composable
fun SettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    databaseInitialized: Boolean,
) {
    var selectedPage by remember { mutableStateOf(SettingsPage.Key) }

    Row(modifier = Modifier.fillMaxSize()) {
        // First column: List of pages/settings
        Column(
            modifier = Modifier
                .width(200.dp) // Fixed width to avoid unbounded size issues
                .fillMaxHeight()
        ) {
            val scrollState = rememberScrollState()

            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.Start)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            ListOfPages(selectedPage) { page ->
                selectedPage = page
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 200.dp) // Adjust padding to not overlap content
            ) {
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState)
                )
            }
        }

        // Vertical Divider
        Divider(
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )

        // Second column: Content
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val contentScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
            ) {
                when (selectedPage) {
                    SettingsPage.Key -> KeyContent(viewModel, databaseInitialized)
                    SettingsPage.Account -> AccountContent(viewModel)
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(contentScrollState)
            )
        }
    }
}

@Composable
fun ListOfPages(selectedPage: SettingsPage, onPageSelected: (SettingsPage) -> Unit) {
    SettingsPage.entries.forEach { page ->
        Box(
            modifier = Modifier
                .border(1.dp, Color.LightGray)
                .clickable { onPageSelected(page) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = page.name,
                modifier = Modifier
                    .fillMaxWidth(),
                fontWeight = if (page == selectedPage) FontWeight.Bold else FontWeight.Normal,
                color = if (page == selectedPage) MaterialTheme.colors.primary else Color.Unspecified
            )
        }
    }
}

@Composable
fun KeyContent(viewModel: MainViewModel, databaseInitialized: Boolean) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    var keys by remember { mutableStateOf(emptyList<Key>()) }
    var newKeyid by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (databaseInitialized) {
            keys = getAllKeys()
        } else {
            errorMessage = "Database initialization failed. Check logs for details."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        errorMessage.let {
            Text(it, color = MaterialTheme.colors.error)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            "Key Settings",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if(databaseInitialized) {

//         Display all keys
            keys.forEach { savedKey ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved Key: ${savedKey.keyID}",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setKey(savedKey.keyID) }
                    )

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (deleteKey(savedKey.id)) {
                                    keys = getAllKeys()
                                } else {
                                    errorMessage = "Failed to delete key. Check logs for details."
                                }
                            }
                        },
                        modifier = Modifier.padding(start = 5.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            OutlinedTextField(
                value = newKeyid,
                onValueChange = { newKeyid = it },
                label = { Text("Key") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    /*viewModel.saveKey(key)*/
                    coroutineScope.launch {
                        if(addKey(newKeyid)) {
                            newKeyid = ""
                            keys = getAllKeys()
                        } else {
                            errorMessage = "Failed to add key. Check logs for details."
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Key")
            }
        }
    }

    // Show Snackbar if needed
    LaunchedEffect(Unit) {
        viewModel.errorMessage?.let { message ->
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "OK"
                )
            }
            viewModel.errorMessage = null
        }
    }
}

@Composable
fun AccountContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Account Settings",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Add account-related settings here
        Text("Account settings content goes here")
    }
}

@Preview()
@Composable
fun SettingsPagePreview() {
    SettingsPage(
        MainViewModel(),
        onBack = {},
        databaseInitialized = true
    )
}