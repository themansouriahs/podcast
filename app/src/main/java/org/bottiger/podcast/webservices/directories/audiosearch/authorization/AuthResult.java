package org.bottiger.podcast.webservices.directories.audiosearch.authorization;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by aplb on 12-12-2016.
 */
public class AuthResult {
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("token_type")
    public String tokenType;

    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
