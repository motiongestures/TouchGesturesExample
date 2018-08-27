package com.motiongestures.touchgesturesexample.events;

import org.json.JSONException;
import org.json.JSONObject;

public class TouchGesturePoint {
    private float x;
    private float y;

    public TouchGesturePoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public TouchGesturePoint() {
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("x",(double)x);
        obj.put("y",(double)y);
        return obj;
    }
}
