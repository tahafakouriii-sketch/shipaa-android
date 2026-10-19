package ir.shipaa.android;
import android.os.*;
import android.webkit.CookieManager;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
final class Api {
    static final String SITE="https://shipaa.ir";
    final ExecutorService pool=Executors.newFixedThreadPool(3);final Handler main=new Handler(Looper.getMainLooper());
    String nonce=""; boolean closed;
    interface Done{void accept(JSONObject data,int status,String error);}
    void get(String path,Done done){request(path,null,done);}
    void post(String path,JSONObject body,Done done){request(path,body,done);}
    void request(String path,JSONObject body,Done done){
        final String cookie=CookieManager.getInstance().getCookie(SITE),token=nonce;
        pool.execute(()->{JSONObject data=null;int status=0;String error="اتصال برقرار نشد. دوباره تلاش کن.";HttpURLConnection c=null;
            try{c=(HttpURLConnection)new URL(SITE+path).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setRequestProperty("Accept","application/json");
                if(cookie!=null)c.setRequestProperty("Cookie",cookie);if(!token.isEmpty())c.setRequestProperty("X-WP-Nonce",token);
                if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}}
                status=c.getResponseCode();InputStream stream=status>=400?c.getErrorStream():c.getInputStream();if(stream==null)throw new IOException();
                try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>3*1024*1024)throw new IOException();}data=new JSONObject(out.toString("UTF-8"));}
                if(status>=200&&status<300)error="";else error=data.optString("message",status==401?"برای ادامه وارد حساب شو.":"دریافت اطلاعات انجام نشد.");
            }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
            final JSONObject result=data;final int code=status;final String message=error;main.post(()->{if(!closed)done.accept(result,code,message);});
        });
    }
    void close(){closed=true;pool.shutdownNow();main.removeCallbacksAndMessages(null);}
}
