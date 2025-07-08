package client;

import com.beust.jcommander.Parameter;

public class Request {
    @Parameter(names = "-t", description = "Type of request", required = true)
    private String type;

    @Parameter(names = "-k", description = "Key")
    private String key;

    @Parameter(names = "-v", description = "Value")
    private String value;

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
