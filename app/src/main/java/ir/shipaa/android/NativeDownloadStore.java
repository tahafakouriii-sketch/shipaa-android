package ir.shipaa.android;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Native download records. All records are local to this app, not publicly indexed. */
public final class NativeDownloadStore {
  private static final String PREFS = "shipaa_native_offline_v1";
  private static final String RECORDS = "records";
  private final Context context;
  private final SharedPreferences prefs;
  private final DownloadManager manager;
  public NativeDownloadStore(Context context) {
    this.context = context.getApplicationContext();
    this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    this.manager = (DownloadManager)this.context.getSystemService(Context.DOWNLOAD_SERVICE);
  }
  public static boolean validVideo(String input) {
    if (input == null) return false;
    Uri uri = Uri.parse(input);
    String host = uri.getHost(), path = uri.getPath();
    return "https".equalsIgnoreCase(uri.getScheme())
        && host != null && host.equalsIgnoreCase("dl.shipaa.ir")
        && uri.getUserInfo() == null && uri.getPort() == -1
        && path != null && path.toLowerCase(Locale.ROOT).endsWith(".mp4");
  }
  public synchronized JSONArray all() {
    try { return new JSONArray(prefs.getString(RECORDS, "[]")); }
    catch (Exception ignored) { return new JSONArray(); }
  }
  public synchronized long find(String url) {
    JSONArray list = all();
    for (int i=0; i<list.length(); i++) {
      JSONObject row = list.optJSONObject(i);
      if (row != null && url.equals(row.optString("url"))) return row.optLong("id", -1);
    }
    return -1;
  }
  public long freeBytes() {
    java.io.File dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
    if (dir == null) return 0;
    return new StatFs(dir.getAbsolutePath()).getAvailableBytes();
  }
  public synchronized long enqueue(String url, String title, boolean wifiOnly) throws Exception {
    if (!validVideo(url)) throw new IllegalArgumentException("Unsupported video source");
    long existing = find(url);
    if (existing > 0) return existing;
    if (freeBytes() < 150L*1024*1024) throw new IllegalStateException("Low storage");
    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
    req.setAllowedOverMetered(!wifiOnly);
    req.setAllowedOverRoaming(false);
    req.setMimeType("video/mp4");
    req.setTitle(title.length()>90 ? title.substring(0,90):title);
    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
    req.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES,
        "shipaa-offline-" + System.currentTimeMillis() + ".mp4");
    long id = manager.enqueue(req);
    JSONArray list = all();
    JSONObject record = new JSONObject();
    record.put("id",id);
    record.put("title",title);
    record.put("url",url);
    record.put("created",System.currentTimeMillis());
    list.put(record);
    prefs.edit().putString(RECORDS,list.toString()).apply();
    return id;
  }
  public static final class Status {
    public final int state;
    public final long bytes, total;
    Status(int state,long bytes,long total) { this.state=state;this.bytes=bytes;this.total=total; }
    public boolean completed() { return state==DownloadManager.STATUS_SUCCESSFUL; }
    public int percent() { return total>0 ? (int)Math.min(100,100*bytes/total):-1; }
  }
  public Status status(long id) {
    try(Cursor c=manager.query(new DownloadManager.Query().setFilterById(id))) {
      if(c==null || !c.moveToFirst()) return new Status(DownloadManager.STATUS_FAILED,0,0);
      return new Status(
        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
        c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
        c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
    }
  }
  public Uri completedUri(long id) {
    Status s=status(id);
    return s.completed()?manager.getUriForDownloadedFile(id):null;
  }
  public synchronized void delete(long id) {
    manager.remove(id);
    JSONArray source=all(), retained=new JSONArray();
    for(int i=0;i<source.length();i++){
      JSONObject row=source.optJSONObject(i);
      if(row!=null && row.optLong("id")!=id) retained.put(row);
    }
    prefs.edit().putString(RECORDS,retained.toString()).apply();
  }
  public synchronized void deleteAll() {
    JSONArray source=all();
    for(int i=0;i<source.length();i++){
      JSONObject row=source.optJSONObject(i);
      if(row!=null) manager.remove(row.optLong("id"));
    }
    prefs.edit().remove(RECORDS).apply();
  }
}
