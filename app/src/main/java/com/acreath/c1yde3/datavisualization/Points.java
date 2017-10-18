package com.acreath.c1yde3.datavisualization;

/**
 * Created by 10543 on 2017/9/23.
 */

public class Points {
    String time;
    String x;
    String y;
    String z;

    public Points(String time, String x, String y, String z) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getZ() {
        return z;
    }

    public void setZ(String z) {
        this.z = z;
    }
}
