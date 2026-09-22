package com.catcore.ctrlmietze.multitask;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public final class LegacyHomeActivity extends AppCompatActivity {
    private LinearLayout appsHost;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state); CatUi.applyWindow(this);
        SettingsStore.setStartMode(this, SettingsStore.MODE_APP_OWN_TASK);
        if(SettingsStore.frameworkEnabled(this)) try{CatCoreFrameworkService.start(this);}catch(Throwable ignored){}
        build();
        List<AppEntry> cached=AppCatalog.loadCached(this);
        render(cached);
        if(cached.isEmpty()||AppCatalog.needsRefresh(this))
            AppCatalog.refreshAsync(this,true,this::render);
        else if(SettingsStore.frameworkEnabled(this))
            CatCoreFrameworkService.requestCatalogRefresh(this);
    }

    @Override protected void onResume(){
        super.onResume();
        if(!SettingsStore.legacyEasyMode(this)){finish();startActivity(new Intent(this,MainActivity.class));return;}
        List<AppEntry> cached=AppCatalog.loadCached(this);if(!cached.isEmpty())render(cached);
    }

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(28),dp(16),dp(18));root.setBackground(CatUi.background());

        LinearLayout hero=CatUi.card(this);hero.setBackground(CatUi.hero(this));root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        LinearLayout text=new LinearLayout(this);text.setOrientation(LinearLayout.VERTICAL);top.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView k=CatUi.text(this,"CATCORE · EASY",11,Color.rgb(165,177,255),true);k.setLetterSpacing(.11f);text.addView(k);
        text.addView(CatUi.text(this,"MultiTask",27,CatUi.TEXT,true));
        text.addView(CatUi.text(this,"Classic app-first workflow · modern V2 own-task backend",12,Color.rgb(205,213,235),false));
        top.addView(CatUi.pill(this,"DEV8",Color.rgb(56,65,128)),new LinearLayout.LayoutParams(dp(70),dp(34)));

        LinearLayout status=new LinearLayout(this);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,dp(34));stp.topMargin=dp(12);hero.addView(status,stp);
        status.addView(CatUi.pill(this,"OWN TASK",Color.rgb(29,78,62)),new LinearLayout.LayoutParams(0,-1,1));
        TextView fw=CatUi.pill(this,SettingsStore.frameworkEnabled(this)?"FRAMEWORK LITE":"FRAMEWORK OFF",SettingsStore.frameworkEnabled(this)?Color.rgb(29,78,62):Color.rgb(88,67,28));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-1,1);fp.leftMargin=dp(7);status.addView(fw,fp);

        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(52));bp.topMargin=dp(12);root.addView(bar,bp);
        TextView title=CatUi.text(this,"Apps",20,CatUi.TEXT,true);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button tasks=CatUi.secondaryButton(this,"Tasks");bar.addView(tasks,new LinearLayout.LayoutParams(dp(86),-1));tasks.setOnClickListener(v->startActivity(new Intent(this,TaskManagerActivity.class)));
        Button settings=CatUi.secondaryButton(this,"Settings");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(96),-1);sp.leftMargin=dp(7);bar.addView(settings,sp);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        appsHost=new LinearLayout(this);appsHost.setOrientation(LinearLayout.VERTICAL);appsHost.setPadding(0,dp(4),0,dp(22));scroll.addView(appsHost,new ScrollView.LayoutParams(-1,-2));
        setContentView(root);
    }

    private void render(List<AppEntry> apps){
        if(appsHost==null||apps==null)return;appsHost.removeAllViews();
        for(AppEntry app:apps){
            LinearLayout card=CatUi.card(this);LinearLayout.LayoutParams cp=CatUi.cardParams(this);cp.topMargin=dp(6);appsHost.addView(card,cp);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row);
            ImageView icon=new ImageView(this);Drawable d=app.icon;try{if(d==null)d=getPackageManager().getApplicationIcon(app.packageName);}catch(Throwable ignored){}if(d!=null)icon.setImageDrawable(d);row.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(12);row.addView(labels,lp);
            labels.addView(CatUi.text(this,app.label,15,CatUi.TEXT,true));TextView pkg=CatUi.text(this,app.packageName,10,CatUi.MUTED,false);pkg.setSingleLine(true);labels.addView(pkg);
            TextView open=CatUi.pill(this,"START",Color.rgb(61,75,145));row.addView(open,new LinearLayout.LayoutParams(dp(72),dp(40)));
            android.view.View.OnClickListener launch=v->launch(app);card.setOnClickListener(launch);open.setOnClickListener(launch);CatUi.pressScale(card);
        }
    }

    private void launch(AppEntry app){
        ManagedSessionRegistry.touch(this,app.packageName);
        SystemTaskBridge.ensureTaskCount(this,app.packageName,app.activityName,1,(ok,before,after,target,message)->{
            if(!ok&&SettingsStore.compatibilityMode(this)){
                TaskLauncher.launchNewTask(this,app.packageName,app.activityName,null,(fallbackOk,fallbackMessage)->
                        Toast.makeText(this,fallbackOk?"Opened "+app.label:fallbackMessage,Toast.LENGTH_LONG).show());
            }else Toast.makeText(this,ok?"Opened "+app.label:message,Toast.LENGTH_LONG).show();
        });
    }
    private int dp(int v){return CatUi.dp(this,v);}
}
