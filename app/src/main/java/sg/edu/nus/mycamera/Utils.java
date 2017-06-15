package sg.edu.nus.mycamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import sg.edu.nus.mycamera.dto.SeeMeObjects;

/**
 * Created by siddharth on 6/15/2017.
 */

public class Utils {
    private static String LOG_TAG = Utils.class.getName();

    public static SeeMeObjects createSeeMeObject(Activity activity,String objectJSON) {

        SeeMeObjects seeMeObject = new SeeMeObjects();
        Location location = getLocation(activity);
        String latitude = String.valueOf(location.getLatitude());
       String  longitude = String.valueOf(location.getLongitude());
        seeMeObject.setLatitude(latitude);
        seeMeObject.setLongitude(longitude);
        List<String> descriptionList = new ArrayList<>();
        try {
            //Thread.sleep(2000);
            JSONObject baseJsonResponse = new JSONObject(objectJSON);
            JSONObject responseObject = baseJsonResponse.getJSONObject("response");
            if (responseObject.has("annotationResults")) {
                JSONArray annotationArray = responseObject.getJSONArray("annotationResults");

                // If there are results in the features array

                if (annotationArray.length() > 0) {
                    for (int i = 0; i < annotationArray.length(); i++) {
                        JSONObject annotationObject = annotationArray.getJSONObject(i);
                        String videoUri = annotationObject.getString("inputUri");
                        seeMeObject.setUrl(videoUri);
                        JSONArray labelAnnotationArray = annotationObject.getJSONArray("labelAnnotations");
                        if (labelAnnotationArray.length() > 0) {
                            for (int j = 0; j < labelAnnotationArray.length(); j++) {
                                JSONObject labelObject = labelAnnotationArray.getJSONObject(j);
                                String description = labelObject.getString("description");
                                descriptionList.add(description);
                            }
                        }

                    }
                }
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the Object JSON results", e);
        } /*catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        seeMeObject.setObjectDescription(descriptionList);
        return seeMeObject;
    }

    public static Location getLocation(Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return null;
                }
            }
            Location l = locationManager.getLastKnownLocation(provider);
           /* Log.d("last known location, provider", provider,
                    l);*/

            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                Log.d("found best last known ", l.toString());
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }
}
