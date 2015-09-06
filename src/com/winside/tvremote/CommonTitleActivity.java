package com.winside.tvremote;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.winside.tvremote.util.LogUtils;

/**
 * Author        : lu
 * Data          : 2015/9/2
 * Time          : 11:19
 * Decription    :
 */
public abstract class CommonTitleActivity extends Activity {

    protected ActionBar actionBar;
    private float tempX = 0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }


    /**
     * 子类使用修改ActionBar的标题
     * @param title
     */
    protected void setActionBarTitle(String title) {
        actionBar.setTitle(title);
    }

    protected void setActionBarTitle(int resId) {
        actionBar.setTitle(resId);
    }


    /**
     * 滑动返回
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = 0f;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                tempX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x = event.getX();
                float finalX = x - tempX;
                if (finalX > 200f) {
                    this.finish();
                }
                LogUtils.e("finalX = " + finalX);
                break;
            default:

                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tutorial, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
