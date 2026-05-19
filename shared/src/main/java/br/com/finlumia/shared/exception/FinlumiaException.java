package br.com.finlumia.shared.exception;

public class FinlumiaException extends RuntimeException {

    private final int code;
    private final String title;
    private final String message;

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

    public int getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}