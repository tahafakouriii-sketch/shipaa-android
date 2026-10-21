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
import android.widget.Toast

/**
 * Android-first learner shell. Existing WordPress authentication remains authoritative.
 * This activity does NOT infer paid access or expose direct lesson video URLs.
 */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var container: FrameLayout
    private var fullScreenView: View? = null
    private var fullScreenCallback: WebChromeClient.CustomViewCallback? = null
    private val origin = "https://shipaa.ir"
    private val home = "$origin/my-account/"
    private val destinations = listOf(
        "خانه من" to home,
        "دوره‌ها" to "$origin/shop/",
        "کتابخانه" to "$origin/library/",
        "پروفایل" to home
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 250, 248)
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val title = TextView(this).apply {
            text = "شیپا  •  خانه یادگیری"
            textSize = 18f
            setTextColor(Color.rgb(51, 135, 69))
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setPadding(dp(18), 0, dp(18), 0)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(52)))

        container = FrameLayout(this)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val u = request.url
                    if (u.scheme == "https" && (u.host == "shipaa.ir" || u.host == "www.shipaa.ir")) return false
                    if (u.scheme in listOf("https", "http", "eitaa", "mailto", "tel")) return openExternal(u)
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (fullScreenView != null) { callback.onCustomViewHidden(); return }
                    fullScreenView = view
                    fullScreenCallback = callback
                    container.addView(view, FrameLayout.LayoutParams(-1, -1))
                    this@MainActivity.web.visibility = View.GONE
                    title.visibility = View.GONE
                }
                override fun onHideCustomView() { closeFullScreen(title) }
            }
        }
        container.addView(web, FrameLayout.LayoutParams(-1, -1))
        root.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(247, 250, 248))
        }
        destinations.forEach { (label, url) ->
            nav.addView(TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(Color.rgb(47, 111, 67))
                setOnClickListener { web.loadUrl(url) }
            }, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        root.addView(nav)
        setContentView(root)
        web.loadUrl(home)
    }

    private fun closeFullScreen(toolbar: TextView? = null) {
        fullScreenView?.let { container.removeView(it) }
        fullScreenView = null
        fullScreenCallback?.onCustomViewHidden()
        fullScreenCallback = null
        web.visibility = View.VISIBLE
        toolbar?.visibility = View.VISIBLE
    }

    private fun openExternal(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            Toast.makeText(this, "برنامه مناسب برای باز کردن پیوند پیدا نشد", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun dp(px: Int): Int = (px * resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (fullScreenView != null) { closeFullScreen(); return }
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
