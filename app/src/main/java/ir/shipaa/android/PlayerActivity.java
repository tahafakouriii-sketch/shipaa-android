package ir.shipaa.android;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
public class PlayerActivity extends Activity {
 ExoPlayer player; PlayerView video; String key; long position; boolean playing=true;
 @Override public void onCreate(Bundle saved){super.onCreate(saved);key=getIntent().getStringExtra("key");position=getSharedPreferences("learner",0).getLong("position:"+key,0);if(saved!=null)position=saved.getLong("position",position);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff101c16);root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});Button back=new Button(this);back.setText("بازگشت · "+getIntent().getStringExtra("title"));back.setOnClickListener(v->finish());root.addView(back);video=new PlayerView(this);root.addView(video,new LinearLayout.LayoutParams(-1,0,1));LinearLayout speeds=new LinearLayout(this);for(float f:new float[]{0.75f,1f,1.25f,1.5f,2f}){Button b=new Button(this);b.setText(f+"×");b.setOnClickListener(v->{if(player!=null)player.setPlaybackSpeed(f);});speeds.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(speeds);setContentView(root);}
 @Override protected void onStart(){super.onStart();player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();video.setPlayer(player);player.setMediaItem(MediaItem.fromUri(getIntent().getStringExtra("uri")));player.seekTo(position);player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){Toast.makeText(PlayerActivity.this,"پخش انجام نشد؛ فایل یا اتصال اینترنت را بررسی کن",Toast.LENGTH_LONG).show();}});player.prepare();player.setPlayWhenReady(playing);}
 @Override protected void onStop(){if(player!=null){position=player.getCurrentPosition();playing=player.getPlayWhenReady();getSharedPreferences("learner",0).edit().putLong("position:"+key,position).apply();video.setPlayer(null);player.release();player=null;}super.onStop();}
 @Override public void onSaveInstanceState(Bundle b){b.putLong("position",player==null?position:player.getCurrentPosition());super.onSaveInstanceState(b);}
}
