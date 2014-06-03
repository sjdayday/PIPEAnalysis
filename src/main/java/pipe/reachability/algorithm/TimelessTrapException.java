package pipe.reachability.algorithm;

/**
 * Timeless trap exception is thrown when state space exploration cannot
 * get out of a cyclic vanishing state.
 */
public class TimelessTrapException extends Exception {
    /**
     * Registers the underlying cause of the timeless trap exception
     * @param cause
     */
    public TimelessTrapException(Throwable cause) {
        super(cause);
    }

    /**
     * Contains no message or cause
     */
    public TimelessTrapException() {
        super();
    }

    /**
     * Exception with the following message
     * @param message
     */
    public TimelessTrapException(String message) {
        super(message);
    }

    /**
     * Exception with message and cause
     * @param message
     * @param cause
     */
    public TimelessTrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
