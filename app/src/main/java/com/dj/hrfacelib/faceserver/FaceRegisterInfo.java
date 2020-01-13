package com.dj.hrfacelib.faceserver;

public class FaceRegisterInfo {
    private byte[] featureData;
    private String name;
    private String num;
    private String uid;
    private String path;

    public FaceRegisterInfo(byte[] featureData, String name, String num, String uid, String path) {
        this.featureData = featureData;
        this.name = name;
        this.num = num;
        this.uid = uid;
        this.path = path;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getFeatureData() {
        return featureData;
    }

    public void setFeatureData(byte[] featureData) {
        this.featureData = featureData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "FaceRegisterInfo{" +
                "featureData=" + featureData.length +
                ", name='" + name + '\'' +
                ", num='" + num + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
