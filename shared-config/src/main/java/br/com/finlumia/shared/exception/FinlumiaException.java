package br.com.finlumia.shared.exception;

public class FinlumiaException extends RuntimeException {

    private int code;
    private String title;
    private String message;

    public FinlumiaException(int code, String title, String message) {
        super(message);
        this.code = code;
        this.title = title;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}