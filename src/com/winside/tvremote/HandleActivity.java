package com.winside.tvremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.winside.tvremote.util.LogUtils;
import com.winside.zxing.camera.CameraManager;

import com.winside.zxing.decoding.InactivityTimer;
import com.winside.zxing.handle.decoding.CaptureActivityHandler;
import com.winside.zxing.handle.view.ViewfinderView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import java.io.File;
//import java.lang.reflect.Method;
//import java.lang.Math;
//import jp.sourceforge.qrcode.QRCodeDecoder;
//import jp.sourceforge.qrcode.data.QRCodeImage;
//import android.os.Environment;
//import android.annotation.SuppressLint;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.PixelFormat;
//import android.graphics.drawable.Drawable;
//import android.hardware.Camera;
//import android.hardware.Camera.PictureCallback;
//import android.hardware.Camera.AutoFocusCallback;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.ViewGroup.LayoutParams;

public class HandleActivity extends CommonTitleActivity implements SensorEventListener, Callback {
//    private static String _TAG = "TDwifiRemote";
    private static int _OutputReportLen = 150;

    private static int _HWVer = 0x5002;
    private static int _SWVer = 0x0707;

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

    private ImageButton Btn_scan;//搜索按钮
    private ImageButton Btn_barcode;//二维码按钮
    private ImageButton Btn_ip;//手动输入IP
    private ImageButton Btn_exit;//退出按钮

    //wifi link info
    private boolean _ServEnable = false;            //server connect or not ?
    private Socket _ServSK = null;                //local socket
    private DataOutputStream _Dout = null;                //send data
    private DataInputStream _Din = null;                //receive data
    private boolean _ServReconnect = false;

    //scan mode 
    private ArrayList<String> t_ipArray = new ArrayList<String>();        //server's ip address
    private Lock _QHLock = new ReentrantLock();
    private int _QHRunnableCnt = 0;                            //Query Host Runnable count 搜索主机地址线程数量

    //flag
    private boolean _StartScanHost = false; //开始搜索主机
    private boolean _StartIPInput = false;  //开始输入IP

    //remote data
    private SensorManager mSensorManager; //传感器
    private GestureDetector detector;//手势识别
    private myGestureListener gListener;
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

    //for zxing 扫码二维码
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface = false;

    //条码格式
    private Vector<BarcodeFormat> decodeFormats = null;
    private String characterSet = null;
    private InactivityTimer inactivityTimer;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //no title bar
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        requestWindowFeature(Window.FEATURE_PROGRESS);
        //no status bar
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //keep screen on
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindowManager().getDefaultDisplay().getMetrics(_DM);

        actionBar.setTitle(R.string.handle);
        LoadCaliInfo();

