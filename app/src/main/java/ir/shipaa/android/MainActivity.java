package ir.shipaa.android;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  static final String SITE="https://shipaa.ir";
  final ExecutorService io=Executors.newFixedThreadPool(3);
  final Handler handler=new Handler(Looper.getMainLooper());
  LinearLayout root,body,nav; WebView web;
  android.content.SharedPreferences prefs;
  JSONArray courses=new JSONArray();
  String tab="home",nonce="",name=""; long uid; int generation=0;
  final int ink=Color.rgb(37,57,47),green=Color.rgb(64,118,64),muted=Color.rgb(105,120,111);
  final java.util.Map<Long,TextView> downloadLabels=new java.util.HashMap<>();
  final java.util.Map<Long,Button> downloadButtons=new java.util.HashMap<>();
  final Runnable refreshDownloads=new Runnable(){public void run(){if(tab.equals("downloads")&&web==null)updateDownloadStatus();handler.postDelayed(this,2000);}};
  @Override public void onCreate(Bundle saved){super.onCreate(saved);prefs=getSharedPreferences("learner",MODE_PRIVATE);uid=prefs.getLong("uid",0);name=prefs.getString("name","");try{courses=new JSONArray(prefs.getString("courses","[]"));}catch(Exception ignored){} render();session(false);}
  int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  GradientDrawable bg(int color,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;}
  TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(7),0,dp(7));t.setGravity(Gravity.RIGHT);return t;}
  Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(green);b.setTextSize(14);b.setMinHeight(dp(48));b.setBackground(bg(Color.rgb(234,243,229),14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,dp(8));b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
  LinearLayout card(int color){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(14),dp(18),dp(14));c.setBackground(bg(color,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(10));c.setLayoutParams(p);body.addView(c);return c;}
  void render(){
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(Color.rgb(248,250,247));
    root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});
    LinearLayout header=new LinearLayout(this);header.setPadding(dp(22),dp(6),dp(22),dp(6));header.setGravity(Gravity.CENTER_VERTICAL);
    TextView logo=text("شیپا",30,green);logo.setTypeface(null,Typeface.BOLD);header.addView(logo,new LinearLayout.LayoutParams(0,-2,1));TextView brand=text("یادگیری، آغاز حرکت",12,muted);header.addView(brand);root.addView(header);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(8),dp(20),dp(20));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    nav=new LinearLayout(this);nav.setBackgroundColor(Color.WHITE);String[] keys={"home","courses","downloads","profile"},labels={"خانه","دوره‌های من","دانلودها","حساب من"};
    for(int i=0;i<keys.length;i++){final String key=keys[i];TextView b=text(labels[i],12,key.equals(tab)?green:muted);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(60));if(key.equals(tab)){b.setTypeface(null,Typeface.BOLD);b.setBackground(bg(Color.rgb(237,246,232),18));}b.setOnClickListener(v->{generation++;tab=key;render();});nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);setContentView(root);
    if(tab.equals("downloads")){downloads();return;}if(tab.equals("profile")){profile();return;}
    if(tab.equals("home")){body.addView(text(uid>0?"سلام، "+name:"به شیپا خوش آمدی",24,ink));LinearLayout hero=card(Color.rgb(232,242,222));hero.addView(text("هر روز، یک قدم جلوتر",25,green));hero.addView(text("آموزش‌های تو، همراه تو\nآنلاین یاد بگیر؛ آفلاین ادامه بده.",14,muted));hero.addView(button(uid>0?"ادامه یادگیری":"ورود به حساب شیپا",()->{if(uid>0){tab="courses";render();}else login();}));body.addView(text("مسیر یادگیری من",20,ink));}
    else body.addView(text("دوره‌های من",26,ink));
    if(uid==0){body.addView(text("با همان حساب سایت وارد شو تا دوره‌هایت اینجا نمایش داده شوند.",15,muted));body.addView(button("ورود / ساخت حساب",this::login));}
    else {if(courses.length()==0)body.addView(text("هنوز دوره‌ای نمایش داده نمی‌شود. برای دریافت آخرین وضعیت، تازه‌سازی کن.",15,muted));for(int i=0;i<courses.length();i++){JSONObject c=courses.optJSONObject(i);if(c!=null)courseCard(c);}body.addView(button("تازه‌سازی دوره‌ها",()->session(true)));}
    if(tab.equals("home")){body.addView(button("آشنایی با همه دوره‌ها",()->sitePage(SITE+"/shop/")));body.addView(button("کتابخانه شیپا",()->sitePage(SITE+"/library/")));}
  }
  void courseCard(JSONObject c){LinearLayout item=card(Color.WHITE);String url=c.optString("image");if(!url.isEmpty()){ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);item.addView(img,new LinearLayout.LayoutParams(-1,dp(160)));io.execute(()->{try{HttpURLConnection conn=connection(url, false);try(InputStream in=conn.getInputStream()){Bitmap b=BitmapFactory.decodeStream(in);runOnUiThread(()->img.setImageBitmap(b));}finally{conn.disconnect();}}catch(Exception ignored){}});}TextView title=text(c.optString("title"),19,ink);title.setTypeface(null,Typeface.BOLD);item.addView(title);item.addView(text(c.optString("duration"),12,muted));item.addView(button("مشاهده جلسات",()->lessons(c)));}
  HttpURLConnection connection(String url,boolean auth)throws Exception{URL u=new URL(url);if(!u.getProtocol().equals("https"))throw new IOException("HTTPS required");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");if(auth){String cookie=CookieManager.getInstance().getCookie(SITE);if(cookie!=null)c.setRequestProperty("Cookie",cookie);if(!nonce.isEmpty())c.setRequestProperty("X-WP-Nonce",nonce);}return c;}
  JSONObject get(String path)throws Exception{HttpURLConnection c=connection(SITE+path,true);try{int code=c.getResponseCode();if(code==401||code==403)throw new SecurityException();if(code!=200)throw new IOException("HTTP "+code);ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2*1024*1024)throw new IOException("Too large");}}return new JSONObject(out.toString("UTF-8"));}finally{c.disconnect();}}
  void session(boolean feedback){int request=++generation;io.execute(()->{try{JSONObject s=get("/?shipaa_native_session=1&_="+System.currentTimeMillis());runOnUiThread(()->{if(isFinishing()||request!=generation)return;long next=s.optLong("id");if(uid!=next){clearDownloads();courses=new JSONArray();prefs.edit().remove("courses").apply();}uid=next;name=s.optString("name");nonce=s.optString("nonce");prefs.edit().putLong("uid",uid).putString("name",name).apply();if(uid>0)loadCourses(feedback);else if(web==null)render();});}catch(Exception e){if(feedback)runOnUiThread(()->toast("اتصال برقرار نشد؛ دانلودهای کامل‌شده همچنان در دسترس‌اند."));}});}
  void loadCourses(boolean feedback){long owner=uid;io.execute(()->{try{JSONObject result=get("/wp-json/shipaa-native/v1/courses");runOnUiThread(()->{if(uid!=owner)return;courses=result.optJSONArray("courses");if(courses==null)courses=new JSONArray();prefs.edit().putString("courses",courses.toString()).apply();if(web==null)render();if(feedback)toast("دوره‌ها به‌روز شد");});}catch(Exception e){runOnUiThread(()->toast(e instanceof SecurityException?"برای دریافت جلسات، دوباره وارد حساب شو":"دریافت دوره‌ها انجام نشد؛ دوباره تلاش کن"));}});}
  void lessons(JSONObject course){tab="courses";render();body.removeAllViews();body.addView(text(course.optString("title"),24,ink));body.addView(text("در حال دریافت جلسات…",14,muted));int request=++generation;io.execute(()->{try{JSONObject result=get("/wp-json/shipaa-native/v1/courses/"+course.optLong("id")+"/lessons");runOnUiThread(()->{if(request!=generation||web!=null)return;body.removeAllViews();body.addView(button("بازگشت به دوره‌ها",()->{generation++;render();}));body.addView(text(course.optString("title"),23,ink));JSONArray list=result.optJSONArray("lessons");if(list==null||list.length()==0)body.addView(text("محتوای قابل پخش پیدا نشد.",14,muted));else for(int i=0;i<list.length();i++){JSONObject l=list.optJSONObject(i);if(l==null)continue;LinearLayout row=card(Color.WHITE);row.addView(text(l.optString("title"),17,ink));if(l.optBoolean("available")&&trustedMedia(l.optString("url"))){row.addView(button("پخش و ادامه از آخرین لحظه",()->play(l.optString("url"),l.optString("id"),l.optString("title"))));row.addView(button("دانلود برای تماشای آفلاین",()->download(l)));}else row.addView(text(l.optString("reason"),13,muted));}body.addView(button("سایر محتوای دوره",()->sitePage(SITE+"/?p="+course.optLong("id"))));});}catch(Exception e){runOnUiThread(()->{if(request!=generation)return;body.removeAllViews();body.addView(text("دریافت جلسات انجام نشد. اتصال و ورود به حساب را بررسی کن.",15,muted));body.addView(button("تلاش دوباره",()->lessons(course)));body.addView(button("ورود به حساب",this::login));});}});}
  boolean trustedMedia(String url){Uri u=Uri.parse(url);return "https".equals(u.getScheme())&&("shipaa.ir".equals(u.getHost())||"dl.shipaa.ir".equals(u.getHost()))&&u.getPath()!=null&&u.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4");}
  void play(String url,String id,String title){Intent i=new Intent(this,PlayerActivity.class);i.putExtra("uri",url);i.putExtra("key",uid+":"+id);i.putExtra("title",title);startActivity(i);}
  JSONArray records(){try{return new JSONArray(prefs.getString("downloads","[]"));}catch(Exception e){return new JSONArray();}}
  void download(JSONObject l){if(uid==0||!trustedMedia(l.optString("url")))return;JSONArray all=records();for(int i=0;i<all.length();i++)if(all.optJSONObject(i).optString("lesson").equals(l.optString("id"))){toast("این ویدیو در بخش دانلودهاست");return;}
    new AlertDialog.Builder(this).setTitle("دانلود ویدیو").setMessage("برای تماشای آفلاین، ویدیو روی این دستگاه ذخیره می‌شود. با خروج از حساب حذف خواهد شد.").setPositiveButton("فقط وای‌فای",(d,w)->enqueue(l,true)).setNeutralButton("با اینترنت فعلی",(d,w)->enqueue(l,false)).setNegativeButton("انصراف",null).show();}
  void enqueue(JSONObject l,boolean wifi){try{DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);DownloadManager.Request r=new DownloadManager.Request(Uri.parse(l.optString("url")));r.setTitle(l.optString("title"));r.setMimeType("video/mp4");r.setAllowedOverMetered(!wifi);r.setAllowedOverRoaming(false);r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);r.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_MOVIES,uid+"-"+l.optString("id")+".mp4");long id=dm.enqueue(r);JSONObject rec=new JSONObject();rec.put("download",id);rec.put("owner",uid);rec.put("lesson",l.optString("id"));rec.put("title",l.optString("title"));JSONArray a=records();a.put(rec);prefs.edit().putString("downloads",a.toString()).apply();toast("دانلود شروع شد");}catch(Exception e){toast("دانلود شروع نشد؛ فضای خالی و اتصال را بررسی کن");}}
  void downloads(){
    downloadLabels.clear(); downloadButtons.clear();
    body.addView(text("همراه تو، حتی آفلاین",25,ink));
    body.addView(text("ویدیوهای ذخیره‌شده روی این دستگاه",13,muted));
    JSONArray all=records();
    if(all.length()==0)body.addView(text("از داخل جلسات دوره، ویدیوی دلخواهت را دانلود کن.",16,muted));
    DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
    for(int i=0;i<all.length();i++){
      JSONObject rec=all.optJSONObject(i);
      if(rec==null||uid==0||rec.optLong("owner")!=uid)continue;
      long id=rec.optLong("download");
      LinearLayout row=card(Color.WHITE); row.addView(text(rec.optString("title"),17,ink));
      TextView status=text("در حال بررسی دانلود…",13,muted);row.addView(status);downloadLabels.put(id,status);
      Button play=button("پخش آفلاین",()->{Uri uri=dm.getUriForDownloadedFile(id);if(uri!=null)play(uri.toString(),rec.optString("lesson"),rec.optString("title"));else toast("فایل پیدا نشد؛ دوباره دانلود کن");});
      play.setEnabled(false);row.addView(play);downloadButtons.put(id,play);
      row.addView(button("حذف / لغو دانلود",()->{
        dm.remove(id);JSONArray keep=new JSONArray();JSONArray old=records();
        for(int j=0;j<old.length();j++)if(old.optJSONObject(j).optLong("download")!=id)keep.put(old.optJSONObject(j));
        prefs.edit().putString("downloads",keep.toString()).apply();render();
      }));
    }
    updateDownloadStatus();
  }
  void updateDownloadStatus(){
    DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
    for(java.util.Map.Entry<Long,TextView> entry:downloadLabels.entrySet()){
      long id=entry.getKey();TextView label=entry.getValue();Button play=downloadButtons.get(id);
      try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(id))){
        if(c==null||!c.moveToFirst()){label.setText("فایل در دسترس نیست");play.setEnabled(false);continue;}
        int state=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
        long bytes=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        long total=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        boolean ready=state==DownloadManager.STATUS_SUCCESSFUL;play.setEnabled(ready);play.setAlpha(ready?1f:0.45f);
        label.setText(ready?"آماده تماشای آفلاین · "+(bytes/1024/1024)+" مگابایت":state==DownloadManager.STATUS_FAILED?"دانلود ناموفق؛ حذف کن و دوباره از جلسه دانلود کن":state==DownloadManager.STATUS_PAUSED?"در انتظار اتصال مناسب":total>0?"در حال دانلود · "+(100*bytes/total)+"٪":"در انتظار شروع دانلود");
      }catch(Exception e){label.setText("وضعیت دانلود در دسترس نیست");play.setEnabled(false);}
    }
  }
  void clearDownloads(){DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);JSONArray a=records();for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(r!=null)dm.remove(r.optLong("download"));}prefs.edit().remove("downloads").apply();}
  void profile(){body.addView(text("حساب من",26,ink));LinearLayout c=card(Color.rgb(239,234,247));c.addView(text(uid>0?name:"مهمان شیپا",23,ink));c.addView(text("همان حساب سایت، در کنار یادگیری همراه",14,muted));body.addView(button(uid>0?"ویرایش حساب و خدمات شیپا":"ورود / ثبت‌نام",()->sitePage(SITE+"/my-account/")));body.addView(button("تازه‌سازی حساب",()->session(true)));if(uid>0)body.addView(button("خروج از حساب",()->new AlertDialog.Builder(this).setMessage("با خروج از حساب، دانلودهای این دستگاه حذف می‌شوند.").setPositiveButton("خروج",(d,w)->{generation++;clearDownloads();prefs.edit().clear().apply();uid=0;nonce="";name="";courses=new JSONArray();CookieManager.getInstance().removeAllCookies(done->{CookieManager.getInstance().flush();render();});WebStorage.getInstance().deleteAllData();}).setNegativeButton("انصراف",null).show()));body.addView(text("نسخه آزمایشی ۰٫۲ · آموزش همراه شیپا",12,muted));}
  void login(){sitePage(SITE+"/my-account/");}
  void sitePage(String url){generation++;LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setPadding(0,dp(28),0,dp(20));screen.addView(button("بازگشت به اپ / بررسی ورود",this::closeWeb));web=new WebView(this);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){Uri u=req.getUrl();if("https".equals(u.getScheme())&&"shipaa.ir".equals(u.getHost()))return false;if("https".equals(u.getScheme())||"mailto".equals(u.getScheme())||"tel".equals(u.getScheme()))try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}@Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError e){if(req.isForMainFrame())toast("صفحه باز نشد؛ اتصال اینترنت را بررسی کن");}});screen.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(screen);web.loadUrl(url);}
  void closeWeb(){if(web!=null){web.destroy();web=null;}CookieManager.getInstance().flush();render();session(true);}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override public void onBackPressed(){if(web!=null){closeWeb();return;}if(!tab.equals("home")){generation++;tab="home";render();return;}super.onBackPressed();}
  @Override protected void onResume(){super.onResume();handler.postDelayed(refreshDownloads,2000);}
  @Override protected void onPause(){handler.removeCallbacks(refreshDownloads);super.onPause();}
  @Override protected void onDestroy(){generation++;handler.removeCallbacksAndMessages(null);if(web!=null)web.destroy();io.shutdownNow();super.onDestroy();}
}
