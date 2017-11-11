package com.vrinda.rider;

import io.realm.RealmObject;

public class RideInfo extends RealmObject {
    private double speed;
    private double maxSpeed;
    private double avgSpeed;
    private double distance;
    private long startTime;
    private double curLocationLat;
    private double curLocationLang;
    private boolean isIdle;
    private long idleStartTime;
    private boolean isActive;

    public RideInfo() {
        init();
    }

    public void init() {
        setSpeed(0);
        setMaxSpeed(0);
        setAvgSpeed(0);
        setDistance(0);
        setStartTime(System.currentTimeMillis());
        setCurLocationLang(0);
        setCurLocationLat(0);
        setIdle(true);
        setIdleStartTime(System.currentTimeMillis());
        setActive(false);
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getCurLocationLat() {
        return curLocationLat;
    }

    public void setCurLocationLat(double curLocationLat) {
        this.curLocationLat = curLocationLat;
    }

    public double getCurLocationLang() {
        return curLocationLang;
    }

    public void setCurLocationLang(double curLocationLang) {
        this.curLocationLang = curLocationLang;
    }

    public boolean isIdle() {
        return isIdle;
    }

    public void setIdle(boolean idle) {
        isIdle = idle;
    }

    public long getIdleStartTime() {
        return idleStartTime;
    }

    public void setIdleStartTime(long idleStartTime) {
        this.idleStartTime = idleStartTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
