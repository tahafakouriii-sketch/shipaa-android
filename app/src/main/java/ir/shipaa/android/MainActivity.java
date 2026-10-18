package ir.shipaa.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
  private static final String HOME = "https://shipaa.ir/my-learning/";
  private WebView web;
  private ValueCallback<Uri[]> fileCallback;
  private final int FILE_REQUEST = 41;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(247,250,247));
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.WHITE);
    web=new WebView(this);
    web.setBackgroundColor(Color.WHITE);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(true);
    web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.getSettings().setMediaPlaybackRequiresUserGesture(true);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req){
        Uri uri=req.getUrl(); String host=uri.getHost(); String scheme=uri.getScheme();
        if(("https".equals(scheme)) && host!=null && (host.equals("shipaa.ir") || host.endsWith(".shipaa.ir"))) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW,uri)); } catch(Exception e){ Toast.makeText(MainActivity.this,"امکان بازکردن لینک نیست",Toast.LENGTH_SHORT).show(); }
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams p){
        if(fileCallback!=null) fileCallback.onReceiveValue(null);
        fileCallback=callback;
        try { startActivityForResult(p.createIntent(),FILE_REQUEST); return true; }
        catch(Exception e){fileCallback=null;return false;}
      }
    });
    root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(root);
    if(state==null) web.loadUrl(HOME); else web.restoreState(state);
  }
  @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);web.saveState(out);}
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==FILE_REQUEST && fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
  }
  @Override public void onBackPressed(){if(web!=null && web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override public void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
