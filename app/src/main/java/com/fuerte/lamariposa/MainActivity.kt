package com.fuerte.lamariposa

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.fuerte.lamariposa.ui.theme.LamariposaTheme
import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LamariposaTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebViewComponent()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LamariposaTheme {
        Greeting("Android")
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WebViewComponent() {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val webSettings: WebSettings = settings
            webSettings.javaScriptEnabled = true // Habilita JavaScript si es necesario

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url.toString()
                    if (url?.startsWith("http://www.cajondesastre.com.es") == true) {
                        // Cargar la URL en el WebView
                        view?.loadUrl(url)
                        return true // indica que la navegación ha sido manejada
                    } else {
                        // Evitar que la URL se cargue en el WebView
                        return false
                    }
                }
            }

            setDownloadListener { url, _, _, _, _ ->
                // Verificar y solicitar permisos de almacenamiento si es necesario
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

                // Configurar la solicitud de descarga
                val request = DownloadManager.Request(Uri.parse(url))
                request.setAllowedOverMetered(true)
                request.setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                request.addRequestHeader("Cookie", cookie)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // Obtener el nombre del archivo de la URL
                val uri = Uri.parse(url)
                val fileName = uri.lastPathSegment
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                // Obtener el servicio de descarga y encolar la solicitud
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
            }

            // Cargar la URL inicial
            loadUrl("http://www.cajondesastre.com.es/mariposa/app1/base.php")
        }
    }

    AndroidView(factory = { webView })
}
