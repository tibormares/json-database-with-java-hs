package server;

import com.google.gson.JsonElement;

public class Response {
    private String response;
    private JsonElement value;
    private String reason;

    public Response(String response, JsonElement value, String reason) {
        this.response = response;
        this.value = value;
        this.reason = reason;
    }

    public Response(String response, String reason) {
        this.response = response;
        this.reason = reason;
    }

    public Response(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public JsonElement getValue() {
        return value;
    }

    public String getReason() {
        return reason;
    }
}
