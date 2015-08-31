package com.google.android.apps.tvremote.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.apps.tvremote.MainActivity;
import com.google.android.apps.tvremote.R;
import com.google.android.apps.tvremote.TouchHandler;

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

        View view = inflater.inflate(R.layout.subview_touchpad, null);

        new TouchHandler(view.findViewById(R.id.touch_pad), TouchHandler.Mode.POINTER_MULTITOUCH,
                mainActivity.getCommands());
        return view;
    }
}
