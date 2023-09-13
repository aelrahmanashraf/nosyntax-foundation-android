package app.mynta.template.android.presentation.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import app.mynta.template.android.core.Constants
import app.mynta.template.android.core.components.ChangeScreenOrientationComponent
import app.mynta.template.android.core.components.NoConnectionComponent
import app.mynta.template.android.core.components.SystemUIControllerComponent
import app.mynta.template.android.core.components.SystemUIState
import app.mynta.template.android.core.utility.Connectivity
import app.mynta.template.android.core.utility.Intents.handleUrlAction
import app.mynta.template.android.core.utility.Utilities.isUrlValid
import app.mynta.template.android.core.utility.WebKitChromeClient
import app.mynta.template.android.core.utility.WebKitClient
import app.mynta.template.android.core.utility.WebView
import app.mynta.template.android.core.utility.rememberSaveableWebViewState
import app.mynta.template.android.core.utility.rememberWebViewNavigator
import app.mynta.template.android.domain.model.app_config.AppConfig
import app.mynta.template.android.presentation.web.components.AlertDialogComponent
import app.mynta.template.android.presentation.web.components.ConfirmDialogComponent
import app.mynta.template.android.presentation.web.components.JsDialog
import app.mynta.template.android.presentation.web.components.LoadingIndicator
import app.mynta.template.android.presentation.web.components.PromptDialogComponent
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(appConfig: AppConfig, url: String, drawerState: DrawerState) {
    val context = LocalContext.current
    val webKitConfig = appConfig.modules.webkit

    val systemUiState = remember { mutableStateOf(SystemUIState.SYSTEM_UI_VISIBLE) }
    var requestedOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

    val webViewState = rememberSaveableWebViewState()
    val navigator = rememberWebViewNavigator()

    var jsDialogInfo by rememberSaveable { mutableStateOf<Pair<JsDialog?, JsResult?>?>(null) }
    var customWebView by rememberSaveable { mutableStateOf<View?>(null) }
    var customWebViewCallback by rememberSaveable { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var noConnectionState by rememberSaveable { mutableStateOf(false) }

    SystemUIControllerComponent(systemUiState = systemUiState)
    ChangeScreenOrientationComponent(orientation = requestedOrientation)

    LaunchedEffect(navigator) {
        if (webViewState.viewState == null) {
            navigator.loadUrl(url)
        }
    }

    WebView(
        modifier = Modifier.fillMaxSize(),
        state = webViewState,
        navigator = navigator,
        captureBackPresses = !drawerState.isOpen,
        onCreated = { webView ->
            webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = Constants.WEB_JAVASCRIPT_OPTION
                    allowFileAccess = Constants.WEB_ALLOW_FILE_ACCESS
                    allowContentAccess = Constants.WEB_ALLOW_CONTENT_ACCESS
                    domStorageEnabled = Constants.WEB_DOM_STORAGE_ENABLED
                    databaseEnabled = Constants.WEB_DATABASE_ENABLED
                    javaScriptCanOpenWindowsAutomatically = Constants.JAVASCRIPT_CAN_OPEN_WINDOWS_AUTOMATICALLY
                    cacheMode = WebSettings.LOAD_DEFAULT
                    supportMultipleWindows()
                    setGeolocationEnabled(Constants.WEB_SET_GEOLOCATION_ENABLED)
                }

                if (webKitConfig.userAgent.android != "") {
                    settings.userAgentString = webKitConfig.userAgent.android
                }

                setDownloadListener { url, _, _, _, _ ->
                    context.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
                }
            }
        },
        client = webClient(
            context = context,
            onResourceLoaded = {
                val resourceContainer =
                    """javascript:(function() { 
                        var node = document.createElement('style');
                        node.type = 'text/css';
                        node.innerHTML = '${webKitConfig.customCss}';
                        document.head.appendChild(node);
                    })()""".trimIndent()
                navigator.loadUrl(resourceContainer)
            },
            onRequestInterrupted = {
                noConnectionState = true
            }
        ),
        chromeClient = chromeClient(
            context = context,
            onJsDialog = { dialog, result ->
                jsDialogInfo = dialog to result
            },
            onCustomViewShown = { view, callback ->
                if (customWebView != null) {
                    systemUiState.value = SystemUIState.SYSTEM_UI_VISIBLE
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                    customWebView = null
                    customWebViewCallback?.onCustomViewHidden()
                    customWebViewCallback = null
                } else {
                    customWebView = view
                    customWebViewCallback = callback

                    systemUiState.value = SystemUIState.SYSTEM_UI_HIDDEN
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            },
            onCustomViewHidden = {
                if (customWebView != null) {
                    systemUiState.value = SystemUIState.SYSTEM_UI_VISIBLE
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                    customWebView = null
                    customWebViewCallback?.onCustomViewHidden()
                    customWebViewCallback = null
                }
            }
        )
    )


    if (customWebView != null) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mContext ->
            val frameLayout = FrameLayout(mContext)
            frameLayout.addView(customWebView)
            frameLayout
        })
    }

    if (webViewState.isLoading) {
        val indicatorConfig = appConfig.appearance.components.loadingIndicator
        LoadingIndicator(indicatorConfig = indicatorConfig)
    }

    // TODO: Reset connection state when update navigate.
    if (noConnectionState) {
        NoConnectionComponent(onRetry = {
            if (Connectivity.getInstance().isOnline()) {
                noConnectionState = false
                navigator.reload()
            }
        })
    }

    jsDialogInfo?.let { (dialog, result) ->
        val cancelDialog: () -> Unit = {
            jsDialogInfo = null
            result?.cancel()
        }
        val confirmDialog: () -> Unit = {
            jsDialogInfo = null
            result?.confirm()
        }

        when(dialog) {
            is JsDialog.Alert -> {
                AlertDialogComponent(
                    message = dialog.message,
                    onConfirm = confirmDialog
                )
            }
            is JsDialog.Confirm -> {
                ConfirmDialogComponent(
                    message = dialog.message,
                    onCancel = cancelDialog,
                    onConfirm = confirmDialog
                )
            }
            is JsDialog.Prompt -> {
                PromptDialogComponent(
                    message = dialog.message,
                    defaultValue = dialog.defaultValue,
                    onCancel = cancelDialog,
                    onConfirm = { promptResult ->
                        if (result is JsPromptResult) {
                            result.confirm(promptResult)
                        }
                        cancelDialog()
                    }
                )
            }
            else -> { }
        }
    }
}

@Composable
fun webClient(
    context: Context,
    onResourceLoaded: () -> Unit,
    onRequestInterrupted: () -> Unit
): WebKitClient {
    val webClient = remember {
        object: WebKitClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                return if (isUrlValid(url)) {
                    navigator.loadUrl(url)
                    true
                } else {
                    context.handleUrlAction(url)
                    true
                }
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                onResourceLoaded()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                onRequestInterrupted()
            }
        }
    }

    return webClient
}

@Composable
fun chromeClient(
    context: Context,
    onJsDialog: (JsDialog, JsResult) -> Unit,
    onCustomViewShown: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onCustomViewHidden: () -> Unit
): WebKitChromeClient {
    var filePath by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }

    val fileChooser = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            var results: Array<Uri> = emptyArray()

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                if (data != null) {
                    results = arrayOf(Uri.parse(data.dataString))
                } else {
                    if (imagePath != null) {
                        val file = File(Uri.parse(imagePath).path)
                        if (file.length() > 0) {
                            results = arrayOf(Uri.parse(imagePath))
                        }
                    }
                }
            }
            filePath?.onReceiveValue(results)
            filePath = null
        }
    )

    val chromeClient = remember {
        object: WebKitChromeClient() {
            @SuppressLint("QueryPermissionsNeeded")
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                if (filePath != null) {
                    filePath?.onReceiveValue(null)
                }
                filePath = filePathCallback

                var cameraCapture: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (cameraCapture?.resolveActivity(context.packageManager) != null) {
                    var imageFile: File? = null
                    try {
                        imageFile = createImageFile(context)
                        cameraCapture.putExtra("PhotoPath", imagePath)
                    } catch (e: IOException) {
                        Log.e("TAG", e.localizedMessage)
                    }
                    if (imageFile != null) {
                        imagePath = "file:" + imageFile.absolutePath
                        cameraCapture.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", imageFile)
                        )
                    } else {
                        cameraCapture = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "image/* video/*"
                    )
                }
                val mimeTypes = arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "application/pdf",
                    "image/*",
                    "video/*",
                    "*/*"
                )
                contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

                val intentArray: Array<Intent?>
                intentArray = if (cameraCapture != null) {
                    arrayOf(cameraCapture)
                } else {
                    arrayOfNulls(0)
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                    putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    putExtra(Intent.EXTRA_TITLE, "Upload")
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                }
                fileChooser.launch(chooserIntent)

                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                message?.let { onJsDialog(JsDialog.Alert(it), result!!) }
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                message?.let { onJsDialog(JsDialog.Confirm(it), result!!) }
                return true
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                message?.let { onJsDialog(JsDialog.Prompt(it, defaultValue.toString()), result!!) }
                return true
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return BitmapFactory.decodeResource(context.resources, 2130837573)
            }

            override fun onHideCustomView() {
                onCustomViewHidden()
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    onCustomViewShown(view, callback)
                }
            }
        }
    }

    return chromeClient
}

private fun createImageFile(context: Context): File? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_$timeStamp"
    val mediaStorageDir = context.cacheDir
    if (!mediaStorageDir.exists()) {
        if (!mediaStorageDir.mkdirs()) {
            Log.d("", "Oops! Failed create " + "WebView" + " directory")
            return null
        }
    }
    return File.createTempFile(
        imageFileName,
        ".jpg",
        mediaStorageDir
    )
}