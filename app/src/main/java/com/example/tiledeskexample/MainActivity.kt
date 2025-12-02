package com.example.tiledeskexample

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tiledeskexample.ui.theme.TiledeskExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TiledeskExampleTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var showFullScreenWidget by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var uploadMessage: ValueCallback<Array<Uri>>? by remember { mutableStateOf(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (uploadMessage == null) return@rememberLauncherForActivityResult
        var results: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.dataString?.let {
                results = arrayOf(Uri.parse(it))
            }
        }
        uploadMessage?.onReceiveValue(results)
        uploadMessage = null
    }

    val tiledeskWebView = remember {
        WebView(context).apply {
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d(
                        "TiledeskWebView",
                        "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"
                    )
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    uploadMessage = filePathCallback
                    val intent = fileChooserParams.createIntent()
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "Cannot open file chooser", Toast.LENGTH_LONG).show()
                        return false
                    }
                    return true
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = true
        }
    }

    // Use LaunchedEffect to load the URL once as a side effect.
    LaunchedEffect(tiledeskWebView) {
        tiledeskWebView.loadUrl("https://chat.vortexio.tech/widget/assets/twp/blank.html?tiledesk_projectid=68f73807d8ab1e0fba8a1dca&tiledesk_fullscreenMode=true&tiledesk_hideHeaderCloseButton=true&tiledesk_open=true")
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Layer 1: The main content (the button)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { showFullScreenWidget = true }) {
                    Text("Open Tiledesk Widget")
                }
            }

            // Layer 2: The overlay that acts like a dialog
            if (showFullScreenWidget) {
                val configuration = LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                val topPadding = screenHeight * 0.05f
                val bottomPadding = screenHeight * 0.05f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface) // Hides content behind it
                        .imePadding() // The crucial keyboard handler
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding, bottom = bottomPadding)
                    ) {
                        AndroidView(
                            factory = { 
                                (tiledeskWebView.parent as? ViewGroup)?.removeView(tiledeskWebView)
                                tiledeskWebView 
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { showFullScreenWidget = false },
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close widget")
                        }
                    }
                }
            }
        }
    }
}
