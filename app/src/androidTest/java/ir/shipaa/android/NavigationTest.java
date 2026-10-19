package ir.shipaa.android;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class NavigationTest {
    @Test public void guestNavigationAndScreenshots() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context.startActivity(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        assertTrue(device.wait(Until.hasObject(By.text("یادگیری، آغاز حرکت")),15000));
        device.waitForIdle(); capture(context,"home");
        device.findObject(By.desc("کاوش")).click();
        assertTrue(device.wait(Until.hasObject(By.text("کاوش دوره‌ها")),5000));
        capture(context,"discover");
        device.findObject(By.desc("دانلودها")).click();
        assertTrue(device.wait(Until.hasObject(By.text("دانلودها")),5000));
        capture(context,"downloads");
        device.findObject(By.desc("یادگیری من")).click();
        assertTrue(device.wait(Until.hasObject(By.text("مسیرت را ادامه بده")),5000));
        device.findObject(By.desc("حساب من").clazz("android.widget.LinearLayout")).click();
        assertTrue(device.wait(Until.hasObject(By.text("ورود / ساخت حساب")),5000));
        capture(context,"profile");
    }
    void capture(Context c,String name) throws Exception {
        Bitmap full=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        Bitmap small=Bitmap.createScaledBitmap(full,390,full.getHeight()*390/full.getWidth(),true);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,88,bytes);
        String encoded=android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP);
        String command="sh -c 'mkdir -p /data/local/tmp/shipaa-ui; echo "+encoded+" | base64 -d > /data/local/tmp/shipaa-ui/"+name+".jpg'";
        try(android.os.ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);FileInputStream in=new FileInputStream(fd.getFileDescriptor())){while(in.read()!=-1){}}
        small.recycle();full.recycle();
    }
}
