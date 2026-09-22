package com.catcore.ctrlmietze.multitask;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public final class FrameworkViewerActivity extends AppCompatActivity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService exec=Executors.newSingleThreadExecutor();
    private LinearLayout metrics;
    private TextView state;
    private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,2500L);}};

    @Override protected void onCreate(Bundle b){super.onCreate(b);CatUi.applyWindow(this);build();}
    @Override protected void onResume(){super.onResume();handler.post(tick);}
    @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}

    private void build(){
        ScrollView scroll=new ScrollView(this); scroll.setBackground(CatUi.background());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(34));scroll.addView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
        Button back=CatUi.secondaryButton(this,"‹");header.addView(back,new LinearLayout.LayoutParams(dp(46),dp(46)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(12);header.addView(titles,tp);
        titles.addView(CatUi.text(this,"Framework Viewer",27,CatUi.TEXT,true));
        titles.addView(CatUi.text(this,"Live CatCore runtime health",12,CatUi.MUTED,false));

        LinearLayout hero=CatUi.card(this);hero.setBackground(CatUi.hero(this));LinearLayout.LayoutParams hp=CatUi.cardParams(this);hp.topMargin=dp(18);root.addView(hero,hp);
        hero.addView(CatUi.text(this,"CATCORE FRAMEWORK",11,Color.rgb(174,190,235),true));
        state=CatUi.text(this,"Reading framework…",22,CatUi.TEXT,true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(8);hero.addView(state,sp);
        TextView h=CatUi.text(this,"Dedicated :framework process · app catalog · managed sessions · Stability Guard",12,Color.rgb(198,207,231),false);LinearLayout.LayoutParams hhp=new LinearLayout.LayoutParams(-1,-2);hhp.topMargin=dp(7);hero.addView(h,hhp);

        metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.VERTICAL);root.addView(metrics);
        setContentView(scroll);
    }

    private void refresh(){
        exec.execute(()->{
            List<TaskInspector.TaskInfo> tasks=TaskInspector.readUserTasks(this);
            Set<String> apps=new HashSet<>();int managed=0;long ram=0;
            Set<String> registry=ManagedSessionRegistry.recent(this);
            for(TaskInspector.TaskInfo t:tasks){apps.add(t.packageName);ram+=t.rssBytes;if(registry.contains(t.packageName)||t.displayId!=0)managed++;}
            boolean enabled=SettingsStore.frameworkEnabled(this);
            String text=enabled?"Framework online · healthy":"Framework disabled";
            int finalManaged=managed, taskCount=tasks.size(), appCount=apps.size(); long finalRam=ram;
            runOnUiThread(()->render(text,finalManaged,taskCount,appCount,finalRam,registry.size()));
        });
    }

    private void render(String s,int managed,int tasks,int apps,long ram,int registered){
        state.setText(s);metrics.removeAllViews();
        addMetric("MANAGED TASKS",String.valueOf(managed),"Tasks currently associated with CatCore sessions.");
        addMetric("VISIBLE TASKS",String.valueOf(tasks),apps+" user apps currently represented.");
        addMetric("SESSION REGISTRY",String.valueOf(registered),"Recent packages tracked by Stability Guard.");
        addMetric("TASK RAM",String.format(Locale.US,"%.1f GB",ram/1073741824d),"Resident memory reported by the live task snapshot.");
        addMetric("APP CATALOG",String.valueOf(AppCatalog.loadCached(this).size()),"Cached launcher entries; refreshed by the framework.");
        addMetric("STABILITY GUARD",SettingsStore.frameworkEnabled(this)?"ACTIVE":"OFF","Grace-period recovery remains conservative and preserves primary tasks.");
    }

    private void addMetric(String label,String value,String detail){
        LinearLayout card=CatUi.card(this);LinearLayout.LayoutParams p=CatUi.cardParams(this);p.topMargin=dp(10);metrics.addView(card,p);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row);
        row.addView(CatUi.text(this,label,12,Color.rgb(142,158,205),true),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(CatUi.pill(this,value,Color.rgb(48,62,116)));
        TextView d=CatUi.text(this,detail,12,CatUi.MUTED,false);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(-1,-2);dpv.topMargin=dp(8);card.addView(d,dpv);
    }
    private int dp(int v){return CatUi.dp(this,v);}
}
