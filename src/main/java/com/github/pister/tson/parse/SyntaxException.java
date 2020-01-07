package com.github.pister.tson.parse;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class SyntaxException extends RuntimeException {
    private static final long serialVersionUID = -2729514557013485717L;

    public SyntaxException() {
    }

    public SyntaxException(String message) {
        super(message);
    }

    public SyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyntaxException(Throwable cause) {
        super(cause);
    }
}
