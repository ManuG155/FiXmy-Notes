package com.nexopp.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024 // 20 MB limit
private const val TIMEOUT_MS = 15000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegratedBrowserDialog(
    initialUrl: String = "https://www.google.com",
    onInsertText: (String) -> Unit,
    onInsertImage: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var addressInput by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Image context menu state
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showImageMenu by remember { mutableStateOf(false) }
    var isDownloadingImage by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra superior de navegación y controles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Adelante")
                    }

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Recargar")
                    }

                    // Campo de búsqueda / dirección URL
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        singleLine = true,
                        placeholder = { Text("Buscar o escribir URL…", fontSize = 13.sp) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val target = formatTargetUrl(addressInput)
                                addressInput = target
                                webViewInstance?.loadUrl(target)
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Ir")
                            }
                        }
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar navegador")
                    }
                }

                // Barra de progreso de carga
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }

                // WebView embebido
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            createConfiguredWebView(
                                context = ctx,
                                initialUrl = initialUrl,
                                onUrlChanged = { url ->
                                    currentUrl = url
                                    addressInput = url
                                },
                                onProgress = { p, loading ->
                                    progress = p
                                    isLoading = loading
                                    canGoBack = webViewInstance?.canGoBack() == true
                                    canGoForward = webViewInstance?.canGoForward() == true
                                },
                                onImageLongPressed = { imgUrl ->
                                    selectedImageUrl = imgUrl
                                    showImageMenu = true
                                }
                            ).also { webViewInstance = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Indicador de descarga de imagen
                    if (isDownloadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text("Descargando imagen…", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Menú contextual para imagen pulsada
    if (showImageMenu && selectedImageUrl != null) {
        val imageUrl = selectedImageUrl!!
        AlertDialog(
            onDismissRequest = { showImageMenu = false },
            title = { Text("Opciones de Imagen") },
            text = {
                Column {
                    Text(
                        text = imageUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(16.dp))

                    ListItem(
                        headlineContent = { Text("Insertar en notas") },
                        leadingContent = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageMenu = false
                            isDownloadingImage = true
                            coroutineScope.launch {
                                val result = downloadImageSafe(imageUrl, webViewInstance)
                                isDownloadingImage = false
                                result.fold(
                                    onSuccess = { bytes ->
                                        onInsertImage(bytes)
                                        Toast.makeText(context, "Imagen insertada en notas", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "No se pudo insertar la imagen: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Copiar imagen al portapapeles") },
                        leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageMenu = false
                            isDownloadingImage = true
                            coroutineScope.launch {
                                val result = downloadImageSafe(imageUrl, webViewInstance)
                                isDownloadingImage = false
                                result.fold(
                                    onSuccess = { bytes ->
                                        copyImageToClipboard(context, bytes)
                                        Toast.makeText(context, "Imagen copiada al portapapeles", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "No se pudo copiar la imagen: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Copiar enlace de la imagen") },
                        leadingContent = { Icon(Icons.Filled.Link, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageMenu = false
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("URL de imagen", imageUrl))
                            Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageMenu = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.apply {
                stopLoading()
                clearHistory()
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
            webViewInstance = null
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onProgress: (Int, Boolean) -> Unit,
    onImageLongPressed: (String) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let(onUrlChanged)
                onProgress(0, true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let(onUrlChanged)
                onProgress(100, false)
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgress(newProgress, newProgress < 100)
            }
        }

        setOnLongClickListener {
            val result = hitTestResult
            if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                val imgUrl = result.extra
                if (!imgUrl.isNullOrBlank()) {
                    onImageLongPressed(imgUrl)
                    return@setOnLongClickListener true
                }
            }
            false
        }

        loadUrl(formatTargetUrl(initialUrl))
    }
}

private fun formatTargetUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=" + runCatching { URLEncoder.encode(trimmed, "UTF-8") }.getOrDefault(trimmed)
    }
}

private suspend fun downloadImageSafe(url: String, webView: WebView?): Result<ByteArray> = withContext(Dispatchers.IO) {
    runCatching {
        if (url.startsWith("data:image/")) {
            val comma = url.indexOf(',')
            if (comma > 0) {
                val base64Data = url.substring(comma + 1)
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                validateImageBytes(bytes)
                return@runCatching bytes
            } else {
                throw IllegalArgumentException("Formato de URI de datos no válido.")
            }
        }

        if (url.startsWith("blob:") || url.startsWith("canvas:")) {
            throw IllegalArgumentException("Las imágenes 'blob:' o 'canvas:' son recursos dinámicos y no pueden descargarse directamente.")
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw IllegalArgumentException("Esquema no soportado: $url")
        }

        val cookie = CookieManager.getInstance().getCookie(url)
        val userAgent = webView?.settings?.userAgentString

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        if (!userAgent.isNullOrBlank()) {
            conn.setRequestProperty("User-Agent", userAgent)
        }
        if (!cookie.isNullOrBlank()) {
            conn.setRequestProperty("Cookie", cookie)
        }

        val code = conn.responseCode
        if (code == 401 || code == 403) {
            throw IllegalAccessException("Acceso denegado ($code). El sitio web protege esta imagen.")
        }
        if (code !in 200..299) {
            throw IllegalStateException("El servidor respondió con código de error: $code")
        }

        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalRead = 0
        conn.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                totalRead += read
                if (totalRead > MAX_IMAGE_BYTES) {
                    throw IllegalStateException("La imagen supera el límite de seguridad de 20 MB.")
                }
                out.write(buffer, 0, read)
            }
        }

        val bytes = out.toByteArray()
        validateImageBytes(bytes)
        bytes
    }
}

private fun validateImageBytes(bytes: ByteArray) {
    if (bytes.isEmpty()) throw IllegalArgumentException("La imagen está vacía.")
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    if (opts.outWidth <= 0 || opts.outHeight <= 0) {
        throw IllegalArgumentException("El archivo descargado no es una imagen válida.")
    }
}

private fun copyImageToClipboard(context: Context, bytes: ByteArray) {
    val cacheDir = File(context.cacheDir, "shared_cache").apply { mkdirs() }
    val cacheFile = File(cacheDir, "clip_browser_${System.currentTimeMillis()}.png").apply {
        writeBytes(bytes)
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        cacheFile
    )
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newUri(context.contentResolver, "FiXmy Notes", uri)
    cm?.setPrimaryClip(clip)
}
