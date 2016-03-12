package sdis_proj1;

public class UnknownMessageException extends Exception {
	
	private static final long serialVersionUID = -2585014697472478945L;

	public UnknownMessageException(final String messageType) {
		m_message = "received message was not " + messageType;
	}

	private final String m_message;

	@Override
	public String getMessage() {
		return m_message;
	}
}