package com.google.android.apps.tvremote;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.tvremote.fragment.HomeFragment;
import com.google.android.apps.tvremote.fragment.MouseFragment;
import com.google.android.apps.tvremote.fragment.SoftDpadFragment;
import com.google.android.apps.tvremote.layout.SlidingLayout;
import com.google.android.apps.tvremote.util.LogUtils;
import com.google.android.apps.tvremote.util.PromptManager;
import com.google.android.apps.tvremote.widget.ActionBarDrawerToggle;
import com.google.android.apps.tvremote.widget.DrawerArrowDrawable;
import com.google.android.apps.tvremote.widget.KeyCodeButton;
import com.google.anymote.Key;

import java.util.ArrayList;

/**
 * Author        : lu
 * Data          : 2015/8/27
 * Time          : 11:15
 * Decription    :
 */
public class New_MainActivity extends BaseActivity implements KeyCodeButton.KeyCodeHandler,
        HomeFragment.ISwitchMode {

    private final Handler handler;
    private SharedPreferences sharedPreferences;
    private TextView gesture;
    private TextView mouse;
    private SoftDpadFragment softFragment;
    private MouseFragment mouseFragment;
    private HomeFragment homeFragment;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerArrowDrawable drawerArrow;
    private boolean drawerArrowColor;

    @Override
    public void switchSoftFragment() {
        getFragmentManager().beginTransaction().replace(R.id.area, softFragment)
                                    .commit();
    }

    @Override
    public void switchMouseFragment() {
        getFragmentManager().beginTransaction().replace(R.id.area, mouseFragment)
                .commit();
    }


    /**
     * The enum represents modes of the remote controller with
     * {@link SlidingLayout} screens assignment. In conjunction with
     * {@link } allows sliding between the screens.
     */
    private enum RemoteMode {
        TV(0, R.drawable.icon_04_touchpad_selector),
        TOUCHPAD(1, R.drawable.icon_04_buttons_selector);

        private final int screenId;
        private final int switchButtonId;

        RemoteMode(int screenId, int switchButtonId) {
            this.screenId = screenId;
            this.switchButtonId = switchButtonId;
        }
    }

    public New_MainActivity() {
        handler = new Handler();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_main);  // 加载主界面

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navdrawer);

        initDrawer();


        // 设置手势模式滑动带有振动效果
        sharedPreferences = getSharedPreferences(ConstValues.settings, MODE_PRIVATE);
        boolean vibrator = sharedPreferences.getBoolean(ConstValues.vibrator, true);
        SharedPreferences.Editor edit = sharedPreferences.edit();

        // 可关闭振动
        edit.putBoolean(ConstValues.vibrator, false);
        edit.commit();

        // 控制方向及确认键
        softFragment = new SoftDpadFragment();
        mouseFragment = new MouseFragment();
        homeFragment = new HomeFragment();

        getFragmentManager().beginTransaction().replace(R.id.new_container, homeFragment).commit();
//        initView();

