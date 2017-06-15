package sg.edu.nus.mycamera;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import static android.os.Environment.getExternalStorageDirectory;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback{

    CameraManager mCmanager;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private  MediaRecorder recorder;
    private  SurfaceHolder holder;
    private boolean recording = false;
    private File mOutputFile;
    private String mFileName;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private boolean permissionTORecordVideo = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE","android.permission.CAMERA"};
    private String LOG_TAG = MainActivity.class.getName();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       // mFileName  =  getExternalStorageDirectory().getAbsolutePath();
        //mFileName += "/test.mp4";

        recorder = new MediaRecorder();
        int requestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }

        setContentView(R.layout.activity_main);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surface_view);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraView.setClickable(true);
        cameraView.setOnClickListener(this);

    }

    private void initRecorder() {

        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_480P);
        recorder.setProfile(cpHigh);
        Calendar calendar=Calendar.getInstance();
        String timeInMillis = String.valueOf(calendar.getTimeInMillis());
        mFileName  =  getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/"+timeInMillis+".mp4";
        recorder.setOutputFile(mFileName);
        recorder.setMaxDuration(15000); // 15 seconds
        //recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
        recorder.setOrientationHint(180);
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.i("VIDEOCAPTURE","Maximum Duration Reached");
                    recorder.stop();
                    recorder.reset();
                    UploadCloud();
                    recording = false;
                    initRecorder();
                    prepareRecorder();
                    recorder.start();
                }
            }
        });
    }

    private void prepareRecorder() {
        recorder.setPreviewDisplay(holder.getSurface());

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    public void onClick(View v) {
        if (recording) {
            recorder.stop();
            recording = false;

            // Let's initRecorder so we can record again
            recorder.reset();
            initRecorder();
            prepareRecorder();
        } else {
            recording = true;
            recorder.start();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        initRecorder();
        prepareRecorder();
        recording = true;
        recorder.start();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        if (recorder != null) {
            //recorder.release();
            //recorder = null;
        }
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first


        // Release the Camera because we don't need it when paused
        // and other activities might need to use it.
        if (recorder != null) {
            //recorder.release();
            //recorder = null;
        }
    }

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();

        if (recorder != null) {
            //recorder.release();
            //recorder = null;
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                permissionTORecordVideo = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) MainActivity.super.finish();
        if (!permissionToWriteAccepted ) MainActivity.super.finish();

        if (!permissionTORecordVideo ){
            Log.e("Recorder ", "PermissionNotPresent");
            MainActivity.super.finish();}
    }

/*    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.v("VIDEOCAPTURE","Maximum Duration Reached");
            recorder.stop();
            recorder.reset();
            recording = false;
            initRecorder();
            prepareRecorder();
            recorder.start();
            UploadCloud();
        }
        //Toast.makeText(MainActivity.this, "Again",Toast.LENGTH_LONG).show();
    }*/

    public void UploadCloud(){
        String path0 = "video-api";
        AsyncTaskRunner asyncTaskRunner=new AsyncTaskRunner();
        Log.i("file",mFileName);
        asyncTaskRunner.execute(path0,mFileName);
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            if (params.length < 1 || params[0] == null) {
                return null;
            }
            String response = null;
            try {
                InputStream inputStream = getResources().openRawResource(getResources().getIdentifier("poc_video1","raw",getPackageName()));
                response = UploadFile.uploadFile(params[0],params[1],inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                String fileName = response.split("o/")[1];
                Log.i(LOG_TAG,response+", fileName:"+fileName);
                AsyncTaskRunnerForAPI asyncTaskRunnerForAPI = new AsyncTaskRunnerForAPI();
                asyncTaskRunnerForAPI.execute(Constant.postUrl+Constant.apiKey,fileName);
                //Toast.makeText(MainActivity.this, "Choose Countries :", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "No Record Found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class AsyncTaskRunnerForAPI extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            if (params.length < 1 || params[0] == null) {
                return null;
            }
            String response = ApiCall.callHttpConnection(params[0],params[1]);
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                Toast.makeText(MainActivity.this, "Choose Countries :", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "No Record Found", Toast.LENGTH_SHORT).show();
            }
        }
    }

/*
    public boolean prepareVideoRecorder(){
         mCmanager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // Use the same size for recording profile.

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mOutputFile=CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        mMediaRecorder.setOutputFile(mOutputFile.getPath());
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    *//**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     *//*
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                MainActivity.this.finish();
            }
            // inform the user that recording has started


        }
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.

        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mPreview.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(mOutputFile);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = mPreview.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                   // updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            Log.i(TAG, "Camera present in devices " + manager.getCameraIdList());
            cameraId = manager.getCameraIdList()[0];
            Log.e(TAG, "Camera Id is " + cameraId);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    *//*protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*//*
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }*/
}
