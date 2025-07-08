package server;

public class Response {

    private String response;
    private String value;
    private String reason;

    public Response(String response, String value, String reason) {
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

}
