package com.fuerte.lamariposa

import com.fuerte.lamariposa.ui.theme.LamariposaTheme
import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.widget.Toast

private lateinit var webView: WebView

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LamariposaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebViewComponent()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WebViewComponent() {
    val context = LocalContext.current
    webView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val webSettings: WebSettings = settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return if (url.endsWith(".pdf") || url.endsWith(".doc") || url.endsWith(".docx") ||
                    url.endsWith(".ods") || url.endsWith(".xlsx") || url.endsWith(".xls")) {
                    // Descargar o abrir archivo en una app externa según el tipo
                    val intent = Intent(Intent.ACTION_VIEW)
                    val mimeType = when {
                        url.endsWith(".pdf") -> "application/pdf"
                        url.endsWith(".doc") -> "application/msword"
                        url.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        url.endsWith(".ods") -> "application/vnd.oasis.opendocument.spreadsheet"
                        url.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        url.endsWith(".xls") -> "application/vnd.ms-excel"
                        else -> "*/*"
                    }

                    // Configurar el Intent para abrir el archivo con la app externa
                    intent.setDataAndType(Uri.parse(url), mimeType)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace() // Maneja el error si no hay apps compatibles
                    }
                    true // Indica que la navegación ha sido manejada

                }else if (url.startsWith("mailto:")) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse(url)
                    }

                    try {
                        // Intent.createChooser para permitir selección de app de correo si es necesario
                        val chooser = Intent.createChooser(intent, "Enviar correo con...")
                        if (view?.context?.packageManager?.let { chooser.resolveActivity(it) } != null) {
                            view?.context?.startActivity(chooser)
                        } else {
                            // Opcional: Mensaje si no hay apps de correo disponibles
                            Toast.makeText(view?.context, "No se encontró aplicación de correo", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Maneja el error si algo sale mal al abrir el correo
                        Toast.makeText(view?.context, "No se pudo abrir la aplicación de correo", Toast.LENGTH_SHORT).show()
                    }
                    true
                }else if (url.startsWith("tel:")) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                    view?.context?.startActivity(intent)
                    true
                } else if (url.startsWith("https://www.cajondesastre.com.es")) {
                    // Verifica si la URL es HTTP antes de cargarla
                    if (url.startsWith("https")) {
                        view?.loadUrl(url)
                        true
                    } else if (url.startsWith("http")) {
                        view?.loadUrl(url)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Mostrar un mensaje o manejar el error SSL aquí si es necesario
                handler?.proceed() // Solo si confías en el sitio
            }
        }

        setDownloadListener { url, _, _, _, _ ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                return@setDownloadListener
            }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setAllowedOverMetered(true)
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                val fileName = Uri.parse(url).lastPathSegment ?: "archivo_descargado"
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }

        loadUrl("https://www.cajondesastre.com.es/mariposa/app1/base.php")
    }

    AndroidView(factory = { webView })
}