package ir.shipaa.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebViewCompat.WebMessageListener;
import org.json.JSONObject;
import java.util.Collections;

/** Shipaa website remains the canonical UI. Native actions are added only to lesson pages. */
public final class SiteShellActivity extends Activity {
  private static final String HOME="https://shipaa.ir/my-account/";
  private static final String SITE="shipaa.ir";
  private FrameLayout root;
  private WebView web;
  private View customView;
  private WebChromeClient.CustomViewCallback customCallback;
  private NativeDownloadStore downloads;
  private boolean bridgeSupported=false;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(0xfff7faf8);
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    downloads=new NativeDownloadStore(this);
    root=new FrameLayout(this);
    web=new WebView(this);
    WebSettings settings=web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setAllowFileAccessFromFileURLs(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setJavaScriptCanOpenWindowsAutomatically(false);
    settings.setSupportMultipleWindows(false);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);

    // Only the exact HTTPS origin can communicate with Android.
    if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
      WebViewCompat.addWebMessageListener(web,"ShipaaNative",
          Collections.singleton("https://shipaa.ir"),
          (view,message,sourceOrigin,isMainFrame,reply)->{
            if(!isMainFrame || sourceOrigin==null ||
               !"https".equals(sourceOrigin.getScheme()) ||
               !SITE.equals(sourceOrigin.getHost())) return;
            try{
              JSONObject data=new JSONObject(message.getData());
              String action=data.optString("action");
              if("library".equals(action)){
                runOnUiThread(this::openLibrary);
              }else if("download".equals(action)){
                String url=data.optString("url"),title=data.optString("title","ویدئوی جلسه");
                if(NativeDownloadStore.validVideo(url))runOnUiThread(()->confirmDownload(url,title));
              }
            }catch(Exception ignored){}
          });
      bridgeSupported=true;
    }
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
        Uri uri=request.getUrl();
        String host=uri.getHost();
        if("https".equals(uri.getScheme()) && (SITE.equals(host)||"www.shipaa.ir".equals(host)))return false;
        String scheme=uri.getScheme();
        if("https".equals(scheme)||"http".equals(scheme)||"mailto".equals(scheme)||"tel".equals(scheme)||"eitaa".equals(scheme)){
          try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){toast("برنامه مناسب برای بازکردن پیوند پیدا نشد");}
        }
        return true;
      }
      @Override public void onPageFinished(WebView view,String url){
        if(bridgeSupported && url.startsWith("https://shipaa.ir/")) injectControls();
      }
      @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
        if(request.isForMainFrame())toast("صفحه در دسترس نیست؛ ویدئوهای دانلودشده را از کتابخانه آفلاین باز کن");
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public void onShowCustomView(View view,CustomViewCallback callback){
        if(customView!=null){callback.onCustomViewHidden();return;}
        customView=view;customCallback=callback;
        root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        web.setVisibility(View.GONE);
      }
      @Override public void onHideCustomView(){closeFullScreen();}
    });
    root.addView(web,new FrameLayout.LayoutParams(-1,-1));
    setContentView(root);
    web.loadUrl(HOME);
  }
  private void injectControls(){
    // A transient DOM control: no changes to the site's themes, snippets or navigation.
    final String js="(function(){if(!window.ShipaaNative||document.getElementById('shipaa-native-offline'))return;"
      +"var v=document.querySelector('video[src],video source[src]');"
      +"if(!v)return;var url=v.currentSrc||v.src||v.getAttribute('src');"
      +"if(!url&&v.parentElement&&v.parentElement.tagName==='VIDEO')url=v.parentElement.currentSrc;"
      +"if(!url)return;var wrap=document.createElement('div');wrap.id='shipaa-native-offline';"
      +"wrap.setAttribute('dir','rtl');wrap.style.cssText='display:flex;gap:8px;align-items:center;justify-content:flex-start;padding:10px 0;flex-wrap:wrap;';"
      +"var button=document.createElement('button');button.type='button';button.textContent='ذخیره آفلاین در اپ';"
      +"button.style.cssText='background:#ecf7e8;color:#2e7846;border:1px solid #c9e5c8;border-radius:14px;padding:10px 16px;min-height:44px;font:inherit;cursor:pointer;';"
      +"button.onclick=function(){ShipaaNative.postMessage(JSON.stringify({action:'download',url:url,title:document.title}));};"
      +"var library=document.createElement('button');library.type='button';library.textContent='ویدئوهای ذخیره‌شده';"
      +"library.style.cssText='background:#f6f5fa;color:#454556;border:1px solid #ddd8e8;border-radius:14px;padding:10px 16px;min-height:44px;font:inherit;cursor:pointer;';"
      +"library.onclick=function(){ShipaaNative.postMessage(JSON.stringify({action:'library'}));};"
      +"wrap.appendChild(button);wrap.appendChild(library);"
      +"var parent=(v.tagName==='SOURCE'?v.parentElement:v).parentElement;"
      +"if(parent)parent.insertAdjacentElement('afterend',wrap);})();";
    web.evaluateJavascript(js,null);
  }
  private void confirmDownload(String url,String title){
    if(!NativeDownloadStore.validVideo(url)){toast("منبع ویدئو پشتیبانی نمی‌شود");return;}
    new AlertDialog.Builder(this).setTitle("ذخیره آفلاین در شیپا")
      .setMessage("این ویدئو در حافظه اختصاصی برنامه ذخیره می‌شود. دریافت فقط برای جلساتی مجاز است که حق تماشای آن‌ها را دارید.")
      .setPositiveButton("شروع دانلود",(d,w)->startDownload(url,title,false))
      .setNeutralButton("فقط وای‌فای",(d,w)->startDownload(url,title,true))
      .setNegativeButton("انصراف",null).show();
  }
  private void startDownload(String url,String title,boolean wifi){
    try{
      downloads.enqueue(url,title,wifi);
      toast("دانلود ثبت شد؛ وضعیت را در کتابخانه آفلاین ببین");
      openLibrary();
    }catch(Exception e){toast("دانلود آغاز نشد؛ اتصال و حافظه دستگاه را بررسی کن");}
  }
  private void openLibrary(){startActivity(new Intent(this,OfflineDownloadsActivity.class));}
  private void closeFullScreen(){
    if(customView==null)return;
    root.removeView(customView);customView=null;
    if(customCallback!=null)customCallback.onCustomViewHidden();
    customCallback=null;
    web.setVisibility(View.VISIBLE);
  }
  private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
  @Override public void onBackPressed(){
    if(customView!=null){closeFullScreen();return;}
    if(web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("از شیپا خارج می‌شوی؟")
      .setPositiveButton("خروج",(d,w)->finish())
      .setNegativeButton("ماندن",null).show();
  }
  @Override protected void onPause(){CookieManager.getInstance().flush();super.onPause();}
  @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
