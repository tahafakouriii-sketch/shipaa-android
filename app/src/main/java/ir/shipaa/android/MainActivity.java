package ir.shipaa.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
  private static final String SITE="https://shipaa.ir";
  private static final String CATALOG=SITE+"/wp-json/wc/store/v1/products?per_page=30";
  private static final int PICK_FILE=41;
  private final int GREEN=Color.rgb(63,151,89), INK=Color.rgb(34,58,51), MUTED=Color.rgb(99,117,111);
  private FrameLayout root;
  private WebView web;
  private LinearLayout home, cards;
  private ScrollView homeScreen;
  private TextView catalogStatus;
  private ValueCallback<Uri[]> upload;
  private boolean inWeb=false;
  private String currentPage=SITE+"/";

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(247,250,248));
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    root=new FrameLayout(this);
    setContentView(root);
    createHome();
    createWeb();
    showHome();
    fetchCourses();
  }
  private GradientDrawable background(int color,int radius){
    GradientDrawable d=new GradientDrawable();
    d.setColor(color); d.setCornerRadius(dp(radius)); return d;
  }
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private TextView text(String s,int sp,int color,boolean bold){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);
    t.setGravity(Gravity.RIGHT); t.setTextDirection(View.TEXT_DIRECTION_RTL);
    if(bold)t.setTypeface(null,Typeface.BOLD);return t;
  }
  private LinearLayout.LayoutParams lp(int top){
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);return p;
  }
  private void createHome(){
    ScrollView scroll=new ScrollView(this);homeScreen=scroll;scroll.setFillViewport(true);
    scroll.setBackgroundColor(Color.rgb(247,250,248));
    home=new LinearLayout(this); home.setOrientation(LinearLayout.VERTICAL);
    home.setPadding(dp(22),dp(26),dp(22),dp(36));
    home.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    scroll.addView(home);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
    home.addView(text("شیپا  •  یادگیری نقطه آغاز حرکت",16,GREEN,true));
    home.addView(text("سلام، به شیپا خوش آمدید",27,INK,true),lp(28));
    home.addView(text("دوره‌های آموزشی و مسیر یادگیری شما، در یک جا.",15,MUTED,false),lp(8));
    LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);
    hero.setPadding(dp(20),dp(22),dp(20),dp(22));
    hero.setBackground(background(Color.rgb(229,246,232),22));
    hero.addView(text("مسیر یادگیری من",21,INK,true));
    hero.addView(text("برای دیدن جلسات و ادامه دوره‌ها وارد حساب خود شوید.",14,MUTED,false),lp(9));
    Button mine=button("ورود به دوره‌های من",true,()->openPage(SITE+"/my-learning/"));
    hero.addView(mine,lp(18));
    home.addView(hero,lp(22));
    LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);
    quick.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    Button shop=button("همه دوره‌ها",false,()->openPage(SITE+"/shop/"));
    Button account=button("حساب کاربری",false,()->openPage(SITE+"/my-account/"));
    quick.addView(shop,new LinearLayout.LayoutParams(0,dp(56),1));
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(56),1);ap.leftMargin=dp(10);quick.addView(account,ap);
    home.addView(quick,lp(16));
    home.addView(text("جدیدترین دوره‌ها",22,INK,true),lp(32));
    catalogStatus=text("در حال دریافت دوره‌ها از فروشگاه شیپا…",13,MUTED,false);
    home.addView(catalogStatus,lp(10));
    cards=new LinearLayout(this);cards.setOrientation(LinearLayout.VERTICAL);
    home.addView(cards,lp(8));
    home.addView(text("شیپا • نسخه آزمایشی اندروید",12,MUTED,false),lp(28));
  }
  private Button button(String label,boolean primary,Runnable onClick){
    Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);
    b.setTextColor(primary?Color.WHITE:INK);
    b.setBackground(background(primary?GREEN:Color.rgb(235,239,245),16));
    b.setOnClickListener(v->onClick.run());return b;
  }
  private void fetchCourses(){
    new Thread(()->{
      try{
        HttpURLConnection con=(HttpURLConnection)new URL(CATALOG).openConnection();
        con.setRequestMethod("GET");con.setConnectTimeout(12000);con.setReadTimeout(12000);
        con.setRequestProperty("Accept","application/json");
        int code=con.getResponseCode();
        if(code!=200)throw new Exception("HTTP "+code);
        BufferedReader reader=new BufferedReader(new InputStreamReader(con.getInputStream(),"UTF-8"));
        StringBuilder data=new StringBuilder();String line;
        while((line=reader.readLine())!=null)data.append(line);
        reader.close();con.disconnect();
        JSONArray products=new JSONArray(data.toString());
        runOnUiThread(()->displayCourses(products));
      }catch(Exception e){
        runOnUiThread(()->{
          catalogStatus.setText("فهرست دوره‌ها در دسترس نیست؛ از دکمه همه دوره‌ها استفاده کنید.");
          cards.removeAllViews();
          cards.addView(button("باز کردن فروشگاه شیپا",true,()->openPage(SITE+"/shop/")),lp(12));
        });
      }
    }).start();
  }
  private void displayCourses(JSONArray products){
    cards.removeAllViews();
    int count=Math.min(products.length(),8);
    catalogStatus.setText(count==0?"فعلاً دوره‌ای برای نمایش دریافت نشد.":"دوره‌های منتشرشده، مستقیم از ووکامرس");
    for(int i=0;i<count;i++){
      try{
        JSONObject p=products.getJSONObject(i);
        String title=android.text.Html.fromHtml(p.optString("name","دوره شیپا"),android.text.Html.FROM_HTML_MODE_LEGACY).toString();
        String permalink=p.optString("permalink",SITE+"/shop/");
        if(!isShipaa(permalink))permalink=SITE+"/shop/";
        final String link=permalink;
        LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(18),dp(17),dp(18),dp(17));
        tile.setBackground(background(Color.WHITE,18));
        tile.addView(text(title,17,INK,true));
        tile.addView(text("مشاهده معرفی و ثبت‌نام  ←",13,GREEN,false),lp(10));
        tile.setOnClickListener(v->openPage(link));
        cards.addView(tile,lp(11));
      }catch(Exception ignored){}
    }
    if(count==0)cards.addView(button("همه دوره‌ها",true,()->openPage(SITE+"/shop/")),lp(12));
  }
  private boolean isShipaa(String url){
    try { Uri u=Uri.parse(url);String h=u.getHost();
      return "https".equalsIgnoreCase(u.getScheme())&&h!=null&&(h.equals("shipaa.ir")||h.endsWith(".shipaa.ir"));
    }catch(Exception e){return false;}
  }
  private void createWeb(){
    web=new WebView(this);web.setBackgroundColor(Color.WHITE);
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
    s.setAllowFileAccess(false);s.setAllowContentAccess(true);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
        Uri u=req.getUrl(); if(isShipaa(u.toString()))return false;
        String scheme=u.getScheme();
        if("https".equals(scheme)||"http".equals(scheme)||"mailto".equals(scheme)||"tel".equals(scheme)||"eitaa".equals(scheme)){
          try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ex){Toast.makeText(MainActivity.this,"برنامه مناسب برای بازکردن لینک پیدا نشد",Toast.LENGTH_SHORT).show();}
        }return true;
      }
      @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError err){
        if(req.isForMainFrame())Toast.makeText(MainActivity.this,"بارگذاری صفحه ناموفق بود؛ اتصال اینترنت را بررسی کنید",Toast.LENGTH_LONG).show();
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams params){
        if(upload!=null)upload.onReceiveValue(null);upload=cb;
        try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}
        catch(Exception e){upload=null;return false;}
      }
    });
    root.addView(web,new FrameLayout.LayoutParams(-1,-1));
  }
  private void showHome(){inWeb=false;web.setVisibility(View.GONE);homeScreen.setVisibility(View.VISIBLE);}
  private void openPage(String url){
    if(!isShipaa(url))return;
    inWeb=true;currentPage=url;
    homeScreen.setVisibility(View.GONE);
    web.setVisibility(View.VISIBLE);web.loadUrl(url);
  }
  @Override public void onBackPressed(){
    if(inWeb){if(web.canGoBack())web.goBack();else showHome();}
    else super.onBackPressed();
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==PICK_FILE&&upload!=null){upload.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));upload=null;}
  }
  @Override protected void onDestroy(){
    if(upload!=null){upload.onReceiveValue(null);upload=null;}
    if(web!=null)web.destroy();super.onDestroy();
  }
}
