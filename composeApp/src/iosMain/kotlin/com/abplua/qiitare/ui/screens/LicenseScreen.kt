package com.abplua.qiitare.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes
import platform.WebKit.WKWebView

@OptIn(ExperimentalMaterial3Api::class, InternalResourceApi::class)
@Composable
fun LicenseScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var licenseHtml by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        licenseHtml = readResourceBytes("files/license.html").decodeToString()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(text = "ライセンス情報") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        val html = licenseHtml
        if (html == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            UIKitView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                factory = { WKWebView() },
                update = { webView ->
                    webView.loadHTMLString(
                        string = html,
                        baseURL = null,
                    )
                },
            )
        }
    }
}
