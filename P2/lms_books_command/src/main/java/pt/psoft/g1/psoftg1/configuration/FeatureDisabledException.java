package pt.psoft.g1.psoftg1.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a feature is disabled by feature flags
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class FeatureDisabledException extends RuntimeException {

    public FeatureDisabledException(String message) {
        super(message);
    }

    public FeatureDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}


