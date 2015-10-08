package com.winside.tvremote;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import com.winside.tvremote.systembar.SystemBarTintManager;
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
    protected SystemBarTintManager mTintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // 沉浸式状态栏
        // 沉浸式状态栏，因为会遮挡住drawerlayout中的一部分，暂时不用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            setTranslucentStatus(true);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintResource(R.color.actionbar_color);
        }
    }

    protected void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
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
                    overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
                }
                LogUtils.e("finalX = " + finalX);
                break;
            default:

                break;
        }
        return super.onTouchEvent(event);
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tutorial, menu);
        return true;
    }*/

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
