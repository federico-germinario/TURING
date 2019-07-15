package Turing.exceptions;

public class EditInProgressException extends Exception{
	private static final long serialVersionUID = 1L;
	
	public EditInProgressException() {
		super();
	}
	
	public EditInProgressException(String s) {
		super(s);
	}

}
