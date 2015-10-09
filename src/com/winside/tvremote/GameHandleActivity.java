package com.winside.tvremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.winside.tvremote.component.BatteryBroadcastListener;
import com.winside.tvremote.component.ScreenBroadcastListener;
import com.winside.tvremote.fragment.GameHandleFragment;
import com.winside.tvremote.fragment.GameTouchFragment;
import com.winside.tvremote.util.EffectUtils;
import com.winside.tvremote.util.LogUtils;
import com.winside.tvremote.util.PromptManager;
import com.winside.tvremote.widget.GameTouchMode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Author        : lu
 * Data          : 2015/9/22
 * Time          : 15:01
 * Decription    :
 */
public class GameHandleActivity extends CommonTitleActivity implements SensorEventListener,
        GameHandleFragment.BtnListener, GameTouchMode.MyGameGestureListener {

    // 连接的目标主机端口
    private static final int DST_PORT = 4215;
    //    private static String _TAG = "TDwifiRemote";
    private static int _OutputReportLen = 150;

    private static int _HWVer = 0x5002;
    private static int _SWVer = 0x0707;

    private GestureDetector detector;//手势识别

    //多点触摸
    private static int _TouchMaxCount = 5;

    //wifi是否开启
    private boolean _WifiEnable = false;
    private byte _RemoteMode = 0x00;
    private boolean _UpdateRemoteMode = false;
    //ip
    private int _IPAddr = 0;
    //mac地址
    private byte _MacAddr[] = new byte[6];
    private DisplayMetrics _DM = new DisplayMetrics();
    //目标地址
    private String _TargetIpAddr = "";

    //wifi link info
    private boolean _ServEnable = false;            //server connect or not ?
    private Socket _ServSK = null;                //local socket
    private DataOutputStream _Dout = null;                //send data
    private DataInputStream _Din = null;                //receive data
    private boolean _ServReconnect = false;


    //flag
    private boolean _StartScanHost = false; //开始搜索主机
    private boolean _StartConnect = false;  // 是否开始连接远程主机

    //remote data
    private SensorManager mSensorManager; //传感器
    private Vibrator mVibrator; //震动

    private byte _Battery = 50;
    private byte _Ability = 0x00;
    private short _Cycdata = 0;
    private boolean _TReadTEnable = true;
    private boolean _TSendTEnable = true;

    private byte _Btn = 0x00;
    private float _Acc[] = new float[3]; //加速度
    private float _Gyro[] = new float[3]; //陀螺仪
    private float _Mag[] = new float[3]; //地磁
    private float _Ori[] = new float[3]; //方向传感器数据
    private boolean _SingleTap = false;
    private boolean _SetBack = false;
    private byte _TouchStatus = 0x00;
    private float _TouchX[] = new float[_TouchMaxCount]; //触摸X坐标
    private float _TouchY[] = new float[_TouchMaxCount]; //触摸Y坐标

    private boolean _ServResp = false;            //receive 0x2f report from server (temp only)
    private int _GyroCali[] = new int[3];
    private int _MagCali[] = new int[4];

    private boolean _KeyboardUpdate = false;
    private int _KeyboardCount = 0;
    private int _KeyboradCode[] = new int[4];
    private boolean _KeyboradStatus[] = new boolean[4];

    //屏幕截图
    private long _AskPrintScreenTime = System.currentTimeMillis();
    private long _PrintScreenRecTime = 0;
    private int _PrintScreenDataLen = 0;
    private int _PSDataCount = 0;
    private boolean _PrintScreenRefresh = false;
    private boolean _AutoPrintScreen = false;

    //////////////
    private static int MaxKeyCodeNum = 92;
    private int KeyCodeToKeyboardCode[] = {0,    //KEYCODE_UNKNOWN = 0;
            0,    //KEYCODE_SOFT_LEFT = 1;
            0,    //KEYCODE_SOFT_RIGHT = 2;
            0,    //KEYCODE_HOME = 3;
            0,    //KEYCODE_BACK = 4;
            0,    //KEYCODE_CALL = 5;
            0,    //KEYCODE_ENDCALL = 6;
            11,    //KEYCODE_0 = 7;
            2,    //KEYCODE_1 = 8;
            3,    //KEYCODE_2 = 9;
            4,    //KEYCODE_3 = 10;
            5,    //KEYCODE_4 = 11;
            6,    //KEYCODE_5 = 12;
            7,    //KEYCODE_6 = 13;
            8,    //KEYCODE_7 = 14;
            9,    //KEYCODE_8 = 15;
            10,    //KEYCODE_9 = 16;
            0,    //KEYCODE_STAR = 17;
            0,    //KEYCODE_POUND = 18;
            0,    //KEYCODE_DPAD_UP = 19;
            0,    //KEYCODE_DPAD_DOWN = 20;
            0,    //KEYCODE_DPAD_LEFT = 21;
            0,    //KEYCODE_DPAD_RIGHT = 22;
            0,    //KEYCODE_DPAD_CENTER = 23;
            0,    //KEYCODE_VOLUME_UP = 24;
            0,    //KEYCODE_VOLUME_DOWN = 25;
            0,    //KEYCODE_POWER = 26;
            0,    //KEYCODE_CAMERA = 27;
            0,    //KEYCODE_CLEAR = 28;
            30,    //KEYCODE_A = 29;
            48,    //KEYCODE_B = 30;
            46,    //KEYCODE_C = 31;
            32,    //KEYCODE_D = 32;
            18,    //KEYCODE_E = 33;
            33,    //KEYCODE_F = 34;
            34,    //KEYCODE_G = 35;
            35,    //KEYCODE_H = 36;
            23,    //KEYCODE_I = 37;
            36,    //KEYCODE_J = 38;
            37,    //KEYCODE_K = 39;
            38,    //KEYCODE_L = 40;
            50,    //KEYCODE_M = 41;
            49,    //KEYCODE_N = 42;
            24,    //KEYCODE_O = 43;
            25,    //KEYCODE_P = 44;
            16,    //KEYCODE_Q = 45;
            19,    //KEYCODE_R = 46;
            31,    //KEYCODE_S = 47;
            20,    //KEYCODE_T = 48;
            22,    //KEYCODE_U = 49;
            47,    //KEYCODE_V = 50;
            17,    //KEYCODE_W = 51;
            45,    //KEYCODE_X = 52;
            21,    //KEYCODE_Y = 53;
            44,    //KEYCODE_Z = 54;
            51,    //KEYCODE_COMMA = 55;
            52,    //KEYCODE_PERIOD = 56;
            0,    //KEYCODE_ALT_LEFT = 57;
            0,    //KEYCODE_ALT_RIGHT = 58;
            42,    //KEYCODE_SHIFT_LEFT = 59;
            0,    //KEYCODE_SHIFT_RIGHT = 60;
            15,    //KEYCODE_TAB = 61;
            57,    //KEYCODE_SPACE = 62;
            0,    //KEYCODE_SYM = 63;
            0,    //KEYCODE_EXPLORER = 64;
            0,    //KEYCODE_ENVELOPE = 65;
            28,    //KEYCODE_ENTER = 66;
            111,    //KEYCODE_DEL = 67;
            41,    //KEYCODE_GRAVE = 68;
            12,    //KEYCODE_MINUS = 69;
            13,    //KEYCODE_EQUALS = 70;
            26,    //KEYCODE_LEFT_BRACKET = 71;
            27,    //KEYCODE_RIGHT_BRACKET = 72;
            43,    //KEYCODE_BACKSLASH = 73;
            39,    //KEYCODE_SEMICOLON = 74;
            40,    //KEYCODE_APOSTROPHE = 75;
            53,    //KEYCODE_SLASH = 76;
            0,    //KEYCODE_AT = 77;
            0,    //KEYCODE_NUM = 78;
            0,    //KEYCODE_HEADSETHOOK = 79;
            0,    //KEYCODE_FOCUS = 80;
            0,    //KEYCODE_PLUS = 81;
            0,    //KEYCODE_MENU = 82;
            0,    //KEYCODE_NOTIFICATION = 83;
            0,    //KEYCODE_SEARCH = 84;
            0,    //KEYCODE_MEDIA_PLAY_PAUSE = 85;
            0,    //KEYCODE_MEDIA_STOP = 86;
            0,    //KEYCODE_MEDIA_NEXT = 87;
            0,    //KEYCODE_MEDIA_PREVIOUS = 88;
            0,    //KEYCODE_MEDIA_REWIND = 89;
            0,    //KEYCODE_MEDIA_FAST_FORWARD = 90;
            0,    //KEYCODE_MUTE = 91;
    };
    private BatteryBroadcastListener batteryBroadcastListener;
    private SharedPreferences sharedPreferences;
    private GameHandleFragment gameHandleFragment;
    private GameTouchFragment gameTouchFragment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //no title bar
        //        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //        requestWindowFeature(Window.FEATURE_PROGRESS);
        //no status bar
        //        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //keep screen on 保持屏幕常亮，一旦锁屏就会关闭连接
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindowManager().getDefaultDisplay().getMetrics(_DM);

        setContentView(R.layout.gamehandle_activity);
        sharedPreferences = getSharedPreferences(ConstValues.settings, Context.MODE_PRIVATE);

        actionBar.setTitle(R.string.handle);
        LoadCaliInfo();

        // 由MainActivity跳转的时候传入参数
        String ip = getIntent().getStringExtra("ip");

        if (!TextUtils.isEmpty(ip)) {
            _TargetIpAddr = ip;
            LogUtils.e("remote ip = " + ip);
            _StartConnect = true;
        }

        _RemoteMode = 0x01;

        _LaunchHandler.post(_LaunchRunnable);
        _TSendThread.start(); //发送数据线程
        _TReadThread.start(); //读取数据线程


        //注册广播接收
        initBroadcast();

        // 初始化手势
//        gListener = new myGestureListener();
//        detector = new GestureDetector(this, gListener);

        LogUtils.e("onCreate ..");

        gameHandleFragment = new GameHandleFragment();
        gameTouchFragment = new GameTouchFragment();

        //初始化界面UI
        InitUI(_RemoteMode);
        //初始化WiFi状态
        InitWifi();

    }

    private void initBroadcast() {

        batteryBroadcastListener = new BatteryBroadcastListener(this);
        batteryBroadcastListener.begin(new BatteryBroadcastListener.BatteryChangeListener() {
            @Override
            public void onBatteryChangeListener(Intent intent) {
                int ilevel = intent.getIntExtra("level", 0);
                int iScale = intent.getIntExtra("scale", 100);
                _Battery = (byte) ((ilevel * 100) / iScale);
                LogUtils.i("battery level: " + _Battery);
            }
        });
    }

    protected void onResume() {
        super.onResume();

        LogUtils.e("onResume RemoteMode = " + _RemoteMode);
        // 振动器服务
        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        //传感器服务，必须要放在onResume方法中，防止锁屏后再打开出现程序崩溃
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> sensors_list = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        if (sensors_list.size() > 0) {
            for (int i = 0; i < sensors_list.size(); i++) {
                Sensor tsen = sensors_list.get(i);
                switch (tsen.getType()) {
                    case Sensor.TYPE_ACCELEROMETER: //加速度
                        _Ability |= 0x01;
                        mSensorManager.registerListener(this, tsen, SensorManager.SENSOR_DELAY_GAME);
                        LogUtils.e("registered sensor: " + tsen.getName() + "--" + tsen.getType());
                        break;
                    case Sensor.TYPE_GYROSCOPE: //陀螺仪
                        _Ability |= 0x02;
                        mSensorManager.registerListener(this, tsen, SensorManager.SENSOR_DELAY_GAME);
                        LogUtils.e("registered sensor: " + tsen.getName() + "--" + tsen.getType());
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD://地磁
                        _Ability |= 0x04;
                        mSensorManager.registerListener(this, tsen, SensorManager.SENSOR_DELAY_GAME);
                        LogUtils.e("registered sensor: " + tsen.getName() + "--" + tsen.getType());
                        break;
                    case Sensor.TYPE_ORIENTATION: // 方向传感
                        _Ability |= 0x08;
                        mSensorManager.registerListener(this, tsen, SensorManager.SENSOR_DELAY_GAME);
                        LogUtils.e("registered sensor: " + tsen.getName() + "--" + tsen.getType());
                        break;
                    default:
                        LogUtils.e("sensor found: " + tsen.getName() + " " + tsen.getType());
                        break;
                }
            }
        }
    }

    protected void onPause() {
        super.onPause();
        LogUtils.e("onPause ....");
        // 取消传感器接收，推荐在onPause()方法中进行
        mSensorManager.unregisterListener(this);
        //        unregisterReceiver(mBatInfoReceiver);
        mSensorManager = null;
        //        System.gc();
    }

    protected void onDestroy() {
        SaveCaliInfo();
        //_TReadThread.interrupt();
        _TSendTEnable = false;
        _TReadTEnable = false;

        _LaunchHandler.removeCallbacks(_LaunchRunnable);

        DisconnectHost();

        batteryBroadcastListener.unregisterBroadcast();
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game, menu);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            case R.id.air_mouse_mode:
                _RemoteMode = 0x01;
                InitUI(_RemoteMode);
                break;
            case R.id.touch_mouse_mode:
                _RemoteMode = 0x02;
                InitUI(_RemoteMode);
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.i ("Virtual Key Test", "onKeyDown: " + keyCode + ";  getKeyCode: " + event.getKeyCode());
        //Log.i(_TAG, "onKeyDown :" + keyCode);

        if (keyCode < MaxKeyCodeNum) {
            int keyboardcode = KeyCodeToKeyboardCode[keyCode];
            LogUtils.e("onKeyDown keyCode = " + keyCode + " KeyCodeToKeyboardCode[keyCode] = " +
                    keyboardcode);
            if (keyboardcode > 0) {
                RemoteKeyboardUpdate(keyboardcode, true);
            }
        }


        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.i(_TAG, "onKeyUp :" + keyCode);
        if (keyCode < MaxKeyCodeNum) {
            int keyboardcode = KeyCodeToKeyboardCode[keyCode];
            LogUtils.e("keyboardcode = " + keyboardcode);
            if (keyboardcode > 0) {
                RemoteKeyboardUpdate(keyboardcode, false);
            }
        }
        return super.onKeyUp(keyCode, event);
    }



    private void InitUI(byte mode) {

        //dialog
        final AlertDialog.Builder exit_builder = new AlertDialog.Builder(this);
        exit_builder.setTitle(R.string.app_tips);
        exit_builder.setMessage(R.string.exit_message);
        exit_builder.setIcon(android.R.drawable.ic_dialog_alert);
        exit_builder.setPositiveButton(R.string.app_exit, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {GameHandleActivity.this.finish();}
        });
        exit_builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        });

        switch (mode) {

            // 已连接到体感游戏
            case 0x01:

                getFragmentManager().beginTransaction().replace(R.id.game_container, gameHandleFragment).commit();

                break;

            case 0x02: // 触摸鼠标模式
                getFragmentManager().beginTransaction().replace(R.id.game_container,
                        gameTouchFragment).commit();

                break;
            case 0x03: {

                break;
            }
            case 0x04: {

                break;
            }
        }
    }

    private void InitWifi() {
        //wifi
        WifiManager mWifiManager;
        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = mWifiManager.getConnectionInfo();

        //mac address 程序先执行 获取mac
        String hostmac = info.getMacAddress();
        ConvertMACAddr(hostmac);
        LogUtils.i("mac addr: " + hostmac);

        //ip address  获取IP address
        int hostip = info.getIpAddress();
        _IPAddr = hostip;
        LogUtils.i("Local IP Address: " + Integer.toHexString(hostip));

        //dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.wifi_alter);
        builder.setCancelable(false);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.app_setting, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 0);}
        });
        builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {GameHandleActivity.this.finish();}
        });

        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED)        //wifi off
        {
            builder.setMessage(R.string.wifi_disable);
            builder.show();
        } else if (hostip == 0)            //network not available
        {
            builder.setMessage(R.string.wifi_error);
            builder.show();
        } else {
            _WifiEnable = true;
        }
    }


    private boolean ConnectHost(String addr) {
        if (_ServEnable) return true;

        try {
            _ServSK = new Socket(addr, DST_PORT);
            _ServSK.setTcpNoDelay(true);
            _ServSK.setSendBufferSize(8192);
            _Dout = new DataOutputStream(_ServSK.getOutputStream());
            _Din = new DataInputStream(_ServSK.getInputStream());

            _ServSK.setSoTimeout(1000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        _ServEnable = true;

        return true;
    }


    private boolean DisconnectHost() {
        if (_ServSK == null) {
            return true;
        }
        _ServEnable = false;
        try {
            _Dout.close();
            _Din.close();
            _ServSK.close();
        } catch (IOException e) {
            _ServSK = null;
            return true;
        }
        _ServSK = null;
        return true;
    }

    private boolean TryReconnect() {
        _ServReconnect = true;

        DisconnectHost();
        boolean res = ConnectHost(_TargetIpAddr);

        _ServReconnect = false;
        return res;
    }

    private Handler _LaunchHandler = new Handler();
    private Runnable _LaunchRunnable = new Runnable() {
        public void run() {

            if (_StartConnect) {  // 开始连接
                LogUtils.i("_StartConnect start....");

                _StartConnect = false;

                if (!TextUtils.isEmpty(_TargetIpAddr)) {
                    LogUtils.e("_TargetIpAddr = " + _TargetIpAddr + " 开始连接主机");
                    Thread thread = new Thread(new _RConnectHost(_TargetIpAddr));
                    thread.start();
                } else {
                    connectStateHandler.sendEmptyMessage(NEED_CONN);
                }
            }

           /* if (_ServEnable) {
                if (_RemoteMode == 0) {
                    LogUtils.e("_Ability & 0x02" + (_Ability & 0x02));
                    InitUI(((_Ability & 0x02) != 0x00) ? (byte) 0x01 : (byte) 0x02);
                }

                if (_UpdateRemoteMode) {
                    _UpdateRemoteMode = false;
                    InitUI(_RemoteMode);
                }

                if (_RemoteMode == 3) {
                    if (_PrintScreenRefresh) {
                        _PrintScreenRefresh = false;
                        SetTouchBG(false);
                        _PrintScreenRecTime = System.currentTimeMillis();
                    }
                }
            } else {
                if ((!_ServReconnect) && (_RemoteMode != 0x00) && (_RemoteMode != 0x10)) {
                    if (_RemoteMode == 0x04) {
                        ShowKeyboard(false);
                    }
                    //                    InitUI((byte) 0x00);
                }
            }*/

            _LaunchHandler.postDelayed(_LaunchRunnable, 10);
        }
    };



    private class _RConnectHost implements Runnable {
        private String ipaddr;

        public _RConnectHost(String addr) {
            this.ipaddr = addr;
        }

        public void run() {
            if (ConnectHost(ipaddr)) {
                LogUtils.i("ConnectHost Success");
            } else {
                LogUtils.i("ConnectHost fail");
                connectStateHandler.sendEmptyMessage(CONNECT_FAILD);
            }
        }
    }

    private static final int CONNECT_FAILD = 0;  // 连接失败
    private static final int DISCONNECT = -1;  // 连接断开
    private static final int NEED_CONN = -2;  // 未连接

    private Handler connectStateHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case CONNECT_FAILD:
                    PromptManager.showToastLong(GameHandleActivity.this, R.string.connectHost_fail);
                    break;
                case DISCONNECT:
                    PromptManager.showToastLong(GameHandleActivity.this, R.string.disconnect);
                    break;
                case NEED_CONN:
                    PromptManager.showToastLong(GameHandleActivity.this, R.string.need_conn);
                    break;

                default:

                    break;
            }
        }
    };

    private void ConvertMACAddr(String mac) {
        int tMac = 0;
        for (int i = 0; i < 6; i++) {
            tMac = (CharToByte(mac.charAt(i * 3)) * 16) + CharToByte(mac.charAt((i * 3) + 1));
            _MacAddr[i] = (byte) tMac;
        }
    }

    private byte CharToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    // 传感器系统接口回调，精度改变时触发
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // 传感器系统接口回调，数值改变时回调，注意，该方法会一直频繁调用，禁止添加耗时操作
    public void onSensorChanged(SensorEvent event) {
        // 接受地磁传感器的类型
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            _Mag[0] = event.values[0];
            _Mag[1] = event.values[1];
            _Mag[2] = event.values[2];
        }
        // 接受方位传感器的类型
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            _Ori[0] = event.values[0];
            _Ori[1] = event.values[1];
            _Ori[2] = event.values[2];
        }
        // 接受加速度感应器的类型
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            _Acc[0] = event.values[0];  // X轴，横向翻转改变其值，向右翻转为负数,向左翻转为正数
            _Acc[1] = event.values[1]; // Y轴，纵向翻转改变其值，向内向自己翻转为正数，向外翻转为负数
            _Acc[2] = event.values[2]; // Z轴，屏幕对应的方向，屏幕朝上为正数，朝下为负数
        }
        // 接受陀螺仪传感器的类型
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            _Gyro[0] = event.values[0];
            _Gyro[1] = event.values[1];
            _Gyro[2] = event.values[2];
        }
    }


    private void RemoteSetVibrator(int ms) {
        if (ms > 0) {
            mVibrator.vibrate((ms > 2550) ? 2550 : ms);
        } else {
            mVibrator.cancel();
        }
    }

    private Thread _TSendThread = new Thread() {
        public void run() {
            while (_TSendTEnable) {
                if (_ServEnable) {
                    RemoteSend0x4aReport();
                    if ((_RemoteMode == 3) && (_AutoPrintScreen)) {
                        if (((System.currentTimeMillis() - _AskPrintScreenTime) > 3000) || ((_PrintScreenRecTime != 0) && ((System.currentTimeMillis() - _PrintScreenRecTime) > 500))) {
                            _AskPrintScreenTime = System.currentTimeMillis();
                            LogUtils.e("Send PS report");
                            RemoteSend0x71Report();
                        }
                    }
                }

                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Thread _TReadThread = new Thread() {
        public void run() {
            int tRequestCount = 0;
            while (_TReadTEnable) {
                if (_ServEnable) {
                    if ((_ServSK == null) || (!_ServSK.isConnected()) || (_Din == null)) {
                        DisconnectHost();
                    } else {
                        if (!_ServResp) {
                            tRequestCount++;
                            LogUtils.e("RemoteSend 0x70");
                            RemoteSend0x70Report();
                            if (tRequestCount > 10) {
                                DisconnectHost();
                            } //timeout invalid host
                        }

                        while (_TReadTEnable && _ServEnable) {
                            try {
                                byte[] buffer = new byte[1500];
                                int rlen = _Din.read(buffer);

                                if (rlen > 0) {
                                    RemoteParseReport(buffer, rlen);
                                } else {
                                    break;
                                }
                            } catch (IOException e) {
                                break;
                            }//timeout
                        }
                    }
                }

                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void RemoteParseReport(byte[] data, int len) {
        if (data == null) return;
        if (len <= 0) return;

        LogUtils.e("解析远程发送过来的数据： " + Arrays.toString(data));
        switch (data[0]) {
            case 0x20:        //set vibrator
            {
                //if (len != 4)				break;
                if (data[3] > 0) {
                    RemoteSetVibrator((int) data[3] * 10);
                } else if (data[3] > 0) {
                    RemoteSetVibrator(0);
                }
                break;
            }
            case 0x26:        //write user data
            {
                if ((data[1] == 0x00) && (data[2] == 0x00) && (data[3] == 0x00)) {
                    if (data[4] == (byte) 0xC4)        //gyro cali
                    {
                        int tlen = (data[5] & 0x1f);
                        if (tlen == 12) {
                            int tdata = 0;
                            tdata = ((data[6] << 24) & 0xff000000) | ((data[7] << 16) & 0x00ff0000) | ((data[8] << 8) & 0x0000ff00) | (data[9] & 0x000000ff);
                            _GyroCali[0] = tdata;
                            tdata = ((data[10] << 24) & 0xff000000) | ((data[11] << 16) & 0x00ff0000) | ((data[12] << 8) & 0x0000ff00) | (data[13] & 0x000000ff);
                            _GyroCali[1] = tdata;
                            tdata = ((data[14] << 24) & 0xff000000) | ((data[15] << 16) & 0x00ff0000) | ((data[16] << 8) & 0x0000ff00) | (data[17] & 0x000000ff);
                            _GyroCali[2] = tdata;
                            //Log.i(_TAG, "Read Cali Gyro: " + _GyroCali[0] + " ; " + _GyroCali[1] + " ; " + _GyroCali[2]);
                        }
                    }
                    if (data[4] == (byte) 0x86)        //mag cali
                    {
                        int tlen = (data[5] & 0x1f);
                        if (tlen == 8) {
                            int tdata = 0;
                            tdata = ((data[6] << 8) & 0x0000ff00) | (data[7] & 0x000000ff);
                            _MagCali[0] = tdata;
                            tdata = ((data[8] << 8) & 0x0000ff00) | (data[9] & 0x000000ff);
                            _MagCali[1] = tdata;
                            tdata = ((data[10] << 8) & 0x0000ff00) | (data[11] & 0x000000ff);
                            _MagCali[2] = tdata;
                            tdata = ((data[12] << 8) & 0x0000ff00) | (data[13] & 0x000000ff);
                            _MagCali[3] = tdata;
                            //Log.i(_TAG, "Read Cali Mag: " + _MagCali[0] + " ; " + _MagCali[1] + " ; " + _MagCali[2] + " ; " + _MagCali[3]);
                        }
                    }
                }
                break;
            }
            case 0x27:        //read user data
            {
                if ((data[1] == 0x00) && (data[2] == 0x00) && (data[3] == 0x00)) {
                    if (data[4] == (byte) 0xC0)        //gyro cali
                    {
                        int tlen = ((data[5] << 8) & 0x0000ff00) | (data[6] & 0x000000ff);
                        if (tlen == 16) {
                            RemoteSend0x31Report(2);
                        }
                    }
                    if (data[4] == (byte) 0x86)        //mag cali
                    {
                        int tlen = ((data[5] << 8) & 0x0000ff00) | (data[6] & 0x000000ff);
                        if (tlen == 8) {
                            RemoteSend0x31Report(1);
                        }
                    }
                }
                break;
            }
            case 0x2e: {
                //if (len != 14)				break;
                if (data[1] == 1) {
                    switch (data[2]) {
                        case 1: {
                            _RemoteMode = ((_Ability & 0x02) != 0x00) ? (byte) 0x01 : (byte) 0x02;
                            _UpdateRemoteMode = true;
                            break;
                        }
                        case 2:
                        case 3: {
                            _RemoteMode = (byte) data[2];
                            _UpdateRemoteMode = true;
                            break;
                        }
                        default:
                            break;
                    }
                } else if (data[1] == 2) {
                    int tdata = 0;
                    tdata = ((data[2] << 24) & 0xff000000) | ((data[3] << 16) & 0x00ff0000) | ((data[4] << 8) & 0x0000ff00) | (data[5] & 0x000000ff);
                    _PrintScreenDataLen = tdata;
                    //Log.i(_TAG, "_PrintScreenDataLen: " + _PrintScreenDataLen);
                }
                break;
            }
            case 0x2f:        //response
            {
                if ((data[1] == (byte) 0x74) && (data[2] == (byte) 0x92) && (data[3] == (byte) 0x13) && (data[4] == (byte) 0xe2) && (data[5] == (byte) 0xa1) && (data[6] == (byte) 0x9a)) {
                    //valid
                    //	Log.i(_TAG, "0x2f, flag3");
                    _ServResp = true;
                }
                break;
            }
        }
    }

    private boolean RemoteSendData(byte[] data, int len) {
        if (!_ServEnable) {
            return false;
        }
        if ((_ServSK == null) || (_Dout == null)) {
            return false;
        }
        if (!_ServSK.isConnected()) {
            return false;
        }
        if (len > _OutputReportLen) {
            return false;
        }

        try {
            _Dout.write(data);
            //_Dout.flush();
        } catch (IOException e) {
            LogUtils.e("_ErrCount (write)");
            if (!TryReconnect()) {
                //连接断开
                LogUtils.e("Reconnect failed, disconnect");
                connectStateHandler.sendEmptyMessage(DISCONNECT);
                DisconnectHost();
            }
            return false;
        }
        return true;
    }

    //code: 1 - Mag Calibrate data; 2 - Gyro Calibrate data
    private boolean RemoteSend0x31Report(int code) {
        switch (code) {
            case 1:        //mag
            {
                byte data[] = new byte[_OutputReportLen];
                data[0] = (byte) 0x31;
                data[1] = (byte) 0x70;
                data[2] = (byte) 0x00;
                data[3] = (byte) 0x86;
                data[4] = (byte) ((_MagCali[0] >> 8) & 0x000000ff);
                data[5] = (byte) ((_MagCali[0] >> 0) & 0x000000ff);
                data[6] = (byte) ((_MagCali[1] >> 8) & 0x000000ff);
                data[7] = (byte) ((_MagCali[1] >> 0) & 0x000000ff);
                data[8] = (byte) ((_MagCali[2] >> 8) & 0x000000ff);
                data[9] = (byte) ((_MagCali[2] >> 0) & 0x000000ff);
                data[10] = (byte) ((_MagCali[3] >> 8) & 0x000000ff);
                data[11] = (byte) ((_MagCali[3] >> 0) & 0x000000ff);
                return RemoteSendData(data, _OutputReportLen);
            }
            case 2:        //gyro
            {
                byte data[] = new byte[_OutputReportLen];
                data[0] = (byte) 0x31;
                data[1] = (byte) 0xF0;
                data[2] = (byte) 0x00;
                data[3] = (byte) 0xC0;
                data[4] = (byte) 0x00;
                data[5] = (byte) 0x00;
                data[6] = (byte) 0x00;
                data[7] = (byte) 0x00;
                data[8] = (byte) ((_GyroCali[0] >> 24) & 0x000000ff);
                data[9] = (byte) ((_GyroCali[0] >> 16) & 0x000000ff);
                data[10] = (byte) ((_GyroCali[0] >> 8) & 0x000000ff);
                data[11] = (byte) ((_GyroCali[0] >> 0) & 0x000000ff);
                data[12] = (byte) ((_GyroCali[1] >> 24) & 0x000000ff);
                data[13] = (byte) ((_GyroCali[1] >> 16) & 0x000000ff);
                data[14] = (byte) ((_GyroCali[1] >> 8) & 0x000000ff);
                data[15] = (byte) ((_GyroCali[1] >> 0) & 0x000000ff);
                data[16] = (byte) ((_GyroCali[2] >> 24) & 0x000000ff);
                data[17] = (byte) ((_GyroCali[2] >> 16) & 0x000000ff);
                data[18] = (byte) ((_GyroCali[2] >> 8) & 0x000000ff);
                data[19] = (byte) ((_GyroCali[2] >> 0) & 0x000000ff);
                return RemoteSendData(data, _OutputReportLen);
            }
        }

        return true;
    }

    // 操作发送传感器等数据方法
    private boolean RemoteSend0x4aReport() {
        int curlen = 0;

        byte data[] = new byte[_OutputReportLen];

        //rpt id
        data[curlen] = 0x4a; // 74
        curlen += 1;

        //for length
        curlen += 2;

        //HWVer, SWVer
        // curlen = 3    _HWVer = 0x5002  _SWVer = 0x0707
        data[curlen] = (byte) ((_HWVer >> 8) & 0x000000ff); // 0x50 == 80
        data[curlen + 1] = (byte) ((_HWVer) & 0x000000ff); // 0x02
        data[curlen + 2] = (byte) ((_SWVer >> 8) & 0x000000ff); // 0x07
        data[curlen + 3] = (byte) ((_SWVer) & 0x000000ff); // 0x07
        curlen += 4; // 7

        //MAC addr data[] = { 0x4a, 0, 0, 0x50, 0x02, 0x07, 0x07, -16, -122, -10, 85, 35, 79, 0...}
        for (int i = 0; i < 6; i++) {
            data[curlen + i] = _MacAddr[i];
        }
        curlen += 6; // 13

        //battery —— data[13]存放手机当前电量信息
        data[curlen] = _Battery;
        curlen += 1;
        // curlen == 14

        //cycdata
        data[curlen] = (byte) _Cycdata;
        _Cycdata++;
        if (_Cycdata > 255) {
            _Cycdata = 0;
        }
        curlen += 1;

        //time
        long tNow = System.currentTimeMillis();
        data[curlen] = (byte) ((tNow >> 8) & 0x0000000f); //
        data[curlen + 1] = (byte) ((tNow) & 0x000000ff);
//        LogUtils.e("tNow = " + Long.toHexString(tNow) + " data["+ curlen +"] = " + data[curlen]
//                + " data["+ 16 +"] = " + data[curlen+1]);
        curlen += 2;

        // curlen == 17
        //ability
        data[curlen] = 0x00;
        data[curlen + 1] = _Ability;
        curlen += 2;

        //button
        data[curlen] = 0x00;
        data[curlen + 1] = _Btn;

        if (data[curlen + 1] != 0) {
            LogUtils.e("data["+19+"] = " + data[curlen+1] + " curlen = " + curlen);
        }
        if (_SingleTap) { // 在触摸屏模式，点击时改为true
            if (_RemoteMode == 0x02) {
                data[curlen + 1] |= 0x02; // data[20]
            }
            LogUtils.e("data[] = " + Arrays.toString(data) + "\n 0xa7 = " + Long.toBinaryString
                    (0xa7)
                    + " curlen = " + curlen);
            _SingleTap = false;
        }

//        LogUtils.e("data[] = " + Arrays.toString(data)
//                                + " curlen = " + curlen);

        if (_SetBack) {   // 触摸屏模式中的主机[返回]键，已废弃
            data[curlen + 1] |= 0x08;
            _SetBack = false;
        }
        curlen += 2; // curlen == 21

        int tmp;

//        LogUtils.e("原始加速度值 x = " + _Acc[0] + " y = " + _Acc[1] + " z = " + _Acc[2]);
//        LogUtils.e("原始加速度值*100000 x = " + _Acc[0]*100000 + " y = " + _Acc[1]*100000 + " z = " +
//                _Acc[2]*100000 + " curlen = " + curlen);
        /**
         * 精确加速度传感器的值
         */
        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Acc[i] * 100000);
            //            LogUtils.e("_Acc["+ i +"] = " + _Acc[i] * 100000);
            //            LogUtils.e("temp hex = " + Integer.toBinaryString(tmp));

            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

//        LogUtils.e("处理后的加速度 X0 = " + data[21] + " X1 = " + data[22] + " X2 = " +
//                data[23] + " X flag = " + data[24]);

        /**
         * 精确陀螺仪传感器的值
         */
        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Gyro[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }
//        LogUtils.e("处理后的陀螺仪 X0 = " + data[33] + " X1 = " + data[34] + " X2 = " +
//                data[35] + " X flag = " + data[36]);

//        LogUtils.e("data[] = " + Arrays.toString(data) + "\n curlen = " + curlen);

        /**
         * 磁场传感器的值
         */
        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Mag[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

        /**
         * 方向传感器数值
         * values[1] --- pitch倾斜角，即由静止状态开始，前后翻转
         */
        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Ori[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
//            LogUtils.e("方向 X = " + data[curlen] + " Y = " + data[curlen + 1] + " Z = " +
//                    data[curlen + 2]);
            curlen += 4;
        }

//        LogUtils.e("处理后的方向传感器 X0 = " + data[57] + " X1 = " + data[58] + " X2 = " +
//                                data[59] + " X flag = " + data[60]);
//        LogUtils.e("data[] = " + Arrays.toString(data) + "\n curlen = " + curlen);

        data[curlen] = (byte) _TouchStatus;
        curlen += 1;

        LogUtils.e("_TouchStatus = " + data[69]);

        for (int i = 0; i < _TouchMaxCount; i++) {
            if ((_TouchStatus & (0x01 << i)) == 0x00) {
                _TouchX[i] = 0;
                _TouchY[i] = 0;
            }
            tmp = (int) (_TouchX[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
            tmp = (int) (_TouchY[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

        data[curlen] = (byte) _RemoteMode;
        curlen += 1;

        tmp = _DM.widthPixels;
        data[curlen] = (byte) ((tmp >> 8) & 0x0000000f);
        data[curlen + 1] = (byte) ((tmp) & 0x000000ff);
        curlen += 2;
        tmp = _DM.heightPixels;
        data[curlen] = (byte) ((tmp >> 8) & 0x0000000f);
        data[curlen + 1] = (byte) ((tmp) & 0x000000ff);
        curlen += 2;

        if (_KeyboardUpdate) {
            _KeyboardUpdate = false;
            data[curlen] = (byte) _KeyboardCount;
            curlen += 1;
            for (int i = 0; i < _KeyboardCount; i++) {
                data[curlen + i * 3] = _KeyboradStatus[i] ? (byte) 0x01 : (byte) 0x00;
                data[curlen + 1 + i * 3] = (byte) ((_KeyboradCode[i] >> 8) & 0x000000ff);
                data[curlen + 2 + i * 3] = (byte) ((_KeyboradCode[i]) & 0x000000ff);

                //Log.i (_TAG, "send key, status: " + data[curlen+i*3] + "; keycode: " + _KeyboradCode[i]);
            }
            _KeyboardCount = 0;
        } else {
            data[curlen] = 0x00;
            curlen += 1;
        }
        curlen += 12;

        //Log.i(_TAG, "send len: " + curlen);

        data[1] = (byte) ((curlen >> 8) & 0x000000ff);
        data[2] = (byte) ((curlen) & 0x000000ff);
//        LogUtils.e("最终发送的数据：" + Arrays.toString(data) + " 长度： " + curlen);
        return RemoteSendData(data, curlen);
    }

    private boolean RemoteSend0x70Report() {
        if (!_ServEnable) return false;
        if (_ServSK == null) return false;

        int localport = _ServSK.getLocalPort();

        byte data[] = new byte[_OutputReportLen];
        data[0] = (byte) 0x70;
        data[1] = (byte) 0x65;
        data[2] = (byte) 0x99;
        data[3] = (byte) 0x17;
        data[4] = (byte) 0xE8;
        data[5] = (byte) 0x86;
        data[6] = (byte) 0xA2;
        data[7] = (byte) ((_IPAddr >> 0) & 0x000000ff);
        data[8] = (byte) ((_IPAddr >> 8) & 0x000000ff);
        data[9] = (byte) ((_IPAddr >> 16) & 0x000000ff);
        data[10] = (byte) ((_IPAddr >> 24) & 0x000000ff);
        data[11] = (byte) ((localport >> 24) & 0x000000ff);
        data[12] = (byte) ((localport >> 16) & 0x000000ff);
        data[13] = (byte) ((localport >> 8) & 0x000000ff);
        data[14] = (byte) ((localport >> 0) & 0x000000ff);
        for (int i = 0; i < 6; i++) {
            data[15 + i] = _MacAddr[i];
        }
        return RemoteSendData(data, _OutputReportLen);
    }

    private boolean RemoteSend0x71Report() {
        if (!_ServEnable) return false;
        if (_ServSK == null) return false;

        _PrintScreenDataLen = 0;
        _PSDataCount = 0;
        _PrintScreenRecTime = 0;

        byte data[] = new byte[_OutputReportLen];
        data[0] = (byte) 0x71;
        for (int i = 0; i < 6; i++) {
            data[1 + i] = 0x00;
        }
        return RemoteSendData(data, _OutputReportLen);
    }

    private boolean RemoteKeyboardUpdate(int keycode, boolean status) {
        if (_KeyboardUpdate) return false;

        _KeyboradCode[_KeyboardCount] = keycode;
        _KeyboradStatus[_KeyboardCount] = status;
        _KeyboardCount++;

        if (status) {
            if (_KeyboardCount < 4) return true;
        } else {
            boolean bshift = false;
            for (int i = 0; i < _KeyboardCount; i++) {
                if (_KeyboradCode[i] == 42) {
                    if (_KeyboradStatus[i]) {
                        bshift = true;
                    }
                    if (!_KeyboradStatus[i]) {
                        bshift = false;
                    }
                }
            }
            if ((_KeyboardCount < 4) && bshift) return true;
        }

        _KeyboardUpdate = true;
        return true;
    }

    private boolean LoadCaliInfo() {
        try {
            FileInputStream mFile = openFileInput("CaliInfo_R.txt");
            byte[] bytes = new byte[100];
            int len = mFile.read(bytes);
            mFile.close();

            String msg = new String(bytes);
            String[] newsplitstr = msg.split(";");
            //Log.i("load file", len + " : " + msg);
            for (int i = 0; (i < newsplitstr.length) & (i < 7); i++) {
                int imsg = Integer.parseInt(newsplitstr[i]);
                LogUtils.i("load file data: " + imsg);
                if (i < 3) {
                    _GyroCali[i] = imsg;
                } else {
                    _MagCali[i - 3] = imsg;
                }
            }
        } catch (FileNotFoundException e) {
            for (int i = 0; i < 3; i++) {
                _GyroCali[i] = 0;
            }
            for (int i = 0; i < 4; i++) {
                _MagCali[i] = 0;
            }
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            for (int i = 0; i < 3; i++) {
                _GyroCali[i] = 0;
            }
            for (int i = 0; i < 4; i++) {
                _MagCali[i] = 0;
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean SaveCaliInfo() {
        try {
            FileOutputStream mFile = openFileOutput("CaliInfo_R.txt", Activity.MODE_PRIVATE);
            String msg = _GyroCali[0] + ";" + _GyroCali[1] + ";" + _GyroCali[2] + ";" + _MagCali[0] + ";" + _MagCali[1] + ";" + _MagCali[2] + ";" + _MagCali[3] + ";";
            //Log.i("save file", msg);
            mFile.write(msg.getBytes());
            mFile.flush();
            mFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // =================== 空中鼠标手柄模式 ==================================================
    // GameHandleFragment中的接口回调
    @Override
    public void TouchListener(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.handle_x_key:
            case R.id.handle_d_key:
                int d_key = touchBtn(event.getAction(), v);
                if (d_key == 1) {
                    _Btn |= 0x01;  // 1
                } else if (d_key == 0) {
                    _Btn &= 0xfe; // 归零操作，以下类似
                }
                break;
            case R.id.handle_b_key:
            case R.id.handle_y_key:
                int y_key = touchBtn(event.getAction(), v);
                if (y_key == 1) {
                    _Btn |= 0x08; // 8
                } else if (y_key == 0) {
                    _Btn &= 0xf7;
                }
                break;
            case R.id.handle_a_key:
                int a_key = touchBtn(event.getAction(), v);
                if (a_key == 1) {
                    _Btn |= 0x02; // 2
                } else if (a_key == 0) {
                    _Btn &= 0xfd;
                }
                break;
            case R.id.handle_up_img_btn:
                int up_btn = touchBtn(event.getAction(), v);
                if (up_btn == 1) {
                    _Btn |= 0x10; // 16
                } else if (up_btn == 0) {
                    _Btn &= 0xef;
                }
                break;
            case R.id.handle_down_img_btn:
                int down_btn = touchBtn(event.getAction(), v);
                if (down_btn == 1) {
                    _Btn |= 0x20; // 32
                } else if (down_btn == 0) {
                    _Btn &= 0xdf;
                }
                break;
            case R.id.handle_left_img_btn:
                int left_btn = touchBtn(event.getAction(), v);
                if (left_btn == 1) {
                    _Btn |= 0x40; // 64
                } else if (left_btn == 0) {
                    _Btn &= 0xbf;
                }
                break;
            case R.id.handle_right_img_btn:
                int right_btn = touchBtn(event.getAction(), v);
                if (right_btn == 1) {
                    _Btn |= 0x80; // -128
                } else if (right_btn == 0) {
                    _Btn &= 0x7f;
                }
                break;
        }
    }

    private int touchBtn(int actionType, View view) {
        switch (actionType) {
            case MotionEvent.ACTION_DOWN: {
                EffectUtils.triggerVibrator(GameHandleActivity.this, view, sharedPreferences.getBoolean(ConstValues.vibrator, true));
                return 1;
            }
            case MotionEvent.ACTION_UP:
                return 0;
            default:
                return -1;
        }
    }



    // ================== 触摸屏模式 =========================================================
    // 这些手势监听是在GameTouchFragment中进行的，这里是回调
    public void onSingleTapUp() {
        _SingleTap = true;
        EffectUtils.triggerVibrator(GameHandleActivity.this, gameTouchFragment.getTouchView(),
                sharedPreferences.getBoolean(ConstValues.vibrator, true));
        LogUtils.e("触摸模式，单击....");
    }

    @Override
    public void onTouchStatus() {
        _TouchStatus = 0x00;
    }

    @Override
    public void mulTouch(int pointer, int i) {
        if (pointer != -1) {
            _TouchStatus |= (byte) (0x0100 >> (8 - i));
        } else {
            _TouchStatus &= (byte) (0xFEFF >> (8 - i));
        }
    }

    @Override
    public void acitonMove(int pointer, int current, MotionEvent event) {
        if (pointer != -1) {
            _TouchStatus |= (byte) (0x0100 >> (8 - current));
            _TouchX[current] = event.getX(pointer);
            _TouchY[current] = event.getY(pointer);
        } else {
            _TouchStatus &= (byte) (0xFEFF >> (8 - current));
        }
    }






    // 操作TV端弹出键盘，暂时无用
    /*private void ShowKeyboard(boolean enable) {
        InputMethodManager _IMM = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!enable) {
            _IMM.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
            _IMM.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else {
            _IMM.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void SetTouchBG(Boolean reset) {
        View background;
        background = (View) findViewById(R.id.game_mouse);

        if (reset) {
            background.setBackgroundResource(R.drawable.handle_img_mw_bg);
            return;
        }

        FileInputStream fosfrom;
        try {
            fosfrom = openFileInput("printscreen.jpg");
            Bitmap bitmapOrg = BitmapFactory.decodeStream(fosfrom);

            float scaleW = (float) background.getHeight() / bitmapOrg.getWidth();
            float scaleH = (float) background.getWidth() / bitmapOrg.getHeight();
            Matrix matrixSR = new Matrix();
            matrixSR.postScale(scaleW, scaleH);
            matrixSR.postRotate(90);

            Bitmap SRBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, bitmapOrg.getWidth(), bitmapOrg.getHeight(), matrixSR, true); //
            BitmapDrawable bmd = new BitmapDrawable(SRBitmap);

            background.setBackgroundDrawable(bmd);
            fosfrom.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }*/

}
