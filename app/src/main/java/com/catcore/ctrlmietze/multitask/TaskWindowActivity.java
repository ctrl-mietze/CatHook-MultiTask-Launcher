package com.catcore.ctrlmietze.multitask;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public final class TaskWindowActivity extends AppCompatActivity {
    public static final String EXTRA_PACKAGE="package";
    private String pkg;
    private LinearLayout host;
    private final java.util.concurrent.ExecutorService exec=Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); CatUi.applyWindow(this);
        pkg=getIntent().getStringExtra(EXTRA_PACKAGE);
        if(pkg==null||pkg.isEmpty()){finish();return;}
        build(); refresh();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setBackground(CatUi.background());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(34));scroll.addView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
        Button back=CatUi.secondaryButton(this,"‹");header.addView(back,new LinearLayout.LayoutParams(dp(46),dp(46)));back.setOnClickListener(v->finish());
        ImageView icon=new ImageView(this);try{Drawable d=getPackageManager().getApplicationIcon(pkg);icon.setImageDrawable(d);}catch(Throwable ignored){}
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(48),dp(48));ip.leftMargin=dp(12);header.addView(icon,ip);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(12);header.addView(titles,tp);
        String label=pkg;try{CharSequence x=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0));if(x!=null)label=x.toString();}catch(Throwable ignored){}
        titles.addView(CatUi.text(this,label,25,CatUi.TEXT,true));titles.addView(CatUi.text(this,pkg,10,CatUi.MUTED,false));
        host=new LinearLayout(this);host.setOrientation(LinearLayout.VERTICAL);root.addView(host);
        setContentView(scroll);
        scroll.setAlpha(0f);scroll.setTranslationY(dp(16));scroll.animate().alpha(1f).translationY(0).setDuration(240).start();
    }

    private void refresh(){
        exec.execute(()->{
            List<TaskInspector.TaskInfo> all=TaskInspector.readUserTasks(this),mine=new ArrayList<>();
            for(TaskInspector.TaskInfo t:all)if(pkg.equals(t.packageName))mine.add(t);
            runOnUiThread(()->render(mine));
        });
    }

    private void render(List<TaskInspector.TaskInfo> tasks){
        host.removeAllViews();
        LinearLayout hero=CatUi.card(this);hero.setBackground(CatUi.hero(this));LinearLayout.LayoutParams hp=CatUi.cardParams(this);hp.topMargin=dp(18);host.addView(hero,hp);
        hero.addView(CatUi.text(this,"LIVE TASK WINDOW",11,Color.rgb(174,190,235),true));
        hero.addView(CatUi.text(this,tasks.size()+" active task"+(tasks.size()==1?"":"s"),24,CatUi.TEXT,true));
        for(TaskInspector.TaskInfo t:tasks){
            LinearLayout row=CatUi.card(this);LinearLayout.LayoutParams rp=CatUi.cardParams(this);rp.topMargin=dp(9);host.addView(row,rp);
            row.addView(CatUi.text(this,"Task "+t.taskId,16,CatUi.TEXT,true));
            row.addView(CatUi.text(this,"Display "+t.displayId+" · "+String.format(Locale.US,"%.1f%% CPU",t.cpuPercent)+" · "+format(t.rssBytes)+" RAM",12,CatUi.MUTED,false));
        }
        Button close=CatUi.secondaryButton(this,"Close duplicate tasks");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(50));cp.topMargin=dp(12);host.addView(close,cp);
        close.setOnClickListener(v->closeExtras(tasks));
        Button stop=CatUi.secondaryButton(this,"Force stop app");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(50));sp.topMargin=dp(8);host.addView(stop,sp);
        stop.setOnClickListener(v->CatDialog.show(this,"TASK CONTROL","Force stop app?","Stops the package and all of its current tasks.","Cancel","Force stop",()->exec.execute(()->{RootShell.Result r=RootShell.run("am force-stop --user current "+RootShell.quote(pkg),6);runOnUiThread(()->{Toast.makeText(this,r.ok?"App stopped":RootShell.shortReason(r),Toast.LENGTH_LONG).show();refresh();});})));
    }

    private void closeExtras(List<TaskInspector.TaskInfo> tasks){
        if(tasks.size()<=1){Toast.makeText(this,"No duplicate tasks.",Toast.LENGTH_SHORT).show();return;}
        exec.execute(()->{int keep=tasks.get(0).taskId;for(TaskInspector.TaskInfo t:tasks)if(t.displayId==0){keep=t.taskId;break;}for(TaskInspector.TaskInfo t:tasks)if(t.taskId!=keep)TaskInspector.removeTask(t.taskId);runOnUiThread(this::refresh);});
    }
    private String format(long b){if(b<=0)return"0 MB";return String.format(Locale.US,"%.0f MB",b/1048576d);}
    private int dp(int v){return CatUi.dp(this,v);}
}
