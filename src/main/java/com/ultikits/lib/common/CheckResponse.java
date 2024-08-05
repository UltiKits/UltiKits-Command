package com.ultikits.lib.exceptions;

import lombok.Getter;

@Getter
public class CheckResponse {
    public static CheckResponse SUCCESS = new CheckResponse();
    private final String message;
    private final boolean success;

    public CheckResponse() {
        this.message = "";
        this.success = true;
    }

    public CheckResponse(String message) {
        this.message = message;
        this.success = false;
    }

}
