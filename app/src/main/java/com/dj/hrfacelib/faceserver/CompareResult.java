package com.dj.hrfacelib.faceserver;

public class CompareResult {
    private String userName;
    private String userNum;
    private String userPath;
    private String userId;

    private float similar;
    private int trackId;

    public CompareResult(String userName, String num , float similar) {
        this.userName = userName;
        this.similar = similar;
        this.userNum = num;
    }

    public CompareResult(String userName, String userNum, String userPath,String userId, float similar) {
        this.userName = userName;
        this.userNum = userNum;
        this.userPath = userPath;
        this.similar = similar;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPath() {
        return userPath;
    }

    public void setUserPath(String userPath) {
        this.userPath = userPath;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getSimilar() {
        return similar;
    }

    public void setSimilar(float similar) {
        this.similar = similar;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    @Override
    public String toString() {
        return "CompareResult{" +
                "userName='" + userName + '\'' +
                ", userNum='" + userNum + '\'' +
                ", similar=" + similar +
                ", trackId=" + trackId +
                '}';
    }
}
