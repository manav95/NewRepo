package mhacks.autismassist;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.firebase.client.Firebase;
import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.Frame.ROTATE;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;


import java.util.List;

    public class MainActivity extends Activity implements CameraDetector.CameraEventListener, Detector.FaceListener,Detector.ImageListener {
        private SurfaceView cameraView; //SurfaceView used to display camera images
        CameraDetector detector;
        private RelativeLayout mainLayout; //layout, to be resized, containing all UI elements
        int cameraPreviewWidth = 0;
        int cameraPreviewHeight = 0;
        private Frame mostRecentFrame;
        private ImageView view;
        private ImageView secView;
        private FirebaseHelper helper;
        private TextView firstTextView;
        private TextView secondTextView;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        String url = "https://autismassist.firebaseIO.com";

        helper = new FirebaseHelper(url);
        this.setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
//        ActionBar actionBar = getActionBar();
//        actionBar.hide();
        view = (ImageView) findViewById(R.id.imageView);
        secView = (ImageView) findViewById(R.id.imageView2);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        cameraView = (SurfaceView) findViewById(R.id.camera_preview);
        firstTextView = (TextView) findViewById(R.id.textView);
        secondTextView = (TextView) findViewById(R.id.textView2);
        PackageManager manager = getPackageManager();
        Log.d("Tag", Boolean.toString(manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) + "front");
        Log.d("Tag", Boolean.toString(manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) + "back");
        initializeCameraDetector();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        detector.stop();
    }

    void initializeCameraDetector() {
        /* Put the SDK in camera mode by using this constructor. The SDK will be in control of
         * the camera. If a SurfaceView is passed in as the last argument to the constructor,
         * that view will be painted with what the camera sees.
         */
        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_BACK, cameraView, 1, Detector.FaceDetectorMode.SMALL_FACES);

        // update the license path here if you name your file something else
        detector.setLicensePath("license.txt");
        detector.setImageListener(this);
        detector.setFaceListener(this);
        detector.setOnCameraEventListener(this);
        detector.setDetectAttention(true);
        detector.setDetectEngagement(true);
        detector.start();
        Log.d("TAG", Boolean.toString(detector.isRunning()));
    }

    @Override
    public void onCameraSizeSelected(int cameraWidth, int cameraHeight, ROTATE rotation) {
        if (rotation == ROTATE.BY_90_CCW || rotation == ROTATE.BY_90_CW) {
            cameraPreviewWidth = cameraHeight;
            cameraPreviewHeight = cameraWidth;
        } else {
            cameraPreviewWidth = cameraWidth;
            cameraPreviewHeight = cameraHeight;
        }
        //drawingView.setThickness((int) (cameraPreviewWidth / 100f));

        mainLayout.post(new Runnable() {
            @Override
            public void run() {
                //Get the screen width and height, and calculate the new app width/height based on the surfaceview aspect ratio.
                DisplayMetrics displaymetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int layoutWidth = displaymetrics.widthPixels;
                int layoutHeight = displaymetrics.heightPixels;

                if (cameraPreviewWidth == 0 || cameraPreviewHeight == 0 || layoutWidth == 0 || layoutHeight == 0)
                    return;

                float layoutAspectRatio = (float) layoutWidth / layoutHeight;
                float cameraPreviewAspectRatio = (float) cameraPreviewWidth / cameraPreviewHeight;

                int newWidth;
                int newHeight;

                if (cameraPreviewAspectRatio > layoutAspectRatio) {
                    newWidth = layoutWidth;
                    newHeight = (int) (layoutWidth / cameraPreviewAspectRatio);
                } else {
                    newWidth = (int) (layoutHeight * cameraPreviewAspectRatio);
                    newHeight = layoutHeight;
                }

                //drawingView.updateViewDimensions(newWidth, newHeight, cameraPreviewWidth, cameraPreviewHeight);

                ViewGroup.LayoutParams params = mainLayout.getLayoutParams();
                params.height = newHeight;
                params.width = newWidth;
                mainLayout.setLayoutParams(params);

                //Now that our main layout has been resized, we can remove the progress bar that was obscuring the screen (its purpose was to obscure the resizing of the SurfaceView)
                //progressBarLayout.setVisibility(View.GONE);
            }
        });

    }


    /**
     * Process picture - from example GDK
     *
     * @param picturePath
     */


    /**
     * Added but irrelevant
     */
    /*
     * (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        this.finishAffinity();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onFaceDetectionStarted() {
        //
    }
    @Override
    public void onFaceDetectionStopped() {
    }
    @Override
    public void onImageResults(List<Face> faces, Frame image,float timestamp) {
        int[] measurements = new int[2];
        int e;
        int a;

        if (faces == null) {
            //Log.d("TAG", "frame not processed");
            return; //frame was not processed
        }

        if (faces.size() == 0) {
            //Log.d("TAG", "no face found");
            firstTextView.setText("0");
            secondTextView.setText("0");
            measurements[0]=0;
            measurements[1]=0;
            helper.saveArray(measurements);
            return; //no face found
        }

        //For each face found

        for (int i = 0 ; i < faces.size() ; i++) {
            Face face = faces.get(i);
            //int faceId = face.getId();

            //Appearance
            //Face.GENDER genderValue = face.appearance.getGender();
            //Face.GLASSES glassesValue = face.appearance.getGlasses();


            //Some Expressions
            float engagement = face.emotions.getEngagement();
            float attention = face.expressions.getAttention();

            e=Math.round(engagement);
            a=Math.round(attention);
            //Log.d("TAG",Integer.toString(s));
            //Measurements

            firstTextView.setText(Integer.toString(e));
            secondTextView.setText(Integer.toString(a));
            measurements[0]=e;
            measurements[1]=a;

            helper.saveArray(measurements);

            //Face feature points coordinates
            //PointF[] points = face.getFacePoints();

        }
    }
}
