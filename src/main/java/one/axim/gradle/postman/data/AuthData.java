package one.axim.gradle.postman.data;

import java.util.ArrayList;

public class AuthData {
    private String type;
    private ArrayList<AuthKeyData> apiKey;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<AuthKeyData> getApiKey() {
        return apiKey;
    }

    public void setApiKey(ArrayList<AuthKeyData> apiKey) {
        this.apiKey = apiKey;
    }
}
