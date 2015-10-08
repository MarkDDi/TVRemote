package com.winside.tvremote.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.winside.tvremote.ConstValues;
import com.winside.tvremote.GameHandleActivity;
import com.winside.tvremote.R;
import com.winside.tvremote.util.EffectUtils;
import com.winside.tvremote.util.LogUtils;
import com.winside.tvremote.widget.GameTouchMode;

/**
 * Author        : lu
 * Data          : 2015/10/8
 * Time          : 16:13
 * Decription    :
 */
public class GameTouchFragment extends Fragment {

    private Context context;
    private GameTouchMode.MyGameGestureListener gameGestureListener;
    private GestureDetector detector;//手势识别
    private GameTouchMode gameTouchMode;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            gameGestureListener = (GameTouchMode.MyGameGestureListener) activity;
            context = activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " Activity必须实现ISwitchMode接口");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.handle_activity_mtouch, null);
        RelativeLayout game_mouse = (RelativeLayout) view.findViewById(R.id.game_mouse);
        gameTouchMode = new GameTouchMode(context, null);
        gameTouchMode.setGestureListener(gameGestureListener);

        // 动态加载触摸屏模式布局
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        gameTouchMode.setLayoutParams(params);
        gameTouchMode.setBackgroundResource(R.drawable.soft_pad);
        game_mouse.addView(gameTouchMode);

        // 游戏触摸屏模式的单击确定键事件
        detector = new GestureDetector(context, gameTouchMode);
        gameTouchMode.setDetector(detector);
        return view;
    }

    public View getTouchView() {
        return gameTouchMode;
    }
}
