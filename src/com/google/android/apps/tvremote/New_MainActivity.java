package com.google.android.apps.tvremote;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.apps.tvremote.fragment.SoftDpadFragment;
import com.google.android.apps.tvremote.layout.SlidingLayout;
import com.google.android.apps.tvremote.util.Action;
import com.google.android.apps.tvremote.util.LogUtils;
import com.google.android.apps.tvremote.util.PromptManager;
import com.google.android.apps.tvremote.widget.HighlightView;
import com.google.android.apps.tvremote.widget.KeyCodeButton;
import com.google.android.apps.tvremote.widget.SoftDpad;
import com.google.anymote.Key;

import java.util.ArrayList;

/**
 * Author        : lu
 * Data          : 2015/8/27
 * Time          : 11:15
 * Decription    :
 */
public class New_MainActivity extends BaseActivity implements KeyCodeButton.KeyCodeHandler {

    private final Handler handler;
    private SharedPreferences sharedPreferences;

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
        setContentView(R.layout.main);  // 加载主界面

        // 设置手势模式滑动带有振动效果
        sharedPreferences = getSharedPreferences(ConstValues.settings, MODE_PRIVATE);
        boolean vibrator = sharedPreferences.getBoolean("vibrator", true);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.commit();

        // 控制方向及确认键
        getFragmentManager().beginTransaction().replace(R.id.container, new SoftDpadFragment())
                .commit();

        flingIntent(getIntent());
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
            LogUtils.d( "No results from VoiceSearch server.");
            return;
        } else {
            searchQuery = queryResults.get(0);
            if (TextUtils.isEmpty(searchQuery)) {
                LogUtils.d( "Empty result from VoiceSearch server.");
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
