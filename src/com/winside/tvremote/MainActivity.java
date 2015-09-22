package com.winside.tvremote;

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
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.winside.tvremote.fragment.HomeFragment;
import com.winside.tvremote.fragment.MouseFragment;
import com.winside.tvremote.fragment.SoftDpadFragment;
import com.winside.tvremote.systembar.SystemBarTintManager;
import com.winside.tvremote.util.LogUtils;
import com.winside.tvremote.util.PromptManager;
import com.winside.tvremote.widget.ActionBarDrawerToggle;
import com.winside.tvremote.widget.DrawerArrowDrawable;
import com.winside.tvremote.widget.KeyCodeButton;
import com.google.anymote.Key;

import java.util.ArrayList;

/**
 * Author        : lu
 * Data          : 2015/8/27
 * Time          : 11:15
 * Decription    :
 */
public class MainActivity extends BaseActivity implements KeyCodeButton.KeyCodeHandler, HomeFragment.ISwitchMode {

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
        getFragmentManager().beginTransaction().replace(R.id.area, softFragment).commit();
    }

    @Override
    public void switchMouseFragment() {
        getFragmentManager().beginTransaction().replace(R.id.area, mouseFragment).commit();
    }


    public MainActivity() {
        handler = new Handler();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); // 加载主界面

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


        flingIntent(getIntent());

    }

    private void initDrawer() {

        drawerArrow = new DrawerArrowDrawable(this) {
            @Override
            public boolean isLayoutRtl() {
                return false;
            }
        };
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, drawerArrow, R.string.drawer_open, R.string.drawer_close) {

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

        final String[] values = new String[]{"扫一扫", "多屏互动", "游戏手柄", "体感手柄", "文件共享", "检查更新", "设置"};
        //        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
        Integer[] icons = new Integer[]{R.drawable.scan, R.drawable.miracast, R.drawable.game_hand,
                R.drawable.somatic_hand, R.drawable.file_share, R.drawable.check_version,
                R.drawable.drawer_setting};
        DrawerAdapter adapter = new DrawerAdapter(this, values, icons);

        mDrawerList.setAdapter(adapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:  // 扫一扫
                            Intent scan = new Intent(MainActivity.this, MipcaActivityCapture.class);
                            startActivity(scan);
                        break;
                    case 1:  // 多屏互动(无线显示)
                            Intent wifi_display = new Intent(Settings.ACTION_SETTINGS);
                            startActivity(wifi_display);
                        break;
                    case 2:   // 游戏手柄
                        Intent game = new Intent(MainActivity.this, GameHandleActivity.class);
                        startActivity(game);
                        break;
                    case 3:    // 体感手柄
                        Intent handle = new Intent(MainActivity.this, HandleActivity.class);
                        String address = getCoreService().getTarget().getAddress().getHostAddress();
                        LogUtils.e("address = " + address);
                        handle.putExtra("ip", address);
                        startActivity(handle);
                        break;
                    case 4:    // 文件共享

                        break;
                    case 5:   // 检查更新
                        //                        Intent share = new Intent(Intent.ACTION_SEND);
                        //                        share.setType("text/plain");
                        //                        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //                        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                        //                        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_description) + "\n" +
                        //                                "GitHub Page :  https://github.com/IkiMuhendis/LDrawer\n" +
                        //                                "Sample App : https://play.google.com/store/apps/details?id=" +
                        //                                getPackageName());
                        //                        startActivity(Intent.createChooser(share, getString(R.string.app_name)));
                        break;
                    case 6:  // 设置
                        //                        String appUrl = "https://play.google.com/store/apps/details?id=" + getPackageName();
                        //                        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl));
                        //                        startActivity(rateIntent);
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


    class DrawerAdapter extends BaseAdapter {
        private Context context;
        private String[] labels;
        private Integer[] icons;

        public DrawerAdapter(Context context, String[] labels, Integer[] icons) {
            this.context = context;
            this.labels = labels;
            this.icons = icons;
        }

        @Override
        public int getCount() {
            return labels.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            DrawerItem drawerItem;
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.drawer_item, null);
                drawerItem = new DrawerItem();
                drawerItem.drawer_iv = (ImageView) convertView.findViewById(R.id.drawer_iv);
                drawerItem.drawer_tv = (TextView) convertView.findViewById(R.id.drawer_tv);
                convertView.setTag(drawerItem);
            } else {
                drawerItem = (DrawerItem) convertView.getTag();
            }

            drawerItem.drawer_iv.setImageResource(icons[position]);
            drawerItem.drawer_tv.setText(labels[position]);

            return convertView;
        }
    }


    /**
     * 侧滑菜单的Item
     */
    class DrawerItem {
        public ImageView drawer_iv;
        public TextView drawer_tv;
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
