package com.zakariaabdelbaki.tp1.llm;

public class RequeteException extends Exception {

    private final int status;
    private final String requeteJson;

    public RequeteException(String message) {
        this(0, message, null);
    }

    public RequeteException(String message, String requeteJson) {
        this(0, message, requeteJson);
    }

    public RequeteException(int status, String message, String requeteJson) {
        super(message);
        this.status = status;
        this.requeteJson = requeteJson;
    }

    public int getStatus() {
        return status;
    }

    public String getRequeteJson() {
        return requeteJson;
    }
}
