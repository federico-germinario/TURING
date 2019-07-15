package Turing.exceptions;

public class UsernameNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public UsernameNotFoundException() {
        super();
    }

    public UsernameNotFoundException(String s) {
        super(s);
    }
		
}

