package com.dj.hrfacelib.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;


import androidx.annotation.Nullable;
import com.dj.hrfacelib.util.DrawHelper;
import com.dj.hrfacelib.util.DrawInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FaceRectView extends View {
    private static final String TAG = "FaceRectView";
    private CopyOnWriteArrayList<DrawInfo> faceRectList = new CopyOnWriteArrayList<>();

    public FaceRectView(Context context) {
        this(context, null);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faceRectList != null && faceRectList.size() > 0) {
            for (int i = 0; i < faceRectList.size(); i++) {
                DrawHelper.drawFaceRect(canvas, faceRectList.get(i), Color.YELLOW, 5);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取屏幕长宽比例，这样设置不会发生畸变，千万不要根据一个手机设定一个数
        //那样换一个手机就可能会出现显示的比例问题
        int screenWidth = CommonUtils.getScreenWidth(getContext());
        int screenHeight = CommonUtils.getScreenHeight(getContext());
        //可以理解为背景盖住了大部分的区域，我们只能看到圆框里面的，如果还是按照原来的比例绘制surfaceview
        //按比例缩小
        double screenWidth1= 0.60*screenHeight;
        double screenHeight1= 0.60*screenHeight;
        //绘制的输入参数必须是整数型，做浮点型运算后为float型数据，故需要做取整操作

        setMeasuredDimension((int) screenWidth1, (int) screenHeight1);
        //setMeasuredDimension(widthSize, heightSize);
    }

    public void clearFaceInfo() {
        faceRectList.clear();
        postInvalidate();
    }

    public void addFaceInfo(DrawInfo faceInfo) {
        faceRectList.add(faceInfo);
        postInvalidate();
    }

    public void addFaceInfo(List<DrawInfo> faceInfoList) {
        faceRectList.addAll(faceInfoList);
        postInvalidate();
    }
}