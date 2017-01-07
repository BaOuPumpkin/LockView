package com.teddy.momoya.mixcameralockview;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by AU on 2017/1/7.
 */

public class CheckScreenOff extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Toast toast;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(toast == null)
            toast.makeText(getApplicationContext(),"OnStartCommand",Toast.LENGTH_SHORT).show();
        else
            toast.setText("OnStartCommand");
        IntentFilter ScreenOff = new IntentFilter();
        ScreenOff.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(ScreenOffReciver,ScreenOff);
        return super.onStartCommand(intent, START_FLAG_REDELIVERY, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(toast == null)
            toast.makeText(getApplicationContext(),"OnDestroy",Toast.LENGTH_SHORT).show();
        else
            toast.setText("OnDestroy");

    }

    private BroadcastReceiver ScreenOffReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                if(toast == null)
                    toast.makeText(getApplicationContext(),"OnRiceive",Toast.LENGTH_SHORT).show();
                else
                    toast.setText("OnRiceive");
                Intent Lock = new Intent(context,MainActivity.class);
                Lock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(Lock);
            }
        }
    };
}
