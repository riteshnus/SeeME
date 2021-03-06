package sg.edu.nus.mycamera;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

import sg.edu.nus.mycamera.dto.SeeMeObjects;

import static android.os.Environment.getExternalStorageDirectory;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    CameraManager mCmanager;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private  MediaRecorder recorder;
    private  SurfaceHolder holder;
    private boolean recording = false;
    private File mOutputFile;
    private String mFileName;
    private int noOfFilesUpload = 0;
    private int noOfResponseAfterApiCall = 0;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private boolean permissionTORecordVideo = false;
    private boolean permissionTOAccessFineLoacation = false;
    private boolean permissionToAccessCoarseLocation=false;
    private String LOG_TAG = MainActivity.class.getName();
    private String textToRead;
    private MediaPlayer mediaPlayer;
    private URL presignedSynthesizeSpeechUrl ;
    private static final Regions MY_REGION = Regions.US_EAST_1;
    private AmazonPollyPresigningClient client;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private String android_id;
    AsyncTaskRunner asyncTaskRunner;
    AsyncTaskRunnerForAPI asyncTaskRunnerForAPI;
    PlaySpeech playSpeech;
    static File saveDir = null;
    private static final String COGNITO_POOL_ID = "us-east-1:7d6bf1bc-6f56-4ec5-8a36-7aaa48fec991";
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;

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
            requestPermissions(Constant.permissions, requestCode);
        }

        setContentView(R.layout.activity_main);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surface_view);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        initAmazonServices();
        setupNewMediaPlayer();
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        //cameraView.setClickable(true);
        //cameraView.setOnClickListener(this);

    }

    private void initRecorder() {

        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_480P);
        recorder.setProfile(cpHigh);
        Calendar calendar=Calendar.getInstance();
        String timeInMillis = String.valueOf(calendar.getTimeInMillis());
        // creating dir for temp storage of videos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            saveDir = new File(Environment.getExternalStorageDirectory(), "seeMe");
            saveDir.mkdirs();
        }
        mFileName  =  getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/seeMe/"+timeInMillis+".mp4";
        recorder.setOutputFile(mFileName);
        recorder.setMaxDuration(10000); // 15 seconds
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
            if (recorder != null) recorder.stop();
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

    // code to delete any file / directory
    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (recorder != null || mediaPlayer !=null) {
            recorder.release();
            recorder = null;
            //mediaPlayer.release();
            if (asyncTaskRunner != null) asyncTaskRunner.cancel(true);
            if (asyncTaskRunnerForAPI != null) asyncTaskRunnerForAPI.cancel(true);
            if (playSpeech != null) playSpeech.cancel(true);
            mediaPlayer.reset();
            mediaPlayer = null;
            if(saveDir != null) deleteRecursive(saveDir);
        }
    }

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                permissionTORecordVideo = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                permissionTOAccessFineLoacation = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                permissionToAccessCoarseLocation = grantResults[4] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) MainActivity.super.finish();
        if (!permissionToWriteAccepted ) MainActivity.super.finish();

        if (!permissionTORecordVideo ){
            Log.e("Recorder ", "PermissionNotPresent");
            MainActivity.super.finish();}
        if (!permissionTOAccessFineLoacation ) MainActivity.super.finish();
        if (!permissionTOAccessFineLoacation ) MainActivity.super.finish();
    }

    public void UploadCloud(){
        String path0 = "video-api";
        asyncTaskRunner=new AsyncTaskRunner();
        asyncTaskRunner.execute(path0,mFileName);
    }

    void initAmazonServices() {
        // Initialize the Amazon Cognito credentials provider.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(credentialsProvider);
        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);
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
                Log.i(LOG_TAG,response+", fileName:"+fileName+", files uploaded-"+ (noOfFilesUpload+1));
                asyncTaskRunnerForAPI = new AsyncTaskRunnerForAPI();
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
            /*if (params.length < 1 || params[0] == null) {
                return null;
            }*/
            String response = ApiCall.callHttpConnection(params[0],params[1]);
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            Log.i(LOG_TAG,response);
            SeeMeObjects seeMeeObject = Utils.createSeeMeObject(MainActivity.this,response);
            seeMeeObject.setId(android_id);
            List<String> textsToRead = seeMeeObject.getObjectDescription();
            if(!textsToRead.isEmpty()) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("There is");
                for (String text : textsToRead) {
                    buffer.append(text + ". ");
                }
                textToRead = buffer.toString();
            }else {
                textToRead = "Video is not clear. Please take clear video";
            }

            Toast.makeText(MainActivity.this,textToRead,Toast.LENGTH_SHORT).show();
            playSpeech = new PlaySpeech();
            playSpeech.execute(seeMeeObject);
        }
    }

    private class PlaySpeech extends AsyncTask<SeeMeObjects,Void,Void> {

        @Override
        protected Void doInBackground(SeeMeObjects... params) {
            // Create speech synthesis request.
            SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                    new SynthesizeSpeechPresignRequest()
                            // Set text to synthesize.
                            .withText(textToRead)
                            // Set voice selected by the user.
                            .withVoiceId("Joanna")
                            // Set format to MP3.
                            .withOutputFormat(OutputFormat.Mp3);

            // Get the presigned URL for synthesized speech audio stream.
            presignedSynthesizeSpeechUrl =
                    client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);
            if(!params[0].getObjectDescription().isEmpty()) {
                mapper.save(params[0]);
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i(LOG_TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

            // Create a media player to play the synthesized audio stream.
            /*if (!mediaPlayer.isPlaying()) {
                setupNewMediaPlayer();
            }*/
            //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                // Set media player's data source to previously obtained URL.
                mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to set data source for the media player! " + e.getMessage());
            }

            // Start the playback asynchronously (since the data source is a network stream).
            mediaPlayer.prepareAsync();
            // Set the callback to start the MediaPlayer when it's prepared.
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            // Set the callback to release the MediaPlayer after playback is completed.
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                }
            });
        }
    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();

            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }
}
