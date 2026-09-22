package com.catcore.ctrlmietze.multitask.window;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONArray;

public final class WindowGuardBridge {
    private static final String PREFS="catcore_guard_plus";
    private static final String K_HEARTBEAT="workspace_heartbeat";
    private static final String K_ACTIVE="active";
    private static final String K_WINDOWS="windows";

    private WindowGuardBridge(){}

    public static void heartbeat(Context c,int windows){
        p(c).edit().putLong(K_HEARTBEAT,SystemClock.elapsedRealtime())
                .putBoolean(K_ACTIVE,windows>0).putInt(K_WINDOWS,Math.max(0,windows)).apply();
    }

    public static void publishGuardPlus(Context c,int windows,boolean active){
        p(c).edit().putBoolean(K_ACTIVE,active).putInt(K_WINDOWS,Math.max(0,windows)).apply();
    }

    public static long workspaceHeartbeatAgeMs(Context c,long now){
        long h=p(c).getLong(K_HEARTBEAT,0L);
        return h<=0?Long.MAX_VALUE:Math.max(0L,now-h);
    }

    public static boolean guardActive(Context c){return p(c).getBoolean(K_ACTIVE,false);}
    public static int protectedWindows(Context c){return p(c).getInt(K_WINDOWS,0);}

    public static int savedWindowCount(Context c){
        try{
            String raw=c.getSharedPreferences("multitask_v2_window_sessions",Context.MODE_PRIVATE)
                    .getString("sessions","[]");
            return new JSONArray(raw==null?"[]":raw).length();
        }catch(Throwable ignored){return 0;}
    }

    public static void requestWorkspaceRecovery(Context c){
        if(savedWindowCount(c)<=0)return;
        try{
            Intent i=new Intent(c,WindowHostActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            c.startActivity(i);
        }catch(Throwable ignored){}
    }

    private static SharedPreferences p(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
}
