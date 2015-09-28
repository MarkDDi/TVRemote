package com.winside.tvremote.util;

import android.content.Context;
import android.os.Vibrator;
import android.view.SoundEffectConstants;
import android.view.View;

/**
 * Author        : lu
 * Data          : 2015/9/28
 * Time          : 9:30
 * Decription    :
 */
public class EffectUtils {

    /**
     *  当点击一个按键时，发出一个音效
     * @param view
     */
    public static void playSoundClick(View view) {
        view.playSoundEffect(SoundEffectConstants.CLICK);
    }

    public static void playSoundUp(View view) {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
    }

    public static void playSoundDown(View view) {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
    }

    public static void playSoundLeft(View view) {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
    }

    public static void playSoundRight(View view) {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
    }

    /**
     * 触发手机振动
     * @param context
     * @param view
     * @param isVibrator
     */
    public static void triggerVibrator(Context context, View view, boolean isVibrator) {
       Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (isVibrator) {
            vibrator.vibrate(40);
            playSoundClick(view);
        }
    }
}
