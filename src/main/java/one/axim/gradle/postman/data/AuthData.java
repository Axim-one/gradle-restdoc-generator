package one.axim.gradle.postman.data;

import java.util.ArrayList;

public class AuthData {
    private String type;
    private ArrayList<AuthKeyData> apiKey;
    private ArrayList<AuthKeyData> bearer;
    private ArrayList<AuthKeyData> basic;

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

    public ArrayList<AuthKeyData> getBearer() {
        return bearer;
    }

    public void setBearer(ArrayList<AuthKeyData> bearer) {
        this.bearer = bearer;
    }

    public ArrayList<AuthKeyData> getBasic() {
        return basic;
    }

    public void setBasic(ArrayList<AuthKeyData> basic) {
        this.basic = basic;
    }
}
