package sg.edu.nus.mycamera;

import android.provider.Settings;

/**
 * Created by Ritesh on 6/11/2017.
 */

public class Constant {
        public static final String apiKey = "AIzaSyBTjFmHan4EGktatA8E8718xMinYKNg18M";
        public static final String postUrl = "https://videointelligence.googleapis.com/v1beta1/videos:annotate?key=";
        public static final String getUrl = "https://videointelligence.googleapis.com/v1/operations/";
        public static final String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE","android.permission.CAMERA", android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION};


}
