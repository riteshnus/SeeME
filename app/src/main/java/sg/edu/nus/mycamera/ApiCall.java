package sg.edu.nus.mycamera;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by Ritesh on 6/11/2017.
 */

public class ApiCall {
    public static String callHttpConnection(String url,String FileName){
        String jsonResponse = "";
        String nameOfRegion = "";
        URL newUrl = null;
        String fileName = "gs://video-api/"+FileName;
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        DataOutputStream outputStream = null;
        try {
            newUrl = new URL(url);
            if (newUrl == null)
                return jsonResponse;
            urlConnection = (HttpURLConnection) newUrl.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type","application/json");
            urlConnection.connect();
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("inputUri",fileName);
            jsonParam.put("features","LABEL_DETECTION");
            outputStream = new DataOutputStream(urlConnection.getOutputStream());
            outputStream.writeBytes(jsonParam.toString());
            outputStream.flush();
            outputStream.close();
            if(urlConnection.getResponseCode()==200){
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
                Log.v("post response",jsonResponse);
                JSONObject resJson = new JSONObject(jsonResponse);
                nameOfRegion = resJson.getString("name");
                callHttpGetRequest(nameOfRegion);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    public static String callHttpGetRequest(String region){
        Log.v("region",region);
        String jsonResponse = "";
        URL newUrl = null;
        String url1 = "https://videointelligence.googleapis.com/v1/operations/asia-east1.3736972764404527650?key=AIzaSyBTjFmHan4EGktatA8E8718xMinYKNg18M"; //Constant.getUrl.trim()+region.trim()+"?key="+Constant.apiKey.trim();
        Log.v("url",url1);
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            newUrl = new URL(url1.toString().trim());
            Log.v("get url",newUrl.toString());
            if (newUrl == null)
                return jsonResponse;
            urlConnection = (HttpURLConnection) newUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type","application/json");
            urlConnection.connect();
            if(urlConnection.getResponseCode()==200){
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
                Log.v("get response",jsonResponse);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

}