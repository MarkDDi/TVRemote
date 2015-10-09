package com.winside.tvremote.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.winside.tvremote.R;
import com.winside.tvremote.util.LogUtils;

/**
 * Author        : lu
 * Data          : 2015/9/24
 * Time          : 17:26
 * Decription    :
 */
public class GameTouchMode extends ImageView implements GestureDetector.OnGestureListener {

    private SparseArray<TouchHistory> mTouches;
    private float mCircleRadius;
    private float mCircleHistoricalRadius;
    private Paint mCirclePaint = new Paint();
    private boolean mHasTouch = false;
    // radius of active touch circle in dp default 75f
    private static final float CIRCLE_RADIUS_DP = 20f;
    // radius of historical circle in dp default 7f
    private static final float CIRCLE_HISTORICAL_RADIUS_DP = 7f;
    public final int[] COLORS = {0xFF33B5E5, 0xFFAA66CC, 0xFF99CC00, 0xFFFFBB33, 0xFFFF4444, 0xFF0099CC, 0xFF9933CC, 0xFF669900, 0xFFFF8800, 0xFFCC0000};
    //多点触摸
    private static int _TouchMaxCount = 1;

    private MyGameGestureListener myGameGestureListener;
    private GestureDetector detector;

    public void setGestureListener(MyGameGestureListener myGameGestureListener) {
        this.myGameGestureListener = myGameGestureListener;
    }

    public GameTouchMode(Context context) {
        super(context);
        // SparseArray for touch events, indexed by touch id
        mTouches = new SparseArray<TouchHistory>(10);
        initialize();
    }

    public GameTouchMode(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(R.styleable.GameTouchMode);

        float aradius_percent = typedArray.getFloat(R.styleable.GameTouchMode_percent, 100f);
        float aradius_percent_ok = typedArray.getFloat(R.styleable.GameTouchMode_percent_ok, 20f);

        typedArray.recycle();
        // SparseArray for touch events, indexed by touch id
        mTouches = new SparseArray<TouchHistory>(10);
        initialize();
    }

