package com.dj.hrfacelib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ImageUtil {
    /**
     * Bitmap转化为ARGB数据，再转化为NV21数据
     *
     * @param src    传入的Bitmap，格式为{@link Bitmap.Config#ARGB_8888}
     * @param width  NV21图像的宽度
     * @param height NV21图像的高度
     * @return nv21数据
     */
    public static byte[] bitmapToNv21(Bitmap src, int width, int height) {
        if (src != null && src.getWidth() >= width && src.getHeight() >= height) {
            int[] argb = new int[width * height];
            src.getPixels(argb, 0, width, 0, 0, width, height);
            return argbToNv21(argb, width, height);
        } else {
            return null;
        }
    }

    /**
     * ARGB数据转化为NV21数据
     *
     * @param argb   argb数据
     * @param width  宽度
     * @param height 高度
     * @return nv21数据
     */
    private static byte[] argbToNv21(int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        byte[] nv21 = new byte[width * height * 3 / 2];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0x00FF00) >> 8;
                int B = argb[index] & 0x0000FF;
                int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
                int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
                int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.length - 2) {
                    nv21[uvIndex++] = (byte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                    nv21[uvIndex++] = (byte) (U < 0 ? 0 : (U > 255 ? 255 : U));
                }

                ++index;
            }
        }
        return nv21;
    }

    /**
     * bitmap转化为bgr数据，格式为{@link Bitmap.Config#ARGB_8888}
     *
     * @param image 传入的bitmap
     * @return bgr数据
     */
    public static byte[] bitmapToBgr(Bitmap image) {
        if (image == null) {
            return null;
        }
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);
        byte[] temp = buffer.array();
        byte[] pixels = new byte[(temp.length / 4) * 3];
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[i * 3] = temp[i * 4 + 2];
            pixels[i * 3 + 1] = temp[i * 4 + 1];
            pixels[i * 3 + 2] = temp[i * 4];
        }
        return pixels;
    }

    /**
     * 裁剪bitmap
     *
     * @param bitmap 传入的bitmap
     * @param rect   需要被裁剪的区域
     * @return 被裁剪后的bitmap
     */
    public static Bitmap imageCrop(Bitmap bitmap, Rect rect) {
        if (bitmap == null || rect == null || rect.isEmpty() || bitmap.getWidth() < rect.right || bitmap.getHeight() < rect.bottom) {
            return null;
        }
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), null, false);
    }

    public static Bitmap getBitmapFromUri(Uri uri, Context context) {
        if (uri == null || context == null) {
            return null;
        }
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree) {
        if (b == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotateDegree);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
    }

    /**
     * 确保传给引擎的BGR24数据宽度为4的倍数
     *
     * @param bitmap 传入的bitmap
     * @return 调整后的bitmap
     */
    public static Bitmap alignBitmapForBgr24(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 4) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean needAdjust = false;
        while (width % 4 != 0) {
            width--;
            needAdjust = true;
        }

        if (needAdjust) {
            bitmap = imageCrop(bitmap, new Rect(0, 0, width, height));
        }
        return bitmap;
    }

    /**
     * 确保传给引擎的NV21数据宽度为4的倍数，高为2的倍数
     *
     * @param bitmap 传入的bitmap
     * @return 调整后的bitmap
     */
    public static Bitmap alignBitmapForNv21(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 4 || bitmap.getHeight() < 2) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean needAdjust = false;
        while (width % 4 != 0) {
            width--;
            needAdjust = true;
        }
        if (height % 2 != 0) {
            height--;
            needAdjust = true;
        }

        if (needAdjust) {
            bitmap = imageCrop(bitmap, new Rect(0, 0, width, height));
        }
        return bitmap;
    }

    /**
     * nv21格式转bitmap
     *
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static Bitmap nv21ToBitmap(byte[] data, int width, int height) {
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, outputStream);
        byte[] jpegData = outputStream.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
    }

    /**
     * /** 根据人脸框的位置从图片中抠出对应的人脸<br>
     * crop the face from image according to the face rectangle
     *
     * @param sourceBitmap 需要处理的原始图片<br>
     *                     the source image object
     * @param orginRect    想要抠出来的人脸框位置<br>
     *                     the face rectangle to crop
     * @return 抠出来的人脸图<br>
     * the cropped face image object
     */
    public static Bitmap getCropBitmap(Bitmap sourceBitmap, Rect orginRect) {
        if (sourceBitmap == null || sourceBitmap.isRecycled()) {
            return null;
        }
        Rect rect = getScaleRect(orginRect, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        return Bitmap.createBitmap(sourceBitmap, rect.left, rect.top, rect.width(), rect.height());
    }

    /**
     * 计算抠脸所需的矩形框，根据人脸框往外分别扩展宽度和高度的15%，可以根据需要调整，如果左边框或者上边框重新计算抠图框之后位置小于0，
     * 那么左边框和上边框位置就设为0， 如果右边框和下边框重新计算抠图框之后位置大于原始图片的宽高，那么左边框和下边框位置就设为图片的宽度和高度<br>
     * calculate the rectangle required to crop face, expand 15% of the width
     * and height of face rectangle respectively, you can adjust as you need, if
     * the left or the top of rectangle after calculate is less than 0, then set
     * to 0, if the right or bottom of the rectangle after calculate is large
     * than the with and height of source image, then set the right to width,
     * set the bottom to height
     *
     * @param rect 人脸矩形框<br>
     *             the face rectangle
     * @param maxW 抠脸所需矩形框限定的最大宽度<br>
     *             the max width of the rectangle for calculating
     * @param maxH 抠脸所需矩形框限定的最大高度<br>
     *             the max height of the rectangle for calculating
     * @return 抠脸所需的新矩形框<br>
     * the new rectangle
     */
    public static Rect getScaleRect(Rect rect, int maxW, int maxH) {
        Rect resultRect = new Rect();
        int left = (int) (rect.left - ((rect.width() * 20) / 100));
        int right = (int) (rect.right + ((rect.width() * 20) / 100));
        int bottom = (int) (rect.bottom + ((rect.height() * 20) / 100));
        int top = (int) (rect.top - ((rect.height() * 20) / 100));
        resultRect.left = left > 0 ? left : 0;
        resultRect.right = right > maxW ? maxW : right;
        resultRect.bottom = bottom > maxH ? maxH : bottom;
        resultRect.top = top > 0 ? top : 0;
        return resultRect;
    }

    /**
     * 根据指定的角度旋转图片<br>
     * rotate the image according to the specified angle
     *
     * @param srcbitmap   原始图片对象<br>
     *                    the source image object
     * @param orientation 旋转角度<br>
     *                    the rotated angle
     * @return 旋转之后的图片对象<br>
     * the image object after rotated
     */
    public static Bitmap adjustPhotoRotation(Bitmap srcbitmap, int orientation) {
        Bitmap rotatebitmap = null;
        Matrix m = new Matrix();
        int width = srcbitmap.getWidth();
        int height = srcbitmap.getHeight();
        // 设置旋转度
        // set the rotated angle
        m.setRotate(orientation);
        try {
            // 新生成图片
            // create a new bitmap
            rotatebitmap = Bitmap.createBitmap(srcbitmap, 0, 0, width, height, m, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (rotatebitmap == null) {
            rotatebitmap = srcbitmap;
        }

        if (srcbitmap != rotatebitmap) {
            srcbitmap.recycle();
        }
        return rotatebitmap;
    }

    /**
     * 根据图片路径获取图片对象<br>
     * get the image object according the path
     *
     * @param absolutePath 图片绝对路径<br>
     *                     the absolute path of image
     * @return 图片对象<br>
     * the image object
     */
    public static Bitmap getRotatedBitmap(String absolutePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(absolutePath);
        if (bitmap == null) {
            return null;
        }
        bitmap = compressImage(bitmap);
        // 获取图片方向
        // get the orientation of image object
        int orientation = getBitmapDegree(absolutePath);
        // 如果图片方向不等于0，那么对图片做旋转操作
        // rotate the image if orientation is not 0
        if (orientation != 0) {
            bitmap = adjustPhotoRotation(bitmap, orientation);
        }
        return bitmap;
    }

    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 80;
        while (baos.toByteArray().length / 1024 > 100) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
            if (options <= 0) {
                break;
            }
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /**
     * 根据图片路径获取图片的方向信息<br>
     * get the image orientation according the image path
     *
     * @param path 图片绝对路径<br>
     *             the absolute path of image
     * @return 方向信息<br>
     * the orientation of the image
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            // get the exif according to the image path
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的方向信息
            //get the orientation according orientation tag
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 保存图片对象到指定路径<br>
     * save the image object to the specific path
     *
     * @param savePath 保存路径<br>
     *                 the path for saving
     * @param bitmap   待保存的图片对象<br>
     *                 the image to save
     * @return 保存结果<br>
     * save result
     */
    public static boolean saveBitmap(String savePath, Bitmap bitmap) {
        if (null == bitmap)
            return false;
        File f = new File(savePath);
        File parentFile = f.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileOutputStream out = null;
        try {
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static Boolean saveFaceCacheDirPathState(String id, Bitmap bt) {
        File sdcardDir = Environment.getExternalStorageDirectory();
        String pathname = sdcardDir + "/DJ_FACE/recordCache";
        File file = new File(pathname);
        if (!file.exists()) {
            file.mkdirs();
        }
        String facePath = pathname + "/" + id + ".jpg";
        return saveBitmap(facePath, bt);
    }

    public static String getFaceCacheDirPath(String id) {
        File sdcardDir = Environment.getExternalStorageDirectory();
        String pathname = sdcardDir + "/DJ_FACE/recordCache";
        File file = new File(pathname);
        if (!file.exists()) {
            return null;
        }
        String facePath = pathname + "/" + id + ".jpg";
        File face = new File(facePath);
        if (!face.exists()) {
            return null;
        }
        return facePath;
    }

    public static String getRootCachePath(){
        File sdcardDir = Environment.getExternalStorageDirectory();
        String pathname = sdcardDir + "/DJ_FACE/recordCache";
        File file = new File(pathname);
        if (!file.exists()) {
            return null;
        }
        return pathname;
    }

    public static boolean deleteAllFacePicture(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteAllFacePicture(f.getAbsolutePath());
            }
        }
        return file.delete();
    }

    public static String getBase64FromImgFile(String imgPath) {
        try {
            InputStream inputStream = new FileInputStream(imgPath);
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
