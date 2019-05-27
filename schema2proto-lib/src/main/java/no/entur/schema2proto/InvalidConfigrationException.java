package no.entur.schema2proto;

public class InvalidConfigrationException extends Exception {

	public InvalidConfigrationException(String optionName) {
		super("Invalid or missing config property " + optionName);
	}

	public InvalidConfigrationException(String message, Exception e) {
		super(message, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
