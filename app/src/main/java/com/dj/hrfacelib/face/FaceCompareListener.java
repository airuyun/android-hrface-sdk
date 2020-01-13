package com.dj.hrfacelib.face;

import android.graphics.Bitmap;

public interface FaceCompareListener {
    /**
     * 当出现异常时执行
     *
     * @param e 异常信息
     */
    void onCompareFail(Exception e);


    /**
     * 比对结果
     * @param cBtm 抓拍照
     * @param pBtm 身份证照
     * @param score 比对分数
     */
    void  onFaceCompareInfoGet(Bitmap cBtm, Bitmap pBtm, Float score);
}
