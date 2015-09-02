package com.winside.tvremote.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.winside.tvremote.MainActivity;
import com.winside.tvremote.TouchHandler;

/**
 * Author        : lu
 * Data          : 2015/8/28
 * Time          : 17:29
 * Decription    :
 */
public class MouseFragment extends Fragment {

    private MainActivity mainActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(com.winside.tvremote.R.layout.subview_touchpad, null);

        new TouchHandler(view.findViewById(com.winside.tvremote.R.id.touch_pad), TouchHandler.Mode.POINTER_MULTITOUCH,
                mainActivity.getCommands());
        return view;
    }
}
