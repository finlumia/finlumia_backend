package br.com.finlumia.shared.exception;

public class RateLimitException extends FinlumiaException {

    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super(429, "Muitas tentativas",
                "Limite de tentativas excedido. Tente novamente em " + retryAfterSeconds + " segundos.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
