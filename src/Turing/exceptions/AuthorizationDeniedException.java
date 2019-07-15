package Turing.exceptions;

public class AuthorizationDeniedException extends Exception{
		private static final long serialVersionUID = 1L;

		public AuthorizationDeniedException() {
	        super();
	    }

	    public AuthorizationDeniedException(String s) {
	        super(s);
	    }
}
