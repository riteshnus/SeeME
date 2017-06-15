package sg.edu.nus.mycamera;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by siddharth on 6/15/2017.
 */

public class Utils {
    private static String LOG_TAG = Utils.class.getName();
    public static List<String> extractDataFromJSON(String objectJSON) {

        List<String> descriptionList = new ArrayList<>();
        try {
            //Thread.sleep(2000);
            JSONObject baseJsonResponse = new JSONObject(objectJSON);
            JSONObject responseObject = baseJsonResponse.getJSONObject("response");
            if(responseObject.has("annotationResults")) {
                JSONArray annotationArray = responseObject.getJSONArray("annotationResults");

                // If there are results in the features array

                if (annotationArray.length() > 0) {
                    for (int i = 0; i < annotationArray.length(); i++) {
                        JSONObject annotationObject = annotationArray.getJSONObject(i);
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
            Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
        } /*catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        return descriptionList;
    }
}
