package org.grits.toolbox.editor.experimentdesigner.io;

public class ProtocolExistsException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String uri;

	public ProtocolExistsException(String message) {
		super(message);
	}
	
	public ProtocolExistsException(String message, String uri) {
		this(message);
		this.uri = uri;
	}
	
	
	public String getUri() {
		return uri;
	}
}