//        initListener();

        flingIntent(getIntent());
    }

    private void initDrawer() {

        drawerArrow = new DrawerArrowDrawable(this) {
            @Override
            public boolean isLayoutRtl() {
                return false;
            }
        };
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                drawerArrow, R.string.drawer_open,
                R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        String[] values = new String[]{
                "Stop Animation (Back icon)",
                "Stop Animation (Home icon)",
                "Start Animation",
                "Change Color",
                "GitHub Page",
                "Share",
                "Rate"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mDrawerToggle.setAnimateEnabled(false);
                        drawerArrow.setProgress(1f);
                        break;
                    case 1:
                        mDrawerToggle.setAnimateEnabled(false);
                        drawerArrow.setProgress(0f);
                        break;
                    case 2:
                        mDrawerToggle.setAnimateEnabled(true);
                        mDrawerToggle.syncState();
                        break;
                    case 3:
                        if (drawerArrowColor) {
                            drawerArrowColor = false;
                            drawerArrow.setColor(R.color.ldrawer_color);
                        } else {
                            drawerArrowColor = true;
                            drawerArrow.setColor(R.color.drawer_arrow_second_color);
                        }
                        mDrawerToggle.syncState();
                        break;
                    case 4:
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/IkiMuhendis/LDrawer"));
                        startActivity(browserIntent);
                        break;
                    case 5:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_description) + "\n" +
                                "GitHub Page :  https://github.com/IkiMuhendis/LDrawer\n" +
                                "Sample App : https://play.google.com/store/apps/details?id=" +
                                getPackageName());
                        startActivity(Intent.createChooser(share, getString(R.string.app_name)));
                        break;
                    case 6:
                        String appUrl = "https://play.google.com/store/apps/details?id=" + getPackageName();
                        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl));
                        startActivity(rateIntent);
                        break;
                }

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // KeyCode handler implementation.
    public void onRelease(Key.Code keyCode) {
        getCommands().key(keyCode, Key.Action.UP);
    }

    public void onTouch(Key.Code keyCode) {
        playClick();
        getCommands().key(keyCode, Key.Action.DOWN);
    }

    private void playClick() {
        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).playSoundEffect(AudioManager.FX_KEY_CLICK);
    }

    private void flingIntent(Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    Uri uri = Uri.parse(text);
                    if (uri != null && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                        getCommands().flingUrl(text);
                    } else {
                        Toast.makeText(this, R.string.error_could_not_send_url, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    LogUtils.w("No URI to fling");
                }
            }
        }
    }

    @Override
    protected void onKeyboardOpened() {
        showActivity(KeyboardActivity.class);
    }

    // SUBACTIVITIES

    /**
     * The activities that can be launched from the main screen.
     * <p>
     * These codes should not conflict with the request codes defined in
     * {@link BaseActivity}.
     */
    private enum SubActivity {
        VOICE_SEARCH,
        UNKNOWN;

        public int code() {
            return BaseActivity.FIRST_USER_CODE + ordinal();
        }

        public static SubActivity fromCode(int code) {
            for (SubActivity activity : values()) {
                if (code == activity.code()) {
                    return activity;
                }
            }
            return UNKNOWN;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SubActivity activity = SubActivity.fromCode(requestCode);
        switch (activity) {
            case VOICE_SEARCH:
                onVoiceSearchResult(resultCode, data);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
            } else {
                mDrawerLayout.openDrawer(mDrawerList);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }



    //=====================================================================
    // 语音搜索， 暂未支持
    private void showVoiceSearchActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        int code = SubActivity.VOICE_SEARCH.code();
        LogUtils.e("voice code = " + code);
        startActivityForResult(intent, code);
    }

    private void onVoiceSearchResult(int resultCode, Intent data) {
        String searchQuery;

        if ((resultCode == RESULT_CANCELED) || (data == null)) {
            return;
        }

        ArrayList<String> queryResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if ((queryResults == null) || (queryResults.isEmpty())) {
            LogUtils.d("No results from VoiceSearch server.");
            return;
        } else {
            searchQuery = queryResults.get(0);
            if (TextUtils.isEmpty(searchQuery)) {
                LogUtils.d("Empty result from VoiceSearch server.");
                return;
            }
        }

        showVoiceSearchDialog(searchQuery);
    }

    private void showVoiceSearchDialog(final String query) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNeutralButton(R.string.voice_send, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getCommands().string(query);
            }
        }).setPositiveButton(R.string.voice_search_send, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getCommands().keyPress(Key.Code.KEYCODE_SEARCH);
                // Send query delayed
                handler.postDelayed(new Runnable() {
                    public void run() {
                        getCommands().string(query);
                    }
                }, getResources().getInteger(R.integer.search_query_delay));
            }
        }).setNegativeButton(R.string.pairing_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).setCancelable(true).setTitle(R.string.voice_dialog_label).setMessage(query);
        builder.create().show();
    }
}
