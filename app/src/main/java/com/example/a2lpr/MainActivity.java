package com.example.a2lpr;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "loc";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView mediaPreview = (ImageView) findViewById(R.id.media_preview);
        initCamera();

        Button buttonSettings = (Button) findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.camera_preview,
                        new SettingsFragment()).addToBackStack(null).commit();
            }
        });

        final Button buttonCapturePhoto = (Button) findViewById(R.id.button_capture_photo);
        buttonCapturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.takePicture(mediaPreview);
            }
        });
        final Button buttonCaptureVideo = (Button) findViewById(R.id.button_capture_video);
        buttonCaptureVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreview.isRecording()) {
                    mPreview.stopRecording(mediaPreview);
                    buttonCaptureVideo.setText("录像");
                } else {
                    if (mPreview.startRecording()) {
                        buttonCaptureVideo.setText("停止");
                    }
                }
                final FrameLayout photo_frame =  (FrameLayout)findViewById(R.id.photo_frame);
                int[] loc = new int[2];
                photo_frame.getLocationOnScreen(loc);
                Log.w(TAG, "loc: x: "+loc[0]+" y: "+loc[1] );
            }
        });
        mediaPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShowPhotoVideo.class);
                intent.setDataAndType(mPreview.getOutputMediaFileUri(), mPreview.getOutputMediaFileType());
                startActivityForResult(intent, 0);
            }
        });
    }
    private CameraPreview mPreview;

    private void initCamera() {
        mPreview = new CameraPreview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        SettingsFragment.passCamera(mPreview.getCameraInstance());
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SettingsFragment.setDefault(PreferenceManager.getDefaultSharedPreferences(this));
        SettingsFragment.init(PreferenceManager.getDefaultSharedPreferences(this));
    }

    public void onPause() {
        super.onPause();
        mPreview = null;
    }

    public void onResume() {
        super.onResume();
        if (mPreview == null) {
            initCamera();
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
