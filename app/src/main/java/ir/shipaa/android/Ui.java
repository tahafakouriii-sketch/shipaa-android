package ir.shipaa.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared visual language: calm surfaces, strong hierarchy, 48dp targets. */
final class Ui {
    static final int INK=0xff213c34, MUTED=0xff677970, GREEN=0xff306c46, LIME=0xffd7ecaf;
    static final int BG=0xfff7f9f5, WHITE=0xffffffff, LINE=0xffe4eae1, LILAC=0xffefeafa, SKY=0xffe8f2fa;
    final Context c; final Typeface normal,bold;
    Ui(Context c){this.c=c;Typeface f;try{f=Typeface.createFromAsset(c.getAssets(),"fonts/Vazirmatn.ttf");}catch(Exception e){f=Typeface.create("sans-serif",Typeface.NORMAL);}normal=f;bold=Typeface.create(f,Typeface.BOLD);}
    int dp(float x){return Math.round(x*c.getResources().getDisplayMetrics().density);}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    android.graphics.drawable.Drawable ripple(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x18306c46),bg(color,radius),bg(WHITE,radius));}
    TextView text(String s,int size,int color,boolean strong){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(strong?bold:normal);v.setGravity(Gravity.START);v.setTextDirection(View.TEXT_DIRECTION_RTL);v.setLineSpacing(dp(2),1.08f);v.setIncludeFontPadding(false);return v;}
    LinearLayout col(){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void space(LinearLayout l,int h){View v=new View(c);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    LinearLayout panel(int color){LinearLayout l=col();l.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawable d=bg(color,24);if(color==WHITE)d.setStroke(dp(1),LINE);l.setBackground(d);return l;}
    TextView chip(String label,int color){TextView t=text(label,11,INK,false);t.setPadding(dp(10),dp(6),dp(10),dp(6));t.setBackground(bg(color,10));return t;}
    TextView action(String label,boolean primary,Runnable click){TextView t=text(label,14,primary?WHITE:GREEN,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(50));t.setPadding(dp(16),dp(12),dp(16),dp(12));t.setBackground(ripple(primary?GREEN:0xffedf3e7,16));t.setOnClickListener(v->click.run());t.setFocusable(true);return t;}
    View iconButton(String icon,String label,Runnable click){FrameLayout f=new FrameLayout(c);f.setContentDescription(label);f.setFocusable(true);f.setBackground(ripple(WHITE,16));Glyph g=new Glyph(c,icon,GREEN);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER);f.addView(g,p);f.setOnClickListener(v->click.run());f.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));return f;}
    static final class Glyph extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final String kind; final int color;
        Glyph(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void line(Canvas c,float... pts){Path path=new Path();path.moveTo(pts[0],pts[1]);for(int i=2;i<pts.length;i+=2)path.lineTo(pts[i],pts[i+1]);c.drawPath(path,p);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStrokeWidth(1.7f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(kind){
            case "home":line(canvas,3,10,12,3,21,10,21,21,3,21,3,10);line(canvas,9,21,9,14,15,14,15,21);break;
            case "book":canvas.drawRoundRect(4,3,20,21,3,3,p);line(canvas,8,3,8,21);line(canvas,12,8,16,8);line(canvas,12,12,16,12);break;
            case "compass":canvas.drawCircle(12,12,9,p);line(canvas,16,8,14,14,8,16,10,10,16,8);break;
            case "download":line(canvas,12,3,12,15);line(canvas,7,10,12,15,17,10);line(canvas,4,16,4,21,20,21,20,16);break;
            case "user":canvas.drawCircle(12,7,4,p);canvas.drawArc(4,13,20,27,180,180,false,p);break;
            case "play":line(canvas,8,4,20,12,8,20,8,4);break;
            case "arrow":line(canvas,9,5,16,12,9,19);break;
            case "search":canvas.drawCircle(10,10,6,p);line(canvas,15,15,21,21);break;
            case "refresh":canvas.drawArc(4,4,20,20,40,285,false,p);line(canvas,16,3,21,6,17,10);break;
            case "clock":canvas.drawCircle(12,12,9,p);line(canvas,12,6,12,12,16,14);break;
            case "check":line(canvas,5,12,10,17,20,6);break;
            case "trash":line(canvas,4,6,20,6);line(canvas,7,6,8,21,16,21,17,6);line(canvas,9,6,9,3,15,3,15,6);break;
            case "lock":canvas.drawRoundRect(5,10,19,21,2,2,p);canvas.drawArc(8,2,16,14,180,180,false,p);break;
            case "order":canvas.drawRoundRect(5,3,19,21,2,2,p);line(canvas,9,8,15,8);line(canvas,9,12,15,12);line(canvas,9,16,13,16);break;
            case "help":canvas.drawCircle(12,12,9,p);canvas.drawArc(9,6,15,12,180,250,false,p);line(canvas,12,12,12,14);canvas.drawPoint(12,18,p);break;
            case "wifi":canvas.drawArc(2,4,22,24,225,90,false,p);canvas.drawArc(6,9,18,21,225,90,false,p);canvas.drawPoint(12,18,p);break;
            default:line(canvas,12,21,12,10);canvas.drawOval(3,4,12,13,p);canvas.drawOval(12,2,21,11,p);
            }canvas.restore();
        }
    }
    static final class GrowthArt extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        GrowthArt(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(0x2276a57e);for(int i=0;i<5;i++)c.drawCircle(w*.05f,h*.6f,45+i*28,p);p.setStyle(Paint.Style.FILL);p.setColor(0xffcbe59f);c.drawCircle(w*.2f,h*.38f,10,p);p.setColor(0xffa9c1a0);c.drawCircle(w*.42f,h*.72f,5,p);}
    }
}
