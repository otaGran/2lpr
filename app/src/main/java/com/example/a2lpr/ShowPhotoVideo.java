package com.example.a2lpr;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.CvType.CV_8U;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.Sobel;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.blur;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;


public class ShowPhotoVideo extends Activity {
    private static final String TAG = "crop";
    static {
        System.loadLibrary("PalateLocate");
    }

    public void showMyToast(final Toast toast, final int cnt) {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                toast.show();
            }
        }, 0, 3000);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                toast.cancel();
                timer.cancel();
            }
        }, cnt );
    }



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        Uri uri = getIntent().getData();
        if (getIntent().getType().equals("image/*")) {

            //Mat input = Imgcodecs.imread(uri.getPath(),Imgcodecs.IMREAD_COLOR);

            Bitmap src = BitmapFactory.decodeFile(uri.getPath());
            Bitmap crop_img = crop(src);
            Mat input = new Mat();
            Utils.bitmapToMat(crop_img,input);
            Mat ori = input.clone();
            cvtColor(input,input,COLOR_BGR2GRAY);

            blur(input,input,new Size(5,5));
            Sobel(input,input,0,1,0,3,1,0,BORDER_DEFAULT);
            threshold(input,input,0,255,THRESH_OTSU+THRESH_BINARY);
            Mat element = Imgproc.getStructuringElement(MORPH_RECT, new Size(25,5));
            Imgproc.morphologyEx(input, input, Imgproc.MORPH_CLOSE, element);
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat mHierarchy = new Mat(0, 0, CvType.CV_8U);
            Imgproc.findContours(input, contours, mHierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            if (contours.size() > 0) {


                MatOfPoint2f allPoints = new MatOfPoint2f();
                // allPoints.push_back(new
                // MatOfPoint2f(contours.get(0).toArray()));
                // Bumble through all the contours
                for (MatOfPoint matOfPoint : contours) {
                    Scalar color = new Scalar(0, 255, 0, 128);


                    MatOfPoint2f points = new MatOfPoint2f(matOfPoint.toArray());

                    allPoints.push_back(points);
                    // Draw a box around this specific area
                    RotatedRect box = Imgproc.minAreaRect(points);
                    if(verifySizes(box)) {
                        Point[] rect_points = new Point[4];
                        box.points(rect_points);
                        for (int j = 0; j < 4; j++) {
                            line(ori, rect_points[j], rect_points[(j + 1) % 4], color,5);
                        }
                        //Imgproc.resize(ori,ori,new Size(232,540));

                    }
                    //Imgproc.rectangle(ori, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(180, 0, 0));
                }
                // Draw a box around the whole lot
                //RotatedRect box = Imgproc.minAreaRect(allPoints);
                //Imgproc.rectangle(ori, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 0, 250));
            }

            //input = PlateLocate(input);
            //Imgproc.cvtColor(input,input,Imgproc.COLOR_RGB2GRAY);
           // Imgproc.threshold(input,input,0,255,Imgproc.THRESH_OTSU);
            Bitmap imshow = Bitmap.createBitmap(input.width(),input.height(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(ori, imshow);

            //Bitmap imcrop = crop(imshow);
            ImageView view = new ImageView(this);

            //view.setImageURI(uri);
            view.setImageBitmap(imshow);
            view.setLayoutParams(layoutParams);
            relativeLayout.addView(view);


        } else {
            MediaController mc = new MediaController(this);
            VideoView view = new VideoView(this);
            mc.setAnchorView(view);
            mc.setMediaPlayer(view);
            view.setMediaController(mc);
            view.setVideoURI(uri);
            view.start();
            view.setLayoutParams(layoutParams);
            relativeLayout.addView(view);
        }
        setContentView(relativeLayout, layoutParams);
    }
    public  Bitmap crop(Bitmap originalBitmap) {

        Log.w(TAG, "crop: ori_width = "+originalBitmap.getWidth()+"ori_height = "+originalBitmap.getHeight() );
        double originalWidth = originalBitmap.getWidth();
        double originalHeight = originalBitmap.getHeight();
        //图片真正的宽度除以设计图的宽(1280)，得到相对于设计图的缩放比，用于后续裁剪起点坐标和裁剪区域大小的计算
        double scaleX = originalWidth / 720;
        //将虚拟导航栏的高度换算为1280x720设计图下所占的高度(px)，若设备没有虚拟导航栏，返回0.
        //int navBarHeightPxIn1280x720Ui = px2dp(getNavigationBarHeightInPx()) * 2 /*1280x720的设计图下，1dp = 2px*/;
        //考虑到虚拟导航栏所占的高度，需要对scaleX进行修正，下面计算修正的倍乘系数
        //double scaleXMultiplier = ((double) 1280) / ((double) (1280 - navBarHeightPxIn1280x720Ui));
        //修正scaleX，以保证在有无虚拟导航栏的情况下，裁剪区域均正确
        //scaleX = scaleX * scaleXMultiplier;
        double scaleY = originalHeight / 180;
        //在1280x720的设计图上，裁剪起点坐标(52, 80)
        int x = (int) (80 * scaleX + 0.5);
        int y = (int) (52 * scaleY + 0.5);
        //在1280x720的设计图上，裁剪区域大小为896x588
        int width = (int) (588 * scaleX + 0.5);
        int height = (int) (896 * scaleY + 0.5);
        return Bitmap.createBitmap(originalBitmap,80, 255, 560, 357);
    }


    /**
     * dp转px
     *
     * @param dpValue dp
     * @return px
     */
    public int dp2px(float dpValue) {

        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int px2dp(float pxValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 判断设备是否具有虚拟导航栏
     * @return 设备是否具有虚拟导航栏
     */
    public  boolean hasNavigationBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return true;
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);
        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);
        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;
        return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
    }

    /**
     * 得到以px为单位的虚拟导航栏高度，若设备没有虚拟导航栏，返回0.
     * @return 虚拟导航栏高度(px)，若设备没有虚拟导航栏，返回0.
     */
    public  int getNavigationBarHeightInPx() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return dp2px(48);
        int navBarHeightInPx = 0;
        Resources rs = getResources();
        int id = rs.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0 && hasNavigationBar()) navBarHeightInPx = rs.getDimensionPixelSize(id);
        return navBarHeightInPx;
    }

    int m_angle = 20;
    boolean verifySizes(RotatedRect mr) {
        double error=0.4;
        //Spain car plate size: 52x11 aspect 4,7272
        //Taiwan car plate size: 38x16 aspect 2.3750
        double aspect=2.254;
        //Set a min and max area. All other patchs are discarded
        int min=(int)(15*aspect*15); // minimum area
        int max= (int)(125*aspect*125); // maximum area
        //Get only patchs that match to a respect ratio.
        double rmin= aspect-aspect*error;
        double rmax= aspect+aspect*error;

        int area= (int)(mr.size.height * mr.size.width);
        double r= (double)mr.size.width / (double)mr.size.height;
        if(r<1)
            r= (double)mr.size.height / (double)mr.size.width;

        double dr = (double) mr.size.width / (double) mr.size.height;
        double roi_angle = mr.angle;
        if (dr < 1) {
            mr.angle = 90 + roi_angle;
            //swap(mr.size.width, mr.size.height);
            double tmp = mr.size.width;
            mr.size.width = mr.size.height;
            mr.size.height = tmp;
        }


        if(( area < min || area > max ) || ( r < rmin || r > rmax )){
            return false;
        }else{
            if(mr.angle - m_angle <0 && mr.angle + m_angle > 0)
                return true;
            else
                return false;
        }

    }







}