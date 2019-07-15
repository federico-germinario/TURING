package Turing.exceptions;

public class InvalidResponseException extends Exception{
	private static final long serialVersionUID = 1L;

	public InvalidResponseException() {
        super();
    }

    public InvalidResponseException(String s) {
        super(s);
    }

}
