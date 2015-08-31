package com.google.android.apps.tvremote.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.apps.tvremote.R;

/**
 * Author        : lu
 * Data          : 2015/8/31
 * Time          : 13:55
 * Decription    :
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home, null);

        return view;
    }
}
