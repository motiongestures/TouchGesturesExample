package com.motiongestures.touchgesturesexample.events;

import android.view.View;

import java.util.List;

public class TestGestureDrawingFinishedEvent {
    private View source;
    private List<TouchGesturePoint> points;

    public TestGestureDrawingFinishedEvent(View source, List<TouchGesturePoint> points) {
        this.source = source;
        this.points = points;
    }

    public TestGestureDrawingFinishedEvent() {
    }

    public View getSource() {
        return source;
    }

    public void setSource(View source) {
        this.source = source;
    }

    public List<TouchGesturePoint> getPoints() {
        return points;
    }

    public void setPoints(List<TouchGesturePoint> points) {
        this.points = points;
    }
}
