package com.motiongestures.touchgesturesexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.motiongestures.touchgesturesexample.events.TestGestureDrawingFinishedEvent;
import com.motiongestures.touchgesturesexample.events.TestGestureDrawingFinishedListener;
import com.motiongestures.touchgesturesexample.events.TouchGesturePoint;

import java.util.ArrayList;
import java.util.List;

public class TestTouchGestureView extends View {
    private static String TAG = "TestTouchGestureView";
    private static float TOUCH_TOLERANCE = 4;
    private static int CIRCLE_RADIUS = 10;

    private final Path gesturePath;
    private final Paint circlePaint;
    private final Path circlePath;
    private final Paint gesturePaint;
    private Paint gridPaint;
    private Path gridPath;
    private int width;
    private int height;
    private int widthSteps;
    private int heightSteps;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint;
    private float mX;
    private float mY;
    private TestGestureDrawingFinishedListener listener;
    private List<TouchGesturePoint> points = new ArrayList<>();

    public TestTouchGestureView(Context context, @Nullable AttributeSet attributeSet) {
        super(context,attributeSet);

        gesturePath = new Path();
        circlePaint = new Paint();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);
        gesturePaint = new Paint();
        gesturePaint.setAntiAlias(true);
        gesturePaint.setDither(true);
        gesturePaint.setColor(Color.GREEN);
        gesturePaint.setStyle(Paint.Style.STROKE);
        gesturePaint.setStrokeJoin(Paint.Join.ROUND);
        gesturePaint.setStrokeCap(Paint.Cap.ROUND);
        gesturePaint.setStrokeWidth(12);

        gridPath = new Path();
        gridPaint = new Paint();
        gridPaint.setAntiAlias(true);
        gridPaint.setDither(true);
        gridPaint.setStrokeJoin(Paint.Join.MITER);
        gridPaint.setStrokeCap(Paint.Cap.SQUARE);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10,5},0));

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG,"on size changed:w="+w+",h="+h);
        width = w;
        height = h;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        widthSteps = width/5;
        heightSteps = height/5;

        generateGridPath();
    }

    private void generateGridPath() {
        gridPath.reset();
        for(int i=0;i<=height;i+=heightSteps) {
            gridPath.moveTo(0,i);
            gridPath.lineTo(width,i);
        }

        for(int i=0;i<=width;i+=widthSteps) {
            gridPath.moveTo(i,0);
            gridPath.lineTo(i,height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(gesturePath, gesturePaint);
        canvas.drawPath( circlePath,  circlePaint);
        canvas.drawPath(gridPath,gridPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int size = event.getHistorySize();
        for(int i=0;i<size;i++) {
            handleTouch(event.getAction(),event.getHistoricalX(i),event.getHistoricalY(i));
        }
        float x = event.getX();
        float y = event.getY();
        return handleTouch(event.getAction(), x,y);
    }

    private boolean handleTouch(int action,float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                if(listener != null) {
                    listener.drawingFinished(new TestGestureDrawingFinishedEvent(this,points));
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                clearDrawing();
                invalidate();
                break;
        }
        return true;
    }

    private void touchStart(float x, float y) {
        Log.d(TAG,"Touch start");
        mX = x;
        mY = y;
        clearDrawing();
        gesturePath.moveTo(x, y);
        points.add(new TouchGesturePoint(x,y));
        circlePath.addCircle(mX, mY, CIRCLE_RADIUS, Path.Direction.CW);
    }

    private void touchMove(float x, float y) {
        Log.d(TAG,"Touch move");
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            points.add(new TouchGesturePoint(x,y));
            gesturePath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
            // circlePath.reset();
            circlePath.addCircle(mX, mY, CIRCLE_RADIUS, Path.Direction.CW);
        }
    }

    private void touchUp() {
        Log.d(TAG,"Touch up");
        gesturePath.lineTo(mX, mY);
        //TouchGesturePoint point = gesture.addPoint(mX,mY);

        // commit the path to our offscreen
        mCanvas.drawPath(gesturePath, gesturePaint);
        mCanvas.drawPath(circlePath, circlePaint);
        // kill this so we don't double draw
        gesturePath.reset();
        circlePath.reset();
    }

    public void clearDrawing() {
        Log.d(TAG,"clear drawing");
        gesturePath.reset();
        circlePath.reset();
        mCanvas.drawColor(Color.WHITE);
        points.clear();
        invalidate();
    }

    public TestGestureDrawingFinishedListener getListener() {
        return listener;
    }

    public void setListener(TestGestureDrawingFinishedListener listener) {
        this.listener = listener;
    }
}
