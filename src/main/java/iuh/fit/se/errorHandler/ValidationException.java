package iuh.fit.se.errorHandler;

public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 2021359321975529694L;

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ValidationException(Throwable cause) {
		super(cause);
	}

}