    private void initialize() {
        // 初始化触摸paint
        float density = getResources().getDisplayMetrics().density;
        // 手指当前圆的大小
        mCircleRadius = CIRCLE_RADIUS_DP * density;
        // 手指之前位置圆的大小
        mCircleHistoricalRadius = CIRCLE_HISTORICAL_RADIUS_DP * density;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        //
        myGameGestureListener.onSingleTapUp();
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void setDetector(GestureDetector detector) {
        this.detector = detector;
    }

    static final class TouchHistory {

        // number of historical points to store
        public static final int HISTORY_COUNT = 10;

        public float x;
        public float y;
        public float pressure = 0f;

        // current position in history array
        public int historyIndex = 0;
        public int historyCount = 0;

        // arrray of pointer position history
        public PointF[] history = new PointF[HISTORY_COUNT];

        private static final int MAX_POOL_SIZE = 10;
        private static final Pools.SimplePool<TouchHistory> sPool = new Pools.SimplePool<TouchHistory>(MAX_POOL_SIZE);

        public static TouchHistory obtain(float x, float y, float pressure) {
            TouchHistory data = sPool.acquire();
            if (data == null) {
                data = new TouchHistory();
            }

            data.setTouch(x, y, pressure);

            return data;
        }

        public TouchHistory() {

            // initialise history array
            for (int i = 0; i < HISTORY_COUNT; i++) {
                history[i] = new PointF();
            }
        }

        public void setTouch(float x, float y, float pressure) {
            this.x = x;
            this.y = y;
            this.pressure = pressure;
        }

        public void recycle() {
            this.historyIndex = 0;
            this.historyCount = 0;
            sPool.release(this);
        }

        /**
         * Add a point to its history. Overwrites oldest point if the maximum
         * number of historical points is already stored.
         */
        public void addHistory(float x, float y) {
            PointF p = history[historyIndex];
            p.x = x;
            p.y = y;

            //            LogUtils.e("x = " + x + " y = " + y);

            historyIndex = (historyIndex + 1) % history.length;

            if (historyCount < HISTORY_COUNT) {
                historyCount++;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                int id = event.getPointerId(0);

                //                LogUtils.e("getX() = " + event.getX() + " getX(0) = " + event.getX(0));
                TouchHistory data = TouchHistory.obtain(event.getX(0), event.getY(0), event.getPressure(0));

                //                LogUtils.e("getPressure = " + event.getPressure(0));
                /*
                 * Store the data under its pointer identifier. The pointer
                 * number stays consistent for the duration of a gesture,
                 * accounting for other pointers going up or down.
                 */
                mTouches.put(id, data);

                mHasTouch = true;

                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < _TouchMaxCount; i++) {
                    int p = event.findPointerIndex(i);
                    myGameGestureListener.acitonMove(p, i, event);
                }
                for (int index = 0; index < event.getPointerCount(); index++) {
                    // get pointer id for data stored at this index
                    int id_ = event.getPointerId(0);
                    // get the data stored externally about this pointer.
//                    TouchHistory history = mTouches.get(id_);
                    TouchHistory history = mTouches.get(0);

                    // add previous position to history and add new values
                    history.addHistory(history.x, history.y);
//                    history.setTouch(event.getX(index), event.getY(index), event.getPressure(index));
                    history.setTouch(event.getX(0), event.getY(0), event.getPressure(0));

                }

                break;

            case MotionEvent.ACTION_UP:
                // 回调
                myGameGestureListener.onTouchStatus();
                //                LogUtils.e("SoftDpad 触发ACTION_UP");
                int id_ = event.getPointerId(0);
//                TouchHistory touchHistory = mTouches.get(id_);
                TouchHistory touchHistory = mTouches.get(0);
                mTouches.remove(0);
                touchHistory.recycle();

                mHasTouch = false;
                break;

            case MotionEvent.ACTION_POINTER_1_UP:
            case MotionEvent.ACTION_POINTER_2_UP:
            case MotionEvent.ACTION_POINTER_3_UP:
            case MotionEvent.ACTION_POINTER_1_DOWN:
            case MotionEvent.ACTION_POINTER_2_DOWN:
            case MotionEvent.ACTION_POINTER_3_DOWN:
                for (int i = 0; i < _TouchMaxCount; i++) {
                    int p = event.findPointerIndex(i);
                    myGameGestureListener.mulTouch(p, i);
                }
                break;
        }
        // trigger redraw on UI thread
        this.postInvalidate();
        // 游戏触摸屏模式的点击事件
        if (detector.onTouchEvent(event)) {
            return detector.onTouchEvent(event);
        } else {
            return true;
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        //        canvas.translate(offsetX, offsetY);
        super.onDraw(canvas);

        int id = 0;
        // 单点触控
        if (mTouches.size() > 0) {
            id = mTouches.keyAt(0);
            TouchHistory data = mTouches.valueAt(0);

            // draw the data and its history to the canvas
            drawCircle(canvas, id, data);
        }

    }

    protected void drawCircle(Canvas canvas, int id, TouchHistory data) {
        // select the color based on the id
        int color = COLORS[id % COLORS.length];
        mCirclePaint.setColor(color);


        float pressure = Math.min(data.pressure, 1f);
        float radius = pressure * mCircleRadius;

        //        LogUtils.e("data.x = " + data.x + " data.y = " + data.y + " radius = " + radius);
        canvas.drawCircle(data.x, (data.y) - (radius / 2f), radius, mCirclePaint);

        // draw all historical points with a lower alpha value
        mCirclePaint.setAlpha(125);
        for (int j = 0; j < data.history.length && j < data.historyCount; j++) {
            PointF p = data.history[j];
            canvas.drawCircle(p.x, p.y, mCircleHistoricalRadius, mCirclePaint);
        }
    }


    public interface MyGameGestureListener {

        void onSingleTapUp();

        void onTouchStatus();

        void mulTouch(int pointer, int current);

        void acitonMove(int pointer, int current, MotionEvent event);
    }
}
