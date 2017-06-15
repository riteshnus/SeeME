package sg.edu.nus.mycamera.dto;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;

/**
 * Created by siddharth on 6/16/2017.
 */

@DynamoDBTable(tableName = "MyObjects")
public class SeeMeObjects {

    @DynamoDBHashKey(attributeName="id")
    private String id;
    @DynamoDBAttribute(attributeName = "Latitude")
    private String latitude;
    @DynamoDBAttribute(attributeName = "Longitude")
    private String longitude;
    @DynamoDBAttribute(attributeName = "Description")
    private List<String> objectDescription;
    @DynamoDBAttribute(attributeName = "VideoUrl")
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public List<String> getObjectDescription() {
        return objectDescription;
    }

    public void setObjectDescription(List<String> objectDescription) {
        this.objectDescription = objectDescription;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
