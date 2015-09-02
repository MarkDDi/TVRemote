package com.winside.tvremote.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.winside.tvremote.MainActivity;
import com.winside.tvremote.widget.SoftDpad;

/**
 * Author        : lu
 * Data          : 2015/8/28
 * Time          : 14:32
 * Decription    :
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SoftDpadFragment extends Fragment {

    private SoftDpad softDpad;
    private MainActivity mainActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
       mainActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(com.winside.tvremote.R.layout.softdpad, null);
        softDpad = ((SoftDpad) view.findViewById(com.winside.tvremote.R.id.SoftDpad));
        softDpad.setDpadListener(mainActivity.getDefaultDpadListener());
        return view;
    }
}
