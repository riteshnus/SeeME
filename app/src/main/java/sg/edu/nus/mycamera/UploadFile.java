package sg.edu.nus.mycamera;

import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ritesh on 6/11/2017.
 */

public class UploadFile {
    static Storage storage = null;
    public static InputStream inputStreamStatic;
    public static String uploadFile(String bucketName, String filePath, InputStream inputStream)throws Exception {
        Log.i("BucketName: ",bucketName+", filename:"+filePath);
        inputStreamStatic = inputStream;
        Storage storage = getStorage();
        StorageObject object = new StorageObject();
        object.setBucket(bucketName);
       // File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(filePath);

        InputStream stream = new FileInputStream(file);

        try {
            String contentType = URLConnection.guessContentTypeFromStream(stream);
            InputStreamContent content = new InputStreamContent(contentType,stream);

            Storage.Objects.Insert insert = storage.objects().insert(bucketName, null, content);
            insert.setName(file.getName());
            insert.setContentEncoding("media");
            insert.setPredefinedAcl("publicread");
            //insert.set
            StorageObject obj = insert.execute();
            return obj.getSelfLink();
        } finally {
            stream.close();
        }
    }

    private static Storage getStorage() throws Exception {

        if (storage == null) {
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = new JacksonFactory();
            List<String> scopes = new ArrayList<String>();
            scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);
            String ACCOUNT_ID_PROPERTY = "ritesh@poc-video-168306.iam.gserviceaccount.com";

            Credential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(jsonFactory)
                    .setServiceAccountId(ACCOUNT_ID_PROPERTY) //Email
                    .setServiceAccountPrivateKeyFromP12File(getTempPkc12File())
                    .setServiceAccountScopes(scopes).build();
//  notasecret
            storage = new Storage.Builder(httpTransport, jsonFactory,
                    credential).setApplicationName("POC-Video")
                    .build();
        }

        return storage;
    }

    private static File getTempPkc12File() throws IOException {
        // xxx.p12 export from google API console
        //File p12File = new File("../res/raw/poc_video.p12");
        //InputStream pkc12Stream = new FileInputStream(p12File);
        InputStream pkc12Stream = inputStreamStatic;
        //InputStream pkc12Stream = AppData.getInstance().getAssets().open("xxx.p12");
        File tempPkc12File = File.createTempFile("temp_pkc12_file", "p12");
        OutputStream tempFileStream = new FileOutputStream(tempPkc12File);

        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = pkc12Stream.read(bytes)) != -1) {
            tempFileStream.write(bytes, 0, read);
        }
        return tempPkc12File;
    }
}
