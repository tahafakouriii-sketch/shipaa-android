package ir.shipaa.mobile

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val origin = "https://shipaa.ir"
    private var home = "$origin/"
    private val destinations = listOf(
        "خانه" to "$origin/",
        "دوره‌ها" to "$origin/shop/",
        "کتابخانه" to "$origin/",
        "حساب من" to "$origin/my-account/"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(246,250,247)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val toolbar = TextView(this).apply {
            text = "شیپا  •  آموزش و عضویت"
            setTextColor(Color.rgb(45,112,67)); textSize = 18f; gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setPadding(22, 14, 22, 14)
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(54)))
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url
                    if (url.scheme == "https" && (url.host == "shipaa.ir" || url.host == "www.shipaa.ir")) return false
                    if (url.scheme == "https" || url.scheme == "http" || url.scheme == "eitaa") {
                        return launchExternal(url)
                    }
                    return true
                }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.rgb(246,250,247)) }
        destinations.forEach { (title,url) ->
            val item = TextView(this).apply {
                text = title; gravity = Gravity.CENTER; textSize = 13f
                setTextColor(Color.rgb(45,112,67))
                setOnClickListener { web.loadUrl(url) }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        root.addView(nav)
        setContentView(root)
        web.loadUrl(home)
    }
    private fun launchExternal(uri: Uri): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: Exception) { true }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
