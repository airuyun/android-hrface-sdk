package com.dj.hrfacelib.center;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.dj.hrfacelib.R;
import com.dj.hrfacelib.face.FaceHelper;
import com.dj.hrfacelib.face.FaceListener;
import com.dj.hrfacelib.face.FacePreviewInfo;
import com.dj.hrfacelib.face.RequestFeatureStatus;
import com.dj.hrfacelib.faceserver.CompareResult;
import com.dj.hrfacelib.faceserver.FaceServer;
import com.dj.hrfacelib.util.ConfigUtil;
import com.dj.hrfacelib.util.DeleteFileUtil;
import com.dj.hrfacelib.util.DrawHelper;
import com.dj.hrfacelib.util.DrawInfo;
import com.dj.hrfacelib.util.ImageUtil;
import com.dj.hrfacelib.util.camera.CameraHelper;
import com.dj.hrfacelib.util.camera.CameraListener;
import com.dj.hrfacelib.widget.FaceRectView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public abstract class BaseFaceActivity extends Activity {

    private static final String TAG = "Asion--ARC--";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final int MAX_DETECT_NUM = 5;

    private int afCode = -1;

    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 50;
    public CameraHelper cameraHelper;

    private DrawHelper drawHelper;
    private Camera.Size previewSize;

    private FaceHelper faceHelper;

    private Boolean closeSearchFace = false;

    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();

    /**
     * 优先打开的摄像头
     */
    private Integer cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private FaceEngine faceEngine;
    private List<CompareResult> compareResultList;

    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onStop() {
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideMenu();
        setContentView(setLayout());
        // Activity启动后就锁定为启动时的方向
        switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default:
                break;
        }

        switch (setFaceDectectRotation()) {
            case 0:
                ConfigUtil.setFtOrient(BaseFaceActivity.this, FaceEngine.ASF_OP_0_ONLY);
                break;
            case 90:
                ConfigUtil.setFtOrient(BaseFaceActivity.this, FaceEngine.ASF_OP_90_ONLY);
                break;
            case 180:
                ConfigUtil.setFtOrient(BaseFaceActivity.this, FaceEngine.ASF_OP_180_ONLY);
                break;
            case 270:
                ConfigUtil.setFtOrient(BaseFaceActivity.this, FaceEngine.ASF_OP_270_ONLY);
                break;
            default:
                ConfigUtil.setFtOrient(BaseFaceActivity.this, FaceEngine.ASF_OP_0_HIGHER_EXT);
                break;
        }

        compareResultList = new ArrayList<>();
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initView();
            initEngine();
            initCamera();
        }
    }

    private void hideMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.这种方式虽然是官方推荐，但是根本达不到效果
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
//                Log.e(TAG, "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId) {
                //FR成功
                if (faceFeature != null) {
                    if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，延迟100ms再执行该函数
                    else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.UNKNOWN) {
                        getFeatureDelayedDisposables.add(Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                .subscribe(new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) {
                                        onFaceFeatureInfoGet(faceFeature, requestId);
                                    }
                                }));
                    }
                    //活体检测失败
                    else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.NOT_ALIVE);
                    }
                }
                //FR 失败
                else {
                    requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                }
            }
        };

        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, getPrewView().getWidth(), getPrewView().getHeight(), displayOrientation
                        , cameraId, isMirror, isPosAndNege());

                faceHelper = new FaceHelper.Builder(getApplicationContext())
                        .faceEngine(faceEngine)
                        .frThreadNum(MAX_DETECT_NUM)
                        .previewSize(previewSize)
                        .faceListener(faceListener)
                        .currentTrackId(ConfigUtil.getTrackId(BaseFaceActivity.this.getApplicationContext()))
                        .build();
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (getRectView() != null) {
                    getRectView().clearFaceInfo();
                }
                if (!closeSearchFace) {
                    List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                    if (facePreviewInfoList != null && getRectView() != null && drawHelper != null) {
                        List<DrawInfo> drawInfoList = new ArrayList<>();
                        for (int i = 0; i < facePreviewInfoList.size(); i++) {
                            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
                            drawInfoList.add(new DrawInfo(facePreviewInfoList.get(i).getFaceInfo().getRect(), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, LivenessInfo.UNKNOWN,
                                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
                        }
                        drawHelper.draw(getRectView(), drawInfoList);
                    }
                    clearLeftFace(facePreviewInfoList);

                    if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                        for (int i = 0; i < facePreviewInfoList.size(); i++) {
                            livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
                            /**
                             * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                             * FR回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer)}中回传
                             */
                            if (requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == null
                                    || requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == RequestFeatureStatus.FAILED) {
                                requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                                faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId(), setFaceDectectRotation());
                            }
                        }
                    }
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };
        cameraHelper = new CameraHelper.Builder()
                .metrics(metrics)
                //.rotation(getWindowManager().getDefaultDisplay().getRotation())
                .rotation(setFaceDectectRotation())
                .specificCameraId(cameraID != null ? cameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(getPrewView())
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        try {
            if (cameraHelper != null) {
                cameraHelper.start();
            }
        } catch (Exception e) {
            Toast.makeText(this, "相机打开错误:" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        Set<Integer> keySet = requestFeatureStatusMap.keySet();
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                int trackId = compareResultList.get(i).getTrackId();
                if (!keySet.contains(trackId)) {
                    String path = ImageUtil.getFaceCacheDirPath(trackId + "");
                    if (!TextUtils.isEmpty(path)) {
                        DeleteFileUtil.deleteFile(path);
                    }
                    compareResultList.remove(i);
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            String path = ImageUtil.getRootCachePath();
            if (!TextUtils.isEmpty(path)) {
                DeleteFileUtil.delete(path);
            }
            return;
        }

        for (Integer integer : keySet) {
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == integer) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                String path = ImageUtil.getFaceCacheDirPath(integer.toString());
                if (!TextUtils.isEmpty(path)) {
                    DeleteFileUtil.deleteFile(path);
                }
                requestFeatureStatusMap.remove(integer);
                livenessMap.remove(integer);
            }
        }

    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable
                .create(new ObservableOnSubscribe<CompareResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<CompareResult> emitter) {
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
                        if (compareResult == null) {
                            emitter.onError(null);
                        } else {
                            emitter.onNext(compareResult);
                        }
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, "VISITOR " + requestId);
                            return;
                        }

                        if (compareResult.getSimilar() > setCompareScore()) {
                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.addName(requestId, "VISITOR " + requestId);
                                return;
                            }
                            for (CompareResult compareResult1 : compareResultList) {
                                if (compareResult1.getTrackId() == requestId) {
                                    isAdded = true;
                                    break;
                                }
                            }
                            if (!isAdded) {
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    CompareResult result = compareResultList.get(0);
                                    String path = ImageUtil.getFaceCacheDirPath(result.getTrackId() + "");
                                    if (!TextUtils.isEmpty(path)) {
                                        DeleteFileUtil.deleteFile(path);
                                    }
                                    compareResultList.remove(0);
                                }
                                //添加显示人员时，保存其trackId
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);
                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.addName(requestId, compareResult.getUserName());
                            showCompareResult(compareResult, requestId, true);
                        } else {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, "VISITOR " + requestId);
                            showCompareResult(compareResult, requestId, false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void initEngine() {
        if (faceEngine == null) {
            faceEngine = new FaceEngine();
            afCode = faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                    16, MAX_DETECT_NUM,
                    FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
            VersionInfo versionInfo = new VersionInfo();
            faceEngine.getVersion(versionInfo);
            Log.e(TAG, "initEngine:  init: " + afCode + "  version:" + versionInfo);

            if (afCode != ErrorInfo.MOK) {
                Toast.makeText(this, getString(R.string.init_failed, afCode), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {
        if (faceEngine != null && afCode == ErrorInfo.MOK) {
            afCode = faceEngine.unInit();
            Log.i(TAG, "unInitEngine: " + afCode);
        }
    }

    @Override
    protected void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }
        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized (faceHelper) {
                unInitEngine();
            }
            ConfigUtil.setTrackId(this, faceHelper.getCurrentTrackId());
            faceHelper.release();
        } else {
            unInitEngine();
        }
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.dispose();
            getFeatureDelayedDisposables.clear();
        }
        super.onDestroy();
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                initEngine();
                initCamera();
            } else {
                Toast.makeText(this, "应用授权受限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 子activity布局
     */
    protected abstract int setLayout();

    /**
     * 绘制人脸的控件 FaceRectView
     */
    protected abstract FaceRectView getRectView();

    /**
     * RGB相机预览显示的控件，可为SurfaceView或TextureView
     */
    protected abstract View getPrewView();

    /**
     * 识别阈值 范围[0,1]
     */
    protected abstract double setCompareScore();

    /**
     * 数据初始化
     */
    protected abstract void initView();

    /**
     * 展示识别结果
     */
    protected abstract void showCompareResult(CompareResult result, Integer trackId, Boolean flag);

    /**
     * 确认检测方向 (0,90,180,270)
     */
    protected abstract int setFaceDectectRotation();

    /**
     * 预览人脸框正反朝向
     */
    protected abstract boolean isPosAndNege();

    public void setCloseSearchFace(Boolean closeSearchFace) {
        this.closeSearchFace = closeSearchFace;
    }
}
