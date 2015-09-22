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

package com.winside.tvremote.widget;

import com.winside.tvremote.ConstValues;
import com.winside.tvremote.R;
import com.winside.tvremote.util.LogUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.widget.ImageView;

/**
 * A widget that imitates a Dpad that would float on top of the UI. Dpad
 * is being simulated as a touch area that recognizes slide gestures or taps
 * for OK.
 * <p>
 * Make sure you set up a {@link DpadListener} to handle the events.
 * <p>
 * To position the dpad on the screen, use {@code paddingTop} or
 * {@code PaddingBottom}. If you use {@code PaddingBottom}, the widget will be
 * aligned on the bottom of the screen minus the padding.
 */
public final class SoftDpad extends ImageView {

    private final SharedPreferences sharedPreferences;

    /**
     * Interface that receives the commands.
     */
    public interface DpadListener {

        /**
         * Called when the Dpad was clicked.
         */
        void onDpadClicked();

        /**
         * Called when the Dpad was moved in a given direction, and with which
         * action (pressed or released).
         * @param direction the direction in which the Dpad was moved
         * @param pressed {@code true} to represent an event down
         */
        void onDpadMoved(Direction direction, boolean pressed);
    }

    /**
     * Tangent of the angle used to detect a direction.
     * <p>
     * The angle should be less that 45 degree. Pre-calculated for performance.
     */
    private static final double TAN_DIRECTION = Math.tan(Math.PI / 4);

    /**
     * Different directions where the Dpad can be moved.
     */
    public enum Direction {
        /**
         * @hide
         */
        IDLE(false),
        CENTER(false),
        RIGHT(true),
        LEFT(true),
        UP(true),
        DOWN(true);

        final boolean isMove;

        Direction(boolean isMove) {
            this.isMove = isMove;
        }
    }

    /**
     * Coordinates of the center of the Dpad in its initial position.
     */
    private int centerX;
    private int centerY;

    /**
     * Current dpad image offset.
     */
    private int offsetX;
    private int offsetY;

    /**
     * Radius of the Dpad's touchable area.
     */
    private int radiusTouchable;

    /**
     * Radius of the area around touchable area where events get caught and ignored.
     */
    private int radiusIgnore;

    /**
     * Percentage of half of drawable's width that is the radius.
     */
    private float radiusPercent;

    /**
     * OK area expressed as percentage of half of drawable's width.
     */
    private float radiusPercentOk;

    /**
     * Radius of the area handling events, should be &gt;= {@code radiusPercent}
     */
    private float radiusPercentIgnore;

    /**
     * Coordinates of the first touch event on a sequence of movements.
     */
    private int originTouchX;
    private int originTouchY;

    /**
     * Touch bounds.
     */
    private int clickRadiusSqr;

    /**
     * {@code true} if the Dpad is capturing the events.
     */
    private boolean isDpadFocused;

    /**
     * Direction in which the DPad is, or {@code null} if idle.
     */
    private Direction dPadDirection;

    private DpadListener listener;

    /**
     * Vibrator. 振动器
     */
    private final Vibrator vibrator;

    public SoftDpad(Context context, AttributeSet attrs) {
        super(context, attrs);

        sharedPreferences = getContext().getSharedPreferences(ConstValues.settings, Context.MODE_PRIVATE);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.SoftDpad);

        try {
            radiusPercent = a.getFloat(R.styleable.SoftDpad_radius_percent, 100.0f);
            radiusPercentOk = a.getFloat(R.styleable.SoftDpad_radius_percent_ok, 20.0f);
            radiusPercentIgnore = a.getFloat(R.styleable.SoftDpad_radius_percent_ignore_touch, radiusPercent);

            if (radiusPercentIgnore < radiusPercent) {
                throw new IllegalStateException("Ignored area smaller than touchable area");
            }
        } finally {
            a.recycle();
        }

        // SparseArray for touch events, indexed by touch id
        mTouches = new SparseArray<TouchHistory>(10);

        initialize();
    }
    private float mCircleRadius;
    private float mCircleHistoricalRadius;
    private Paint mCirclePaint = new Paint();

    // radius of active touch circle in dp default 75f
    private static final float CIRCLE_RADIUS_DP = 20f;
    // radius of historical circle in dp default 7f
    private static final float CIRCLE_HISTORICAL_RADIUS_DP = 7f;
    public final int[] COLORS = {0xFF33B5E5, 0xFFAA66CC, 0xFF99CC00, 0xFFFFBB33, 0xFFFF4444, 0xFF0099CC, 0xFF9933CC, 0xFF669900, 0xFFFF8800, 0xFFCC0000};

    private SparseArray<TouchHistory> mTouches;

    // Is there an active touch?
    private boolean mHasTouch = false;


    private void initialize() {
        isDpadFocused = false;
        setScaleType(ScaleType.CENTER_INSIDE);
        dPadDirection = Direction.IDLE;

        // Calculate radiuses in px from dp based on screen density
        // 初始化触摸paint
        float density = getResources().getDisplayMetrics().density;
        // 手指当前圆的大小
        mCircleRadius = CIRCLE_RADIUS_DP * density;
        // 手指之前位置圆的大小
        mCircleHistoricalRadius = CIRCLE_HISTORICAL_RADIUS_DP * density;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public void setDpadListener(DpadListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        prepare();
    }

    /**
     * Initializes the widget. Must be called after the view has been inflated.
     */
    public void prepare() {
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        radiusTouchable = (int) (radiusPercent * w / 200);
        radiusIgnore = (int) (radiusPercentIgnore * w / 200);
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        clickRadiusSqr = (int) (radiusPercentOk * w / 200);
        clickRadiusSqr *= clickRadiusSqr;

        center();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                LogUtils.e("SoftDpad 触发ACTION_DOWN");
                if (isEventOutsideIgnoredArea(x, y)) {
                    return false;
                }
                if (!isEventInsideTouchableArea(x, y)) {
                    return true;
                }
                handleActionDown(x, y);

                int id = event.getPointerId(0);

//                LogUtils.e("getX() = " + event.getX() + " getX(0) = " + event.getX(0));
                TouchHistory data = TouchHistory.obtain(event.getX(0), event.getY(0), event.getPressure(0));

                LogUtils.e("getPressure = " + event.getPressure(0));
                /*
                 * Store the data under its pointer identifier. The pointer
                 * number stays consistent for the duration of a gesture,
                 * accounting for other pointers going up or down.
                 */
                mTouches.put(id, data);

                mHasTouch = true;

               break;

            case MotionEvent.ACTION_MOVE:
//                LogUtils.e("SoftDpad 触发ACTION_MOVE");
                if (isDpadFocused) {
                    handleActionMove(x, y);

                    for (int index = 0; index < event.getPointerCount(); index++) {
                        // get pointer id for data stored at this index
                        int id_ = event.getPointerId(0);
                        // get the data stored externally about this pointer.
                        TouchHistory history = mTouches.get(id_);

                        // add previous position to history and add new values
                        history.addHistory(history.x, history.y);
                        history.setTouch(event.getX(index), event.getY(index), event.getPressure(index));

                    }
                }
                break;

            case MotionEvent.ACTION_UP:
//                LogUtils.e("SoftDpad 触发ACTION_UP");
                if (isDpadFocused) {
                    handleActionUp(x, y);
                    int id_ = event.getPointerId(0);
                    TouchHistory touchHistory = mTouches.get(id_);
                    mTouches.remove(0);
                    touchHistory.recycle();

                    mHasTouch = false;
                }
                break;
        }
        // trigger redraw on UI thread
        this.postInvalidate();
        return true;
    }

    private void handleActionDown(int x, int y) {
        dPadDirection = Direction.IDLE;
        isDpadFocused = true;
        originTouchX = x;
        originTouchY = y;
    }

    private void handleActionMove(int x, int y) {
        int dx = x - originTouchX;
        int dy = y - originTouchY;

        Direction move = getDirection(dx, dy);
        if (move.isMove && !dPadDirection.isMove) {
            // 开始振动
            sendEvent(move, true, true);
            dPadDirection = move;
        }
    }

    private void handleActionUp(int x, int y) {
        boolean playSound = true;
        handleActionMove(x, y);
        if (dPadDirection.isMove) {
            sendEvent(dPadDirection, false, playSound);
        } else {
            onCenterAction();
        }
        center();
    }

    /**
     * Centers the Dpad.
     */
    private void center() {
        isDpadFocused = false;
        dPadDirection = Direction.IDLE;
    }

    /**
     * Quickly dismiss a touch event if it's not in a square around the idle
     * position of the Dpad.
     * <p>
     * May return {@code false} for an event outside the DPad.
     * @param x x-coordinate form the top left of the screen
     * @param y y-coordinate form the top left of the screen
     * @param r radius of the circle we are testing
     * @return {@code true} if event is outside the Dpad
     */
    private boolean quickDismissEvent(int x, int y, int r) {
        return (x < getCenterX() - r ||
                x > getCenterX() + r ||
                y < getCenterY() - r ||
                y > getCenterY() + r);
    }

    /**
     * Returns {@code true} if the touch event is outside a circle centered on the
     * idle position of the Dpad and of a given radius
     * @param x x-coordinate of the touch event form the top left of the screen
     * @param y y-coordinate of the touch event form the top left of the screen
     * @param r radius of the circle we are testing.
     * @return {@code true} if event is outside designated zone
     */
    private boolean isEventOutside(int x, int y, int r) {
        if (quickDismissEvent(x, y, r)) {
            return true;
        }
        int dx = (x - getCenterX()) * (x - getCenterX());
        int dy = (y - getCenterY()) * (y - getCenterY());
        return (dx + dy) > r * r;
    }

    /**
     * Returns {@code true} if the touch event is outside the touchable area
     * where the Dpad handles events.
     * @param x x-coordinate form the top left of the screen
     * @param y y-coordinate form the top left of the screen
     * @return {@code true} if outside
     */
    public boolean isEventOutsideIgnoredArea(int x, int y) {
        return isEventOutside(x, y, radiusIgnore);
    }

    /**
     * Returns {@code true} if the touch event is outside the area where the Dpad
     * is when idle, the touchable area.
     * @param x x-coordinate form the top left of the screen
     * @param y y-coordinate form the top left of the screen
     * @return {@code true} if outside
     */
    public boolean isEventInsideTouchableArea(int x, int y) {
        return !isEventOutside(x, y, radiusTouchable);
    }

    /**
     * Returns {@code true} if the dpad has moved enough from its idle position to
     * stop being interpreted as a click.
     * @param dx movement along the x-axis from the idle position
     * @param dy movement along the y-axis from the idle position
     * @return {@code true} if not a click event
     */
    private boolean isClick(int dx, int dy) {
        return (dx * dx + dy * dy) < clickRadiusSqr;
    }

    /**
     * Returns a direction for the movement.
     * @param dx x-coordinate form the idle position of Dpad
     * @param dy y-coordinate form the idle position of Dpad
     * @return a direction, or unknown if the direction is not clear enough
     */
    private Direction getDirection(int dx, int dy) {
        if (isClick(dx, dy)) {
            return Direction.CENTER;
        }
        if (dx == 0) {
            if (dy > 0) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        }
        if (dy == 0) {
            if (dx > 0) {
                return Direction.RIGHT;
            } else {
                return Direction.LEFT;
            }
        }
        float ratioX = (float) (dy) / (float) (dx);
        float ratioY = (float) (dx) / (float) (dy);
        if (Math.abs(ratioX) < TAN_DIRECTION) {
            if (dx > 0) {
                return Direction.RIGHT;
            } else {
                return Direction.LEFT;
            }
        }
        if (Math.abs(ratioY) < TAN_DIRECTION) {
            if (dy > 0) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        }
        return Direction.CENTER;
    }

    /**
     * Sends a DPad event if the Dpad is in the right position.
     * @param move the direction in witch the event should be sent.
     * @param pressed {@code true} if touch just begun.
     * @param playSound {@code true} if click sound should be played.
     */
    private void sendEvent(Direction move, boolean pressed, boolean playSound) {
        if (listener != null) {
            switch (move) {
                case UP:
                case DOWN:
                case LEFT:
                case RIGHT:
                    listener.onDpadMoved(move, pressed);
                    if (playSound) {
                        if (pressed) {
                            // 开始手机振动
                            if (sharedPreferences.getBoolean(ConstValues.vibrator, true)) {
                                vibrator.vibrate(getResources().getInteger(com.winside.tvremote.R.integer.dpad_vibrate_time));
                            }
                        }
                        playSound();
                    }
            }
        }
    }

    /**
     * Actions performed when the user click on the Dpad.
     */
    private void onCenterAction() {
        if (listener != null) {
            listener.onDpadClicked();
            // 点击中间OK键开始振动,持续时间为40毫秒
            vibrator.vibrate(getResources().getInteger(com.winside.tvremote.R.integer.dpad_vibrate_time));
            playSound();
        }
    }

    /**
     * Plays a sound when sending a key.
     */
    private void playSound() {
        playSoundEffect(SoundEffectConstants.CLICK);
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

    /**
     * Holds data related to a touch pointer, including its current position,
     * pressure and historical positions. Objects are allocated through an
     * object pool using {@link ()} and {@link #recycle()} to reuse
     * existing objects.
     */
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
}
