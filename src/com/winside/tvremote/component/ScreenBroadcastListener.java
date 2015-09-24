package com.winside.tvremote.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

/**
 * Author        : lu
 * Data          : 2015/9/23
 * Time          : 18:29
 * Decription    :
 */
public class ScreenBroadcastListener {

    private Context mContext;
    private ScreenStateListener mScreenStateListener;
    private ScreenBroadcastRecevier mScreenReceiver;

    public ScreenBroadcastListener(Context mContext) {
        this.mContext = mContext;
        mScreenReceiver = new ScreenBroadcastRecevier();
    }

    class ScreenBroadcastRecevier extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenStateListener.onScreenOff();
            }
        }
    }

    /**
     * 开始监听Screen状态
     * @param listener
     */
    public void begin(ScreenStateListener listener) {
        mScreenStateListener = listener;
        registerBroadcast();
        getScreenState();
    }

    /**
     * 获取Screen状态
     */
    private void getScreenState() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (pm.isScreenOn() && mScreenStateListener != null) {
                mScreenStateListener.onScreenOn();
        } else {
            if (mScreenStateListener != null) {
                mScreenStateListener.onScreenOff();
            }
        }
    }

    /**
     * 注册锁屏广播
     */
    public void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenReceiver, filter);
    }

    /**
     * 解除动态广播
     */
    public void unregisterBroadcast() {
        mContext.unregisterReceiver(mScreenReceiver);
    }

    /**
     * 回调接口
     */
    public interface ScreenStateListener {

        void onScreenOn();
        void onScreenOff();
    }
}
