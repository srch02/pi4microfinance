package pi.db.piversionbd.controller;

/**
 * Thrown when pre-registration is rejected (duplicate CIN, blacklist, excluded condition)
 * or when a requested pre-registration is not found.
 */
public class PreRegistrationException extends RuntimeException {

    public PreRegistrationException(String message) {
        super(message);
    }

    public PreRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
