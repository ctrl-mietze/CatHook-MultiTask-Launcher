package com.catcore.ctrlmietze.multitask;

import android.content.Context;
import android.os.SystemClock;

import com.catcore.ctrlmietze.multitask.window.WindowGuardBridge;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Stability Guard+ complements the pressure/recovery guard.
 * It watches CatCore-owned mirrored/window sessions and keeps the framework
 * heartbeat warm while those sessions exist. It never force-stops packages.
 */
final class FrameworkWindowGuard {
    interface StatusSink { void onStatus(String status); }

    private final Context context;
    private final StatusSink sink;
    private ScheduledExecutorService executor;
    private int staleStreak;

    FrameworkWindowGuard(Context context, StatusSink sink) {
        this.context=context.getApplicationContext();
        this.sink=sink;
    }

    void start(){
        if(executor!=null)return;
        executor=Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::check,5,8, TimeUnit.SECONDS);
    }

    void stop(){
        ScheduledExecutorService x=executor;executor=null;
        if(x!=null)x.shutdownNow();
    }

    private void check(){
        try{
            int windows=WindowGuardBridge.savedWindowCount(context);
            Set<String> managed=ManagedSessionRegistry.recent(context);
            if(windows<=0 && managed.isEmpty()){
                staleStreak=0;
                WindowGuardBridge.publishGuardPlus(context,0,false);
                return;
            }

            long now=SystemClock.elapsedRealtime();
            WindowGuardBridge.publishGuardPlus(context,windows,true);
            boolean stale=windows>0 && WindowGuardBridge.workspaceHeartbeatAgeMs(context,now)>25_000L;
            if(stale)staleStreak++;else staleStreak=0;

            if(staleStreak>=3){
                // Do not kill the app/task. Guard+ asks Android to recreate the
                // CatCore workspace shell from its persisted window sessions.
                WindowGuardBridge.requestWorkspaceRecovery(context);
                staleStreak=0;
                if(sink!=null)sink.onStatus("Stability Guard+ recovered the workspace host");
            }else if(sink!=null && windows>0){
                sink.onStatus("Framework active · Guard+ protecting "+windows+" live window"+(windows==1?"":"s"));
            }
        }catch(Throwable ignored){}
    }
}
