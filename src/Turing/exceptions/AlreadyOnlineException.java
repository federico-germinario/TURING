package Turing.exceptions;

public class AlreadyOnlineException extends Exception{
	private static final long serialVersionUID = 1L;

	public AlreadyOnlineException() {
        super();
    }

    public AlreadyOnlineException(String s) {
        super(s);
    }
}
