/*
 * Copyright (C) 2010 Google Inc.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.winside.tvremote;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.winside.tvremote.util.PromptManager;

import java.util.ArrayList;

/**
 * About activity.
 */
public class AboutActivity extends CommonTitleActivity implements AdapterView.OnItemClickListener {
    private ListView about_list;
    private String[] lists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setActionBarTitle(R.string.about);

        TextView versionTextView = (TextView) findViewById(R.id.version_text);
        about_list = ((ListView) findViewById(R.id.about_list));

        lists = new String[] { "检查新版本", "用户反馈", "操作指南", "特别鸣谢", "多屏互动QQ群" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
               R.layout.about_item, lists);

        about_list.setAdapter(adapter);


        String versionString = getString(R.string.unknown_build);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0 /* basic info */);
            versionString = info.versionName;
        } catch (NameNotFoundException e) {
            // do nothing
        }
        versionTextView.setText(getString(R.string.about_version_title, versionString));

        about_list.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch(position) {
            case 0:
                // 检查新版本
                PromptManager.showToastTest(this, lists[position]);
                break;
            case 1:
                // 用户反馈
                PromptManager.showToastTest(this, lists[position]);
                break;
            case 2:
                // 操作指南
                Intent intent = new Intent(this, TutorialActivity.class);
                startActivity(intent);
                break;
            case 3:
                // 特别鸣谢
                PromptManager.showToastTest(this, lists[position]);
                break;
            case 4:
                // 多屏互动群
                PromptManager.showToastTest(this, lists[position]);
                break;
            default:

                break;
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 优先响应onTouchEvent
        this.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 点击跳转链接
     */
    private class GoToLinkListener implements OnClickListener {
        private String link;

        public GoToLinkListener(int linkId) {
            this.link = getString(linkId);
        }

        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(link));
            startActivity(intent);
        }
    }
}
