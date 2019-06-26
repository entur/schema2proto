package no.entur.schema2proto;

public class InvalidConfigurationException extends Exception {

	public InvalidConfigurationException(String optionName) {
		super("Invalid or missing config property " + optionName);
	}

	public InvalidConfigurationException(String message, Exception e) {
		super(message, e);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

}
