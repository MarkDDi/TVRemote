package com.google.android.apps.tvremote.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.apps.tvremote.R;
import com.google.android.apps.tvremote.util.PromptManager;

/**
 * Author        : lu
 * Data          : 2015/8/31
 * Time          : 13:55
 * Decription    :
 */
public class HomeFragment extends Fragment implements View.OnFocusChangeListener {

    private TextView gesture;
    private TextView mouse;
    private ISwitchMode iSwitchMode = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            iSwitchMode = (ISwitchMode) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " Activity必须实现ISwitchMode接口");
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home, null);
        gesture = ((TextView) view.findViewById(R.id.gesture));
        mouse = ((TextView) view.findViewById(R.id.mouse));

        initListener();

        return view;
    }

    private void initListener() {
        gesture.setOnFocusChangeListener(this);
        mouse.setOnFocusChangeListener(this);
    }


    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            switch (v.getId()) {
                case R.id.gesture:
                   iSwitchMode.switchSoftFragment();
                    break;
                case R.id.mouse:
                    iSwitchMode.switchMouseFragment();
                    //                    getFragmentManager().beginTransaction().replace(R.id.container,
                    //                            mouseFragment).commit();
                    break;
                default:

                    break;
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        iSwitchMode = null;
    }

    public interface ISwitchMode {
        public void switchSoftFragment();

        public void switchMouseFragment();
    }
}
