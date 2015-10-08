package com.winside.tvremote.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.winside.tvremote.R;

/**
 * Author        : lu
 * Data          : 2015/10/8
 * Time          : 16:13
 * Decription    :
 */
public class GameHandleFragment extends Fragment implements View.OnTouchListener {

    public BtnListener btnListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            btnListener = (BtnListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " Activity必须实现BtnListener接口");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.handle_activity_remote_new, null);
        // A 键，确定键
        final ImageButton Btn_A = (ImageButton) view.findViewById(R.id.handle_a_key);
        // B键。同Y键
        final ImageButton Btn_B = (ImageButton) view.findViewById(R.id.handle_b_key);
        // D 键，校正对焦
        final ImageButton Btn_D = (ImageButton) view.findViewById(R.id.handle_d_key);
        // X 键，同D键
        final ImageButton Btn_X = (ImageButton) view.findViewById(R.id.handle_x_key);
        // Y 键，退出菜单键
        final ImageButton Btn_Y = (ImageButton) view.findViewById(R.id.handle_y_key);

        // 上下左右控制
        final ImageButton Btn_Up = (ImageButton) view.findViewById(R.id.handle_up_img_btn);
        final ImageButton Btn_Down = (ImageButton) view.findViewById(R.id.handle_down_img_btn);
        final ImageButton Btn_Left = (ImageButton) view.findViewById(R.id.handle_left_img_btn);
        final ImageButton Btn_Right = (ImageButton) view.findViewById(R.id.handle_right_img_btn);

        // 逻辑代码在onTouch()方法中
        Btn_A.setOnTouchListener(this);
        Btn_B.setOnTouchListener(this);
        Btn_D.setOnTouchListener(this);
        Btn_X.setOnTouchListener(this);
        Btn_Y.setOnTouchListener(this);

        Btn_Up.setOnTouchListener(this);
        Btn_Down.setOnTouchListener(this);
        Btn_Left.setOnTouchListener(this);
        Btn_Right.setOnTouchListener(this);

        return view;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        btnListener.TouchListener(v,event);
        return false;
    }

    public interface BtnListener {
        void TouchListener(View v, MotionEvent event);
    }
}
