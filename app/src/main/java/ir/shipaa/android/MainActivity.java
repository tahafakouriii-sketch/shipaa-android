package ir.shipaa.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {
  private static final String HOME = "https://shipaa.ir/my-learning/";
  private WebView web;
  private LinearLayout cover;
  private TextView status;
  private Button retry;
  private ValueCallback<Uri[]> upload;
  private static final int FILE_REQUEST = 41;
  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(Color.rgb(247,250,247));
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    FrameLayout frame=new FrameLayout(this);
    web=new WebView(this);
    web.setBackgroundColor(Color.WHITE);
    WebSettings settings=web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(true);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setMediaPlaybackRequiresUserGesture(true);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
    frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
    cover=new LinearLayout(this);
    cover.setGravity(Gravity.CENTER);
    cover.setOrientation(LinearLayout.VERTICAL);
    cover.setPadding(32,24,32,24);
    cover.setBackgroundColor(Color.rgb(248,250,247));
    TextView logo=new TextView(this);
    logo.setText("شیپا");
    logo.setTextSize(39);
    logo.setTypeface(null,Typeface.BOLD);
    logo.setTextColor(Color.rgb(78,151,65));
    logo.setGravity(Gravity.CENTER);
    cover.addView(logo);
    status=new TextView(this);
    status.setText("در حال آماده‌سازی...");
    status.setTextSize(14);
    status.setTextColor(Color.rgb(107,125,110));
    status.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-2,-2);
    st.topMargin=18;
    cover.addView(status,st);
    ProgressBar spinner=new ProgressBar(this);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(45,45);
    sp.topMargin=28;
    cover.addView(spinner,sp);
    retry=new Button(this);
    retry.setText("تلاش دوباره");
    retry.setVisibility(View.GONE);
    LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,-2);
    rp.topMargin=20;
    cover.addView(retry,rp);
    retry.setOnClickListener(v->{ retry.setVisibility(View.GONE);status.setText("در حال اتصال...");web.loadUrl(HOME); });
    frame.addView(cover,new FrameLayout.LayoutParams(-1,-1));
    setContentView(frame);
    web.setWebViewClient(new WebViewClient(){
      @Override public void onPageFinished(WebView v,String url){cover.setVisibility(View.GONE);}
      @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){
        if(request.isForMainFrame()){
          cover.setVisibility(View.VISIBLE);
          status.setText("اتصال برقرار نشد");
          retry.setVisibility(View.VISIBLE);
        }
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
        Uri uri=request.getUrl();
        String scheme=uri.getScheme(),host=uri.getHost();
        if("https".equals(scheme)&&host!=null&&(host.equals("shipaa.ir")||host.endsWith(".shipaa.ir"))) return false;
        if("http".equals(scheme)||"https".equals(scheme)||"mailto".equals(scheme)||"tel".equals(scheme)){
          try {startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){Toast.makeText(MainActivity.this,"لینک باز نشد",Toast.LENGTH_SHORT).show();}
        }
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){
        if(upload!=null)upload.onReceiveValue(null);
        upload=callback;
        try {startActivityForResult(params.createIntent(),FILE_REQUEST);return true;}
        catch(Exception e){upload=null;return false;}
      }
    });
    if(saved==null)web.loadUrl(HOME);else web.restoreState(saved);
  }
  @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);web.saveState(b);}
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==FILE_REQUEST&&upload!=null){upload.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));upload=null;}
  }
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(upload!=null){upload.onReceiveValue(null);upload=null;}if(web!=null)web.destroy();super.onDestroy();}
}
