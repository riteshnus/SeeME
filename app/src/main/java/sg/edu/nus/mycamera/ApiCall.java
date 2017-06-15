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
    String LOG_TAG = ApiCall.class.getName();
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
            Log.i("API_Call",fileName);
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
                Log.i("post response",jsonResponse);
                JSONObject resJson = new JSONObject(jsonResponse);
                nameOfRegion = resJson.getString("name");
                Thread.sleep(6000);
                jsonResponse=callHttpGetRequest(nameOfRegion);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        Log.i("region",region);
        //region = "asia-east1.574906563948313063";
        String jsonResponse = "";
        URL newUrl = null;
        String url1 = "https://videointelligence.googleapis.com/v1/operations/asia-east1.574906563948313063?key=AIzaSyBTjFmHan4EGktatA8E8718xMinYKNg18M";
        String url2 = Constant.getUrl.trim()+region.trim()+"?key="+Constant.apiKey.trim();
        Log.i("url2",url2);
        Log.i("url1",url1);
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            newUrl = new URL(url2.toString().trim());
            if (newUrl == null)
                return jsonResponse;
            urlConnection = (HttpURLConnection) newUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type","application/json");
            urlConnection.connect();
            if(urlConnection.getResponseCode()==200){
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
                Log.i("get response",jsonResponse);
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
