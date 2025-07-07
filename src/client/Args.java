package client;

import com.beust.jcommander.Parameter;

public class Args {
    @Parameter(names = "-t", description = "Type of request", required = true)
    private String type;

    @Parameter(names = "-i", description = "Index", required = false)
    private int index;

    @Parameter(names = "-m", description = "Message", required = false)
    private String message;

    public String getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public String getMessage() {
        return message;
    }

}
