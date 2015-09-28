package com.winside.tvremote.util;

import android.view.SoundEffectConstants;
import android.view.View;

/**
 * Author        : lu
 * Data          : 2015/9/28
 * Time          : 9:30
 * Decription    :
 */
public class FeatureEffectUtils {

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
}
