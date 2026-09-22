package com.catcore.ctrlmietze.multitask;

import android.app.Dialog;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class CatDialog {
    private CatDialog() {}

    public static Dialog show(Activity context, String eyebrow, String title, String message,
                              String negative, String positive, Runnable onPositive) {
        Dialog d = base(context);
        LinearLayout card = card(context);
        if (eyebrow != null && !eyebrow.isEmpty()) {
            TextView e = CatUi.text(context, eyebrow.toUpperCase(), 11, Color.rgb(150,166,220), true);
            e.setLetterSpacing(.12f);
            card.addView(e);
        }
        TextView t = CatUi.text(context, title, 24, CatUi.TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(context,6);
        card.addView(t,tp);
        if (message != null && !message.isEmpty()) {
            TextView m = CatUi.text(context,message,13,CatUi.MUTED,false);
            LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2); mp.topMargin=dp(context,10);
            card.addView(m,mp);
        }
        LinearLayout actions=new LinearLayout(context);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(context,50)); ap.topMargin=dp(context,18);
        card.addView(actions,ap);
        if (negative != null) {
            Button n=CatUi.secondaryButton(context,negative);
            actions.addView(n,new LinearLayout.LayoutParams(0,-1,1));
            n.setOnClickListener(v->d.dismiss());
        }
        if (positive != null) {
            Button p=CatUi.primaryButton(context,positive);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,1);
            if(negative!=null) pp.leftMargin=dp(context,8);
            actions.addView(p,pp);
            p.setOnClickListener(v->{ d.dismiss(); if(onPositive!=null) onPositive.run(); });
        }
        d.setContentView(card);
        size(d);
        d.show();
        size(d);
        return d;
    }

    public static Dialog selector(Activity context, String title, String[] labels, int selected,
                                  Choice choice) {
        Dialog d=base(context);
        LinearLayout card=card(context);
        TextView eyebrow=CatUi.text(context,"CATCORE · SELECT",11,Color.rgb(150,166,220),true);
        eyebrow.setLetterSpacing(.12f); card.addView(eyebrow);
        TextView heading=CatUi.text(context,title,24,CatUi.TEXT,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(context,6);
        card.addView(heading,hp);
        for(int i=0;i<labels.length;i++){
            final int index=i;
            LinearLayout row=new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(context,16),0,dp(context,14),0);
            row.setBackground(CatUi.stroke(context,
                    i==selected?Color.rgb(31,40,76):CatUi.SURFACE_2,18,
                    i==selected?CatUi.ACCENT:Color.rgb(45,53,75)));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(context,64)); rp.topMargin=dp(context,10);
            card.addView(row,rp);
            TextView label=CatUi.text(context,labels[i],15,CatUi.TEXT,true);
            row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(CatUi.pill(context,i==selected?"ACTIVE":"SELECT",
                    i==selected?Color.rgb(57,77,163):Color.rgb(45,53,75)));
            row.setOnClickListener(v->{d.dismiss(); if(choice!=null) choice.onChoice(index);});
            CatUi.pressScale(row);
        }
        Button cancel=CatUi.secondaryButton(context,"Cancel");
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(context,48)); cp.topMargin=dp(context,14);
        card.addView(cancel,cp); cancel.setOnClickListener(v->d.dismiss());
        d.setContentView(card); size(d); d.show(); size(d); return d;
    }

    private static Dialog base(Activity c){
        Dialog d=new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w=d.getWindow();
        if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
        return d;
    }
    private static LinearLayout card(Activity c){
        LinearLayout x=CatUi.card(c); x.setPadding(dp(c,22),dp(c,22),dp(c,22),dp(c,20));
        x.setBackground(CatUi.stroke(c,Color.rgb(17,22,39),28,Color.rgb(64,77,126))); return x;
    }
    private static void size(Dialog d){ Window w=d.getWindow(); if(w!=null){w.setLayout((int)(d.getContext().getResources().getDisplayMetrics().widthPixels*.90f),WindowManager.LayoutParams.WRAP_CONTENT);WindowManager.LayoutParams a=w.getAttributes();a.dimAmount=.72f;w.setAttributes(a);} }
    private static int dp(Context c,int v){return CatUi.dp(c,v);}
    // dev7 build marker: Activity-backed CatUi modal
    public interface Choice { void onChoice(int index); }
}
