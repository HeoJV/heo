package heo.exception;

import heo.http.HttpStatusCode;

public class MethodNotAllowError extends ErrorResponse{
    public MethodNotAllowError(String message, int status) {
        super(message, status);
    }

    public MethodNotAllowError(String message) {
        super(message, HttpStatusCode.METHOD_NOT_ALLOWED);
    }
}
