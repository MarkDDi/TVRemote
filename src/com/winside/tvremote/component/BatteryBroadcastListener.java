package com.winside.tvremote.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

/**
 * Author        : lu
 * Data          : 2015/9/24
 * Time          : 9:37
 * Decription    :
 */
public class BatteryBroadcastListener {

    private Context mContext;
    private BatteryChangeListener listener;
    private BatteryChangeBroadcast batteryChangeBroadcast;

    public BatteryBroadcastListener(Context mContext) {
        this.mContext = mContext;
        batteryChangeBroadcast = new BatteryChangeBroadcast();
    }

    class BatteryChangeBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                listener.onBatteryChangeListener(intent);
            }
        }
    }

    /**
     *  注册电量更改广播
      * @param listener
     */
    public void begin(BatteryChangeListener listener) {
        this.listener = listener;
        registerBroadcast();
    }

    public void unregisterBroadcast() {
        mContext.unregisterReceiver(batteryChangeBroadcast);
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(batteryChangeBroadcast, filter);
    }


    public interface BatteryChangeListener {
        void onBatteryChangeListener(Intent intent);
    }

}