        _LaunchHandler.post(_LaunchRunnable);
        _TSendThread.start(); //发送数据线程
        _TReadThread.start(); //读取数据线程
        gListener = new myGestureListener();
        detector = new GestureDetector(this, gListener);

        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        //注册广播接收
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    protected void onResume() {
        super.onResume();

        //初始化界面UI
        InitUI(_RemoteMode);
        //初始化WiFi状态
        InitWifi();
        //传感器控制
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

        if (_RemoteMode == 0x04) {
            ShowKeyboard(false);
        }
        if (_RemoteMode == 0x10) {
            bcExit();
        }

        mSensorManager.unregisterListener(this);
        unregisterReceiver(mBatInfoReceiver);
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

//        System.gc();
//        System.runFinalization();
//        System.exit(0);

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.i ("Virtual Key Test", "onKeyDown: " + keyCode + ";  getKeyCode: " + event.getKeyCode());
        //Log.i(_TAG, "onKeyDown :" + keyCode);

        if (keyCode < MaxKeyCodeNum) {
            int keyboardcode = KeyCodeToKeyboardCode[keyCode];
            if (keyboardcode > 0) {
                RemoteKeyboardUpdate(keyboardcode, true);
            }
        }

/*        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            //dialog 提示 是否确定要退出
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_tips);
            builder.setMessage(R.string.exit_message);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(R.string.app_exit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {HandleActivity.this.finish();}
                    });
            builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {}
                    });

            builder.show();
            //Log.i (_TAG, "KEYCODE_BACK");
           return false;
      }
      */
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.i(_TAG, "onKeyUp :" + keyCode);
        if (keyCode < MaxKeyCodeNum) {
            int keyboardcode = KeyCodeToKeyboardCode[keyCode];
            if (keyboardcode > 0) {
                RemoteKeyboardUpdate(keyboardcode, false);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private int touchBtn(ImageButton btn, int actionType, int downID, int upID) {
        switch (actionType) {
            case MotionEvent.ACTION_DOWN: {
                RemoteSetVibrator(10);
                btn.setImageDrawable(getResources().getDrawable(downID));
                return 1;
            }
            case MotionEvent.ACTION_UP:
                btn.setImageDrawable(getResources().getDrawable(upID));
                return 0;
            default:
                return -1;
        }
    }

    private void InitUI(byte mode) {
        _RemoteMode = mode;

        //dialog 
        final AlertDialog.Builder exit_builder = new AlertDialog.Builder(this);
        exit_builder.setTitle(R.string.app_tips);
        exit_builder.setMessage(R.string.exit_message);
        exit_builder.setIcon(android.R.drawable.ic_dialog_alert);
        exit_builder.setPositiveButton(R.string.app_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {HandleActivity.this.finish();}
                });
        exit_builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                });

        switch (mode) {
            case 0x00: {
                setContentView(R.layout.handle_activity_launch);

                Btn_scan = (ImageButton) findViewById(R.id.imageButton_scan);
                Btn_barcode = (ImageButton) findViewById(R.id.imageButton_barcode);
                Btn_ip = (ImageButton) findViewById(R.id.imageButton_ip);
                Btn_exit = (ImageButton) findViewById(R.id.imageButton_exit);

                //scan
                Btn_scan.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_scan, e.getAction(), R.drawable.handle_img_scan_on, R.drawable.handle_img_scan_off);
                        if (t == 0) {
                            Btn_ip.setEnabled(false);
                            Btn_barcode.setEnabled(false);
                            _StartScanHost = true;
                        }
                        return true;
                    }
                });

                //barcode
                Btn_barcode.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_barcode, e.getAction(), R.drawable.handle_img_bc_on, R.drawable.handle_img_bc_off);
                        if (t == 0) {
                            InitUI((byte) 0x10);
                        }
                        return true;
                    }
                });

                //ip
                Btn_ip.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_ip, e.getAction(), R.drawable.handle_img_ip_on, R.drawable.handle_img_ip_off);
                        if (t == 0) {
                            _StartIPInput = true;
                        }
                        return true;
                    }
                });

                //exit
                Btn_exit.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_exit, e.getAction(), R.drawable.handle_img_exit_on, R.drawable.handle_img_exit_off);
                        if (t == 0) {
                            HandleActivity.this.finish();
                        }
                        return true;
                    }
                });

                break;
            }
            case 0x10: {
                LogUtils.i("0x10 in");
                setContentView(R.layout.handle_activity_bcview);
                bcInit();
                break;
            }
            case 0x01: {
                setContentView(R.layout.handle_activity_remote);

                final ImageButton Btn_A = (ImageButton) findViewById(R.id.imageButton_m1_a);
                final ImageButton Btn_B = (ImageButton) findViewById(R.id.imageButton_m1_b);
                final ImageButton Btn_M = (ImageButton) findViewById(R.id.imageButton_m1_m);
                final ImageButton Btn_J = (ImageButton) findViewById(R.id.imageButton_m1_j);
                final ImageButton Btn_Up = (ImageButton) findViewById(R.id.imageButton_m1_up);
                final ImageButton Btn_Down = (ImageButton) findViewById(R.id.imageButton_m1_down);
                final ImageButton Btn_Left = (ImageButton) findViewById(R.id.imageButton_m1_left);
                final ImageButton Btn_Right = (ImageButton) findViewById(R.id.imageButton_m1_right);
                final ImageButton Btn_Setting = (ImageButton) findViewById(R.id.imageButton_m1_setting);

                //A
                Btn_A.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_A, e.getAction(), R.drawable.handle_img_ma_a_on, R.drawable.handle_img_ma_a_off);
                        if (t == 1) {
                            _Btn |= 0x02;
                        } else if (t == 0) {
                            _Btn &= 0xfd;
                        }
                        return true;
                    }
                });

                //B
                Btn_B.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_B, e.getAction(), R.drawable.handle_img_ma_b_on, R.drawable.handle_img_ma_b_off);
                        if (t == 1) {
                            _Btn |= 0x04;
                        } else if (t == 0) {
                            _Btn &= 0xfb;
                        }
                        return true;
                    }
                });

                //M
                Btn_M.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_M, e.getAction(), R.drawable.handle_img_ma_m_on, R.drawable.handle_img_ma_m_off);
                        if (t == 1) {
                            _Btn |= 0x08;
                        } else if (t == 0) {
                            _Btn &= 0xf7;
                        }
                        return true;
                    }
                });

                //J
                Btn_J.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_J, e.getAction(), R.drawable.handle_img_ma_j_on, R.drawable.handle_img_ma_j_off);
                        if (t == 1) {
                            _Btn |= 0x01;
                        } else if (t == 0) {
                            _Btn &= 0xfe;
                        }
                        return true;
                    }
                });

                //UP
                Btn_Up.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Up, e.getAction(), R.drawable.handle_img_ma_up_on, R.drawable.handle_img_ma_up_off);
                        if (t == 1) {
                            _Btn |= 0x10;
                        } else if (t == 0) {
                            _Btn &= 0xef;
                        }
                        return true;
                    }
                });

                //DOWN
                Btn_Down.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Down, e.getAction(), R.drawable.handle_img_ma_down_on, R.drawable.handle_img_ma_down_off);
                        if (t == 1) {
                            _Btn |= 0x20;
                        } else if (t == 0) {
                            _Btn &= 0xdf;
                        }
                        return true;
                    }
                });

                //LEFT
                Btn_Left.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Left, e.getAction(), R.drawable.handle_img_ma_left_on, R.drawable.handle_img_ma_left_off);
                        if (t == 1) {
                            _Btn |= 0x40;
                        } else if (t == 0) {
                            _Btn &= 0xbf;
                        }
                        return true;
                    }
                });

                //RIGHT
                Btn_Right.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Right, e.getAction(), R.drawable.handle_img_ma_right_on, R.drawable.handle_img_ma_right_off);
                        if (t == 1) {
                            _Btn |= 0x80;
                        } else if (t == 0) {
                            _Btn &= 0x7f;
                        }
                        return true;
                    }
                });

                //Setting
                Btn_Setting.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Setting, e.getAction(), R.drawable.handle_img_setting_on, R.drawable.handle_img_setting_off);
                        if (t == 0) {
                            final String[] item = new String[4];
                            item[0] = getResources().getString(R.string.mode_2);
                            item[1] = getResources().getString(R.string.mode_3);
                            item[2] = getResources().getString(R.string.mode_4);
                            item[3] = getResources().getString(R.string.set_exit);

                            //dialog
                            final AlertDialog.Builder modeSelect = new AlertDialog.Builder(HandleActivity.this);

                            //dialog
                            modeSelect.setTitle(R.string.mode_select);
                            modeSelect.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            });
                            modeSelect.setItems(item, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: {
                                            InitUI((byte) 0x02);
                                            break;
                                        }
                                        case 1: {
                                            InitUI((byte) 0x03);
                                            break;
                                        }
                                        case 2: {
                                            InitUI((byte) 0x04);
                                            break;
                                        }
                                        case 3: {
                                            exit_builder.show();
                                            break;
                                        }
                                    }
                                }
                            });

                            modeSelect.show();
                        }
                        return true;
                    }
                });

                break;
            }
            case 0x02: {
                setContentView(R.layout.handle_activity_mouse);

                final ImageButton Btn_A = (ImageButton) findViewById(R.id.imageButton_m2_a);
                final ImageButton Btn_B = (ImageButton) findViewById(R.id.imageButton_m2_b);
                final ImageButton Btn_M = (ImageButton) findViewById(R.id.imageButton_m2_m);
                final ImageButton Btn_J = (ImageButton) findViewById(R.id.imageButton_m2_j);
                final ImageButton Btn_Setting = (ImageButton) findViewById(R.id.imageButton_m2_setting);

                //A
                Btn_A.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_A, e.getAction(), R.drawable.handle_img_ma_a_on, R.drawable.handle_img_ma_a_off);
                        if (t == 1) {
                            _Btn |= 0x02;
                        } else if (t == 0) {
                            _Btn &= 0xfd;
                        }
                        return true;
                    }
                });

                //B
                Btn_B.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_B, e.getAction(), R.drawable.handle_img_ma_b_on, R.drawable.handle_img_ma_b_off);
                        if (t == 1) {
                            _Btn |= 0x04;
                        } else if (t == 0) {
                            _Btn &= 0xfb;
                        }
                        return true;
                    }
                });

                //M
                Btn_M.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_M, e.getAction(), R.drawable.handle_img_ma_m_on, R.drawable.handle_img_ma_m_off);
                        if (t == 1) {
                            _Btn |= 0x08;
                        } else if (t == 0) {
                            _Btn &= 0xf7;
                        }
                        return true;
                    }
                });

                //J
                Btn_J.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_J, e.getAction(), R.drawable.handle_img_ma_jo_on, R.drawable.handle_img_ma_jo_off);
                        if (t == 1) {
                            _Btn |= 0x01;
                        } else if (t == 0) {
                            _Btn &= 0xfe;
                        }
                        return true;
                    }
                });

                //Setting
                Btn_Setting.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Setting, e.getAction(), R.drawable.handle_img_setting_on, R.drawable.handle_img_setting_off);
                        if (t == 0) {
                            final String[] item = new String[4];
                            item[0] = getResources().getString(R.string.mode_1);
                            item[1] = getResources().getString(R.string.mode_3);
                            item[2] = getResources().getString(R.string.mode_4);
                            item[3] = getResources().getString(R.string.set_exit);

                            //dialog
                            final AlertDialog.Builder modeSelect = new AlertDialog.Builder(HandleActivity.this);

                            //dialog
                            modeSelect.setTitle(R.string.mode_select);
                            modeSelect.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            });
                            modeSelect.setItems(item, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: {
                                            InitUI((byte) 0x01);
                                            break;
                                        }
                                        case 1: {
                                            InitUI((byte) 0x03);
                                            break;
                                        }
                                        case 2: {
                                            InitUI((byte) 0x04);
                                            break;
                                        }
                                        case 3: {
                                            exit_builder.show();
                                            break;
                                        }
                                    }
                                }
                            });

                            modeSelect.show();
                        }
                        return true;
                    }
                });

                break;
            }
            case 0x03: {
                setContentView(R.layout.handle_activity_mtouch);

                final ImageButton Btn_Setting = (ImageButton) findViewById(R.id.imageButton_mw_setting);

                //Setting
                Btn_Setting.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Setting, e.getAction(), R.drawable.handle_img_settingw_on, R.drawable.handle_img_settingw_off);
                        if (t == 0) {
                            final String[] item = new String[6];
                            item[0] = getResources().getString(R.string.mode_1);
                            item[1] = getResources().getString(R.string.mode_2);
                            item[2] = getResources().getString(R.string.mode_4);
                            item[3] = getResources().getString(R.string.set_back);
                            item[4] = _AutoPrintScreen ? getResources().getString(R.string.set_AutoPrintScreenOff) : getResources().getString(R.string.set_AutoPrintScreenOn);
                            item[5] = getResources().getString(R.string.set_exit);

                            //dialog
                            final AlertDialog.Builder modeSelect = new AlertDialog.Builder(HandleActivity.this);

                            //dialog
                            modeSelect.setTitle(R.string.mode_select);
                            modeSelect.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            });
                            modeSelect.setItems(item, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: {
                                            InitUI((byte) 0x01);
                                            break;
                                        }
                                        case 1: {
                                            InitUI((byte) 0x02);
                                            break;
                                        }
                                        case 2: {
                                            //_SetBack = true;
                                            InitUI((byte) 0x04);
                                            break;
                                        }
                                        case 3: {
                                            _SetBack = true;
                                            break;
                                        }
                                        case 4: {
                                            _AutoPrintScreen = !_AutoPrintScreen;
                                            break;
                                        }
                                        case 5: {
                                            exit_builder.show();
                                            break;
                                        }
                                    }
                                }
                            });

                            modeSelect.show();
                        }
                        return true;
                    }
                });

                break;
            }
            case 0x04: {
                setContentView(R.layout.handle_activity_keyboard);
                ShowKeyboard(true);

                final ImageButton Btn_J = (ImageButton) findViewById(R.id.imageButton_mk_j);
                final ImageButton Btn_Up = (ImageButton) findViewById(R.id.imageButton_mk_up);
                final ImageButton Btn_Down = (ImageButton) findViewById(R.id.imageButton_mk_down);
                final ImageButton Btn_Left = (ImageButton) findViewById(R.id.imageButton_mk_left);
                final ImageButton Btn_Right = (ImageButton) findViewById(R.id.imageButton_mk_right);
                final ImageButton Btn_M = (ImageButton) findViewById(R.id.imageButton_mk_m);
                final ImageButton Btn_Setting = (ImageButton) findViewById(R.id.imageButton_mk_setting);

                Btn_J.setFocusable(false);
                Btn_Up.setFocusable(false);
                Btn_Down.setFocusable(false);
                Btn_Left.setFocusable(false);
                Btn_Right.setFocusable(false);
                Btn_M.setFocusable(false);
                Btn_Setting.setFocusable(false);

                //J
                Btn_J.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_J, e.getAction(), R.drawable.handle_img_ma_j_on, R.drawable.handle_img_ma_j_off);
                        if (t == 1) {
                            _Btn |= 0x01;
                        } else if (t == 0) {
                            _Btn &= 0xfe;
                        }
                        return true;
                    }
                });

                //UP
                Btn_Up.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Up, e.getAction(), R.drawable.handle_img_ma_up_on, R.drawable.handle_img_ma_up_off);
                        if (t == 1) {
                            _Btn |= 0x10;
                        } else if (t == 0) {
                            _Btn &= 0xef;
                        }
                        return true;
                    }
                });

                //DOWN
                Btn_Down.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Down, e.getAction(), R.drawable.handle_img_ma_down_on, R.drawable.handle_img_ma_down_off);
                        if (t == 1) {
                            _Btn |= 0x20;
                        } else if (t == 0) {
                            _Btn &= 0xdf;
                        }
                        return true;
                    }
                });

                //LEFT
                Btn_Left.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Left, e.getAction(), R.drawable.handle_img_ma_left_on, R.drawable.handle_img_ma_left_off);
                        if (t == 1) {
                            _Btn |= 0x40;
                        } else if (t == 0) {
                            _Btn &= 0xbf;
                        }
                        return true;
                    }
                });

                //RIGHT
                Btn_Right.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Right, e.getAction(), R.drawable.handle_img_ma_right_on, R.drawable.handle_img_ma_right_off);
                        if (t == 1) {
                            _Btn |= 0x80;
                        } else if (t == 0) {
                            _Btn &= 0x7f;
                        }
                        return true;
                    }
                });

                //M
                Btn_M.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_M, e.getAction(), R.drawable.handle_img_ma_m_on, R.drawable.handle_img_ma_m_off);
                        if (t == 1) {
                            _Btn |= 0x08;
                        } else if (t == 0) {
                            _Btn &= 0xf7;
                        }
                        return true;
                    }
                });

                //Setting
                Btn_Setting.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        int t = touchBtn(Btn_Setting, e.getAction(), R.drawable.handle_img_setting_on, R.drawable.handle_img_setting_off);
                        if (t == 0) {
                            final String[] item = new String[4];
                            item[0] = getResources().getString(R.string.mode_1);
                            item[1] = getResources().getString(R.string.mode_2);
                            item[2] = getResources().getString(R.string.mode_3);
                            item[3] = getResources().getString(R.string.set_exit);

                            //dialog
                            final AlertDialog.Builder modeSelect = new AlertDialog.Builder(HandleActivity.this);

                            //dialog
                            modeSelect.setTitle(R.string.mode_select);
                            modeSelect.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            });
                            modeSelect.setItems(item, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: {
                                            InitUI((byte) 0x01);
                                            ShowKeyboard(false);
                                            break;
                                        }
                                        case 1: {
                                            InitUI((byte) 0x02);
                                            ShowKeyboard(false);
                                            break;
                                        }
                                        case 2: {
                                            InitUI((byte) 0x03);
                                            ShowKeyboard(false);
                                            break;
                                        }
                                        case 3: {
                                            exit_builder.show();
                                            break;
                                        }
                                    }
                                }
                            });

                            modeSelect.show();
                        }
                        return true;
                    }
                });
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
        LogUtils.i("Local IP Address: " + hostip);

        //dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.wifi_alter);
        builder.setCancelable(false);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.app_setting, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 0);}
                });
        builder.setNegativeButton(R.string.app_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {HandleActivity.this.finish();}
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
            _ServSK = new Socket(addr, 4215);
            _ServSK.setTcpNoDelay(true);
            _ServSK.setSendBufferSize(8192);
            _Dout = new DataOutputStream(_ServSK.getOutputStream());
            _Din = new DataInputStream(_ServSK.getInputStream());

            _ServSK.setSoTimeout(1000);
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
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
            if (_StartScanHost) {
                _StartScanHost = false;
                if (!_WifiEnable) {
                    return;
                }
                //开始查找主机
                LogUtils.i("QueryHost start....");

                DisconnectHost();
                t_ipArray.clear();

                int threadnum = 10;
                int ipstep = 253 / threadnum;
                int ipstart = 2;

                for (; ; ) {
                    int ipend = ipstart + ipstep;
                    ipend = (ipend > 254) ? 254 : ipend;
                    Log.e("LaunchActivity", "ipend = " + ipend);
                    _QHLock.lock();
                    _QHRunnableCnt++;
                    _QHLock.unlock();
                    Thread thread = new Thread(new _RQueryHost(ipstart, ipend));
                    thread.start();

                    ipstart += ipstep + 1;
                    if (ipstart > 254) {
                        break;
                    }
                }

                while (_QHRunnableCnt != 0) {
                    SystemClock.sleep(100);
                }

                //print host address  查找到的主机地址列表
                for (int i = 0; i < t_ipArray.size(); i++) {
                    LogUtils.i("QueryHost " + t_ipArray.get(i));
                }

                if (t_ipArray.size() > 0)        //
                {
                    //Log.i(_TAG, "QueryHost ShowBuilder");
                    final String[] item = new String[t_ipArray.size()];
                    for (int i = 0; i < t_ipArray.size(); i++) {
                        item[i] = t_ipArray.get(i);
                    }

                    //dialog
                    final AlertDialog.Builder ipSelect = new AlertDialog.Builder(HandleActivity.this);

                    //dialog
                    ipSelect.setTitle(R.string.scan_select);  //主机选择
                    ipSelect.setCancelable(false);
                    ipSelect.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {t_ipArray.clear();}
                    });
                    ipSelect.setItems(item, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            t_ipArray.clear();
                            _TargetIpAddr = item[which];
                            Thread thread = new Thread(new _RConnectHost(_TargetIpAddr));
                            thread.start();
                        }
                    });

                    ipSelect.show();
                }
                LogUtils.i("QueryHost end....");

                Btn_ip.setEnabled(true);
                Btn_barcode.setEnabled(true);
            }

            if (_StartIPInput)  // 
            {
                LogUtils.i("_StartIPInput start....");

                _StartIPInput = false;
                DisconnectHost();

                final EditText ipinput = new EditText(HandleActivity.this);

                final AlertDialog.Builder ipInsert = new AlertDialog.Builder(HandleActivity.this);
                final AlertDialog.Builder ipInsertErr = new AlertDialog.Builder(HandleActivity.this);

                ipInsertErr.setTitle(R.string.ip_input_error_title);
                ipInsertErr.setCancelable(false);
                ipInsertErr.setIcon(android.R.drawable.ic_dialog_alert);
                ipInsertErr.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {_StartIPInput = true;}
                });

                //insert ip
                ipInsert.setTitle(R.string.ip_input_title);
                ipInsert.setIcon(android.R.drawable.ic_dialog_info);
                ipInsert.setView(ipinput);
                ipInsert.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String ip;
                        ip = ipinput.getText().toString();

                        LogUtils.i("ipinput: " + ip);

                        Pattern patt = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
                        Matcher mat = patt.matcher(ip);

                        if (!mat.matches())            //error1
                        {
                            ipInsertErr.setMessage(R.string.ip_input_error1);
                            ipInsertErr.show();
                            return;
                        }

                        int ipaddr[] = new int[4];
                        int position1 = ip.indexOf(".");
                        int position2 = ip.indexOf(".", position1 + 1);
                        int position3 = ip.indexOf(".", position2 + 1);

                        ipaddr[0] = (int) Long.parseLong(ip.substring(0, position1));
                        ipaddr[1] = (int) Long.parseLong(ip.substring(position1 + 1, position2));
                        ipaddr[2] = (int) Long.parseLong(ip.substring(position2 + 1, position3));
                        ipaddr[3] = (int) Long.parseLong(ip.substring(position3 + 1));
                        for (int i = 0; i < 4; i++) {
                            if ((ipaddr[i] > 255) || (ipaddr[i] < 0))        //error2
                            {
                                ipInsertErr.setMessage(R.string.ip_input_error2);
                                ipInsertErr.show();
                                return;
                            }
                        }

                        _TargetIpAddr = ip;
                        Thread thread = new Thread(new _RConnectHost(_TargetIpAddr));
                        thread.start();
                    }
                });
                ipInsert.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                });

                ipInsert.show();
            }

            if (_ServEnable) {
                if (_RemoteMode == 0) {
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
                    InitUI((byte) 0x00);
                }
            }

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
            }
        }
    }

    ;

    private class _RQueryHost implements Runnable {
        private int ipStart;
        private int ipEnd;

        public _RQueryHost(int start, int end) {
            this.ipStart = start;
            this.ipEnd = end;
        }

        public void run() {
            //get local ip address
            int t_localip[] = new int[4];
            byte t_target[] = new byte[4];
            for (int i = 0; i < 4; i++) {
                t_localip[i] = ((_IPAddr >> (i * 8)) & 0x000000ff);
                t_target[i] = (byte) t_localip[i];
            }
            String t_lan = t_localip[0] + "." + t_localip[1] + "." + t_localip[2] + ".";
            String t_host = null;
            //check host
            for (int ip = ipStart; ip <= ipEnd; ip++) {
                t_target[3] = (byte) ip;
                t_host = t_lan + ip;
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByAddress(t_target), 4215), 80);
                    if (socket != null) {
                        socket.close();
                    }
                    _QHLock.lock();
                    t_ipArray.add(t_host);
                    _QHLock.unlock();
                } catch (Exception e) {
                }
            }
            _QHLock.lock();
            _QHRunnableCnt--;
            _QHLock.unlock();
        }
    }

    ;

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

    /////////////////////////////////////

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int ilevel = intent.getIntExtra("level", 0);
                int iScale = intent.getIntExtra("scale", 100);
                _Battery = (byte) ((ilevel * 100) / iScale);
                //Log.i(_TAG, "battery level: " + _Battery);
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP: {
                _TouchStatus = 0x00;
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                _TouchX[0] = event.getX();
                _TouchY[0] = event.getY();
            }
            case MotionEvent.ACTION_POINTER_1_UP:
            case MotionEvent.ACTION_POINTER_2_UP:
            case MotionEvent.ACTION_POINTER_3_UP:
            case MotionEvent.ACTION_POINTER_1_DOWN:
            case MotionEvent.ACTION_POINTER_2_DOWN:
            case MotionEvent.ACTION_POINTER_3_DOWN: {
                for (int i = 0; i < _TouchMaxCount; i++) {
                    int p = event.findPointerIndex(i);
                    if (p != -1) {
                        _TouchStatus |= (byte) (0x0100 >> (8 - i));
                    } else {
                        _TouchStatus &= (byte) (0xFEFF >> (8 - i));
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < _TouchMaxCount; i++) {
                    int p = event.findPointerIndex(i);
                    if (p != -1) {
                        _TouchStatus |= (byte) (0x0100 >> (8 - i));
                        _TouchX[i] = event.getX(p);
                        _TouchY[i] = event.getY(p);
                        //Log.i("onTouchEvent", "index: " + i + "_TouchX: " + _TouchX[i] + "_TouchY: " + _TouchY[i]);
                    } else {
                        _TouchStatus &= (byte) (0xFEFF >> (8 - i));
                    }
                }
                break;
            }
        }
        //Log.i("onTouchEvent", "_TouchStatus: " + _TouchStatus);

        if (detector.onTouchEvent(event)) {
            return detector.onTouchEvent(event);
        } else {
            return super.onTouchEvent(event);
        }
    }

    ;

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

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
            _Acc[0] = event.values[0];
            _Acc[1] = event.values[1];
            _Acc[2] = event.values[2];
        }
        // 接受陀螺仪传感器的类型
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            _Gyro[0] = event.values[0];
            _Gyro[1] = event.values[1];
            _Gyro[2] = event.values[2];
        }
    }

    public class myGestureListener implements GestureDetector.OnGestureListener {
        public boolean onDown(MotionEvent e) {return false;}

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {return false;}

        public void onLongPress(MotionEvent e) {
            if (_RemoteMode == 0x10) {
                bcExit();
                InitUI((byte) 0x00);
            }
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {return false;}

        public void onShowPress(MotionEvent e) {}

        public boolean onSingleTapUp(MotionEvent e) {
            _SingleTap = true;
            return false;
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
                            LogUtils.i("Send PS report");
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
                            RemoteSend0x70Report();
                            if (tRequestCount > 10) {
                                DisconnectHost();
                            } //timeout invalid host
                        }

                        while (_TReadTEnable && _ServEnable) {
                            try {
                                byte[] buffer = new byte[1500];
                                int rlen = _Din.read(buffer);

                                //								if (rlen > 22)		//save print screen data to temp file
                                //								{
                                //									if (_PrintScreenDataLen > 0) 
                                //									{
                                //										try 
                                //										{									
                                //											int mode =  (_PSDataCount == 0) ? Activity.MODE_PRIVATE : Activity.MODE_APPEND;
                                //											FileOutputStream mFile = openFileOutput ("printscreen.jpg", mode);
                                //											mFile.write(buffer, 0, rlen);
                                //											mFile.flush();
                                //											mFile.close();
                                //										} 
                                //										catch (FileNotFoundException e) {e.printStackTrace();} 
                                //										catch (IOException e) {e.printStackTrace();}    	
                                //
                                //										_PSDataCount += rlen;
                                //										//Log.i(_TAG, "PS data rec: " + _PSDataCount + "total: " + _PrintScreenDataLen);
                                //										if (_PrintScreenDataLen == _PSDataCount)		//print screen data finished
                                //										{
                                //											//CopyFileFromLocalToSDcard ("printscreen.jpg", "printscreen.jpg", true);
                                //											//SetTouchBG (true);
                                //											_PrintScreenRefresh = true;
                                //											Log.i(_TAG, "PS data rec finished: " + _PSDataCount);
                                //											_PrintScreenDataLen = 0;
                                //											_PSDataCount = 0;
                                //										}
                                //									}
                                //								}
                                //								else 
                                if (rlen > 0) {
                                    RemoteParseReport(buffer, rlen);
                                } else {
                                    break;
                                }
/*								else if (rlen == -1)		
                                {
									Log.e(_TAG, "_ErrCount (read): " + _ErrCount); 
									_ErrCount ++;
									if (_ErrCount > 10) {DisconnectHost(); break;}
								} //host disconnect			
*/
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
                //Log.i(_TAG, "0x2f, flag1");
                //if (len != 17)				break;
                //Log.i(_TAG, "0x2f, flag2");
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

    private boolean RemoteSend0x4aReport() {
        int curlen = 0;

        byte data[] = new byte[_OutputReportLen];

        //rpt id
        data[curlen] = 0x4a;
        curlen += 1;

        //for length
        curlen += 2;

        //HWVer, SWVer
        data[curlen] = (byte) ((_HWVer >> 8) & 0x000000ff);
        data[curlen + 1] = (byte) ((_HWVer) & 0x000000ff);
        data[curlen + 2] = (byte) ((_SWVer >> 8) & 0x000000ff);
        data[curlen + 3] = (byte) ((_SWVer) & 0x000000ff);
        curlen += 4;

        //MAC addr
        for (int i = 0; i < 6; i++) {
            data[curlen + i] = _MacAddr[i];
        }
        curlen += 6;

        //battery
        data[curlen] = _Battery;
        curlen += 1;

        //cycdata
        data[curlen] = (byte) _Cycdata;
        _Cycdata++;
        if (_Cycdata > 255) {
            _Cycdata = 0;
        }
        curlen += 1;

        //time
        long tNow = System.currentTimeMillis();
        data[curlen] = (byte) ((tNow >> 8) & 0x0000000f);
        data[curlen + 1] = (byte) ((tNow) & 0x000000ff);
        curlen += 2;

        //ability
        data[curlen] = 0x00;
        data[curlen + 1] = _Ability;
        curlen += 2;

        //button
        data[curlen] = 0x00;
        data[curlen + 1] = _Btn;
        if (_SingleTap) {
            if (_RemoteMode == 0x02) {
                data[curlen + 1] |= 0x02;
            }
            _SingleTap = false;
        }
        if (_SetBack) {
            data[curlen + 1] |= 0x08;
            _SetBack = false;
        }
        curlen += 2;

        int tmp;

        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Acc[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Gyro[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Mag[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

        for (int i = 0; i < 3; i++) {
            tmp = (int) (_Ori[i] * 100000);
            data[curlen] = (byte) ((tmp >> 24) & 0x000000ff);
            data[curlen + 1] = (byte) ((tmp >> 16) & 0x000000ff);
            data[curlen + 2] = (byte) ((tmp >> 8) & 0x000000ff);
            data[curlen + 3] = (byte) ((tmp) & 0x000000ff);
            curlen += 4;
        }

        data[curlen] = (byte) _TouchStatus;
        curlen += 1;

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
                //Log.i("load file", "data: " + imsg);
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

    private void ShowKeyboard(boolean enable) {
        InputMethodManager _IMM = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!enable) {
            _IMM.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
            _IMM.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else {
            _IMM.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    /*
    @SuppressLint("SdCardPath")
	private void CopyFileFromLocalToSDcard (String fFile, String tFile, Boolean rewrite)
    {
		File toFile		=new File(Environment.getExternalStorageDirectory(), tFile);

    	if (!toFile.getParentFile().exists()) 	{toFile.getParentFile().mkdirs();}
    	if (toFile.exists() && rewrite) 		{toFile.delete();}

    	try {
    		FileInputStream fosfrom = openFileInput (fFile);
    		java.io.FileOutputStream fosto = new FileOutputStream(toFile);
    		
    		byte bt[] = new byte[1024];
    		//int count = 0;
    		int c;
    		while ((c = fosfrom.read(bt)) > 0) {
    			fosto.write(bt, 0, c); //将内容写到新文件当中
        		//count += c;
    		}
    		//Log.i(_TAG, "CopyFileFromLocalToSDcard: count = " + count);
    		fosfrom.close();
    		fosto.close();
    	} 
    	catch (Exception ex) {Log.e("readfile", ex.getMessage());}
    }    
*/
    private void SetTouchBG(Boolean reset) {
        View background;
        background = (View) findViewById(R.id.layout_mw_bg);

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
    }

    //////////////////////////////zxing////////////////////////
    public Handler getHandler() {
        return handler;
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void handleDecode(Result obj, Bitmap barcode) {
        inactivityTimer.onActivity();
        //viewfinderView.drawResultBitmap(barcode);
        if (ipCheck(obj.getText())) {
            LogUtils.i(obj.getBarcodeFormat().toString() + ":" + obj.getText());
            bcExit();
            InitUI((byte) 0x00);

            _TargetIpAddr = obj.getText();
            Thread thread = new Thread(new _RConnectHost(_TargetIpAddr));
            thread.start();
        } else {
            LogUtils.i("restart - " + obj.getBarcodeFormat().toString() + ":" + obj.getText());
            bcExit();
            bcInit();
        }
    }

    private boolean ipCheck(String ipa) {
        //String		ip;
        //ip = strQR2;

        //Log.i(_TAG, "ipinput: " + ip);

        Pattern patt = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        Matcher mat = patt.matcher(ipa);

        if (mat.matches())            //error1
        {
            int ipaddr[] = new int[4];
            int position1 = ipa.indexOf(".");
            int position2 = ipa.indexOf(".", position1 + 1);
            int position3 = ipa.indexOf(".", position2 + 1);

            ipaddr[0] = (int) Long.parseLong(ipa.substring(0, position1));
            ipaddr[1] = (int) Long.parseLong(ipa.substring(position1 + 1, position2));
            ipaddr[2] = (int) Long.parseLong(ipa.substring(position2 + 1, position3));
            ipaddr[3] = (int) Long.parseLong(ipa.substring(position3 + 1));

            boolean err = false;
            for (int i = 0; i < 4; i++) {
                if ((ipaddr[i] > 255) || (ipaddr[i] < 0))        //error2
                {
                    err = true;
                    break;
                }
            }
            if (!err) {
                return true;
            }
        }
        return false;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }

        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
        }
    }

    private void bcInit() {
        CameraManager.init(getApplication());

        inactivityTimer = new InactivityTimer(this);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);

        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();

        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private void bcExit() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
        inactivityTimer.shutdown();
    }


}
