package ir.shipaa.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** A fully native offline entry point; never requires loading a website. */
public final class OfflineDownloadsActivity extends Activity {
  private NativeDownloadStore downloads;
  private LinearLayout list;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private boolean active;
  private final int GREEN=0xff398d55, INK=0xff233a31, MUTED=0xff65786f;
  private final Runnable tick=new Runnable(){
    @Override public void run(){ if(active){renderItems();handler.postDelayed(this,2000);} }
  };
  private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value+.5f);}
  private GradientDrawable bg(int color,int radius){
    GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
  }
  private TextView text(String value,int size,int color,boolean strong){
    TextView t=new TextView(this);
    t.setText(value);t.setTextSize(size);t.setTextColor(color);
    t.setTextDirection(View.TEXT_DIRECTION_RTL);
    t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    if(strong)t.setTypeface(null,Typeface.BOLD);
    return t;
  }
  private Button action(String label,Runnable run){
    Button b=new Button(this);b.setAllCaps(false);b.setText(label);
    b.setTextSize(13);b.setTextColor(GREEN);b.setBackground(bg(0xffeaf6e8,15));
    b.setOnClickListener(v->run.run());
    b.setMinHeight(dp(46));
    return b;
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(0xfff7faf8);
    getWindow().setNavigationBarColor(Color.WHITE);
    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    downloads=new NativeDownloadStore(this);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    root.setPadding(dp(16),dp(14),dp(16),dp(10));root.setBackgroundColor(0xfff7faf8);
    LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
    header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    TextView heading=text("ویدئوهای ذخیره‌شده",22,INK,true);
    header.addView(heading,new LinearLayout.LayoutParams(0,dp(55),1));
    header.addView(action("بازگشت",this::finish));
    root.addView(header);
    TextView hint=text("فایل‌های کامل‌شده حتی بدون اینترنت در همین بخش باز می‌شوند.",13,MUTED,false);
    hint.setPadding(0,dp(3),0,dp(16));root.addView(hint);
    ScrollView scroll=new ScrollView(this);
    list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
    list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    scroll.addView(list);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(root);
  }
  private void renderItems(){
    list.removeAllViews();
    JSONArray rows=downloads.all();
    if(rows.length()==0){
      TextView empty=text("هنوز ویدئویی ذخیره نشده. وارد یکی از جلسات خانه یادگیری شو و «ذخیره آفلاین» را انتخاب کن.",15,MUTED,false);
      empty.setPadding(dp(12),dp(38),dp(12),0);list.addView(empty);return;
    }
    for(int i=rows.length()-1;i>=0;i--){
      JSONObject row=rows.optJSONObject(i);if(row==null)continue;
      long id=row.optLong("id",0);
      if(id<=0)continue;
      NativeDownloadStore.Status s=downloads.status(id);
      LinearLayout card=new LinearLayout(this);
      card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(14));
      card.setBackground(bg(Color.WHITE,20));
      LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(12);
      list.addView(card,cp);
      TextView title=text(row.optString("title","ویدئوی آموزشی"),16,INK,true);
      card.addView(title);
      String state;
      if(s.completed())state="آماده تماشای آفلاین";
      else if(s.state==DownloadManager.STATUS_FAILED)state="دانلود ناموفق؛ حذف و دوباره امتحان کن";
      else if(s.state==DownloadManager.STATUS_PAUSED)state="دانلود متوقف است؛ در انتظار شبکه";
      else if(s.state==DownloadManager.STATUS_PENDING)state="در صف دانلود";
      else state="در حال دانلود";
      String progress=s.percent()>=0?" • "+s.percent()+"٪":"";
      String size=" • "+Math.round(s.bytes/1048576.0)+" مگابایت دریافت‌شده";
      TextView sub=text(state+progress+size,12,s.completed()?GREEN:MUTED,false);
      LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(10);
      card.addView(sub,sp);
      LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
      actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
      LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(12);
      card.addView(actions,ap);
      if(s.completed()) {
        Button play=action("▶ پخش",()->{
          Uri uri=downloads.completedUri(id);
          if(uri==null)return;
          Intent intent=new Intent(this,PlayerActivity.class);
          intent.putExtra("uri",uri.toString());
          intent.putExtra("key","offline:"+id);
          intent.putExtra("title",row.optString("title"));
          startActivity(intent);
        });
        actions.addView(play,new LinearLayout.LayoutParams(0,dp(46),1));
      }
      Button remove=action("حذف",()->new AlertDialog.Builder(this)
          .setTitle("حذف ویدئو")
          .setMessage("فایل ذخیره‌شده از این دستگاه حذف شود؟")
          .setPositiveButton("حذف", (d,w)->{downloads.delete(id);renderItems();})
          .setNegativeButton("انصراف",null).show());
      LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(46),1);
      rp.rightMargin=dp(8);actions.addView(remove,rp);
    }
  }
  @Override protected void onResume(){super.onResume();active=true;tick.run();}
  @Override protected void onPause(){active=false;handler.removeCallbacks(tick);super.onPause();}
}
