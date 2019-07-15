package Turing;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Rappresentazione di un messaggio scambiato tra client/server
 * 
 */ 

public class Message implements Serializable {
	static final long serialVersionUID = 1L;
	
	public static final int LOGIN 							  = 0;
	public static final int LOGIN_OK            			  = 1;
	public static final int LOGIN_ERROR                 	  = 2;
	
	public static final int LOGIN_ERROR_USERNAME       		  = 3;
	public static final int LOGIN_ERROR_PASSWORD     		  = 4;
	public static final int LOGIN_ERROR_ALREADY_ONLINE 		  = 5;
	public static final int LOGOUT              			  = 6;
	public static final int LOGOUT_OK           			  = 7;
	public static final int LOGOUT_ERROR 					  = 8;
	
	public static final int CREATE_DOCUMENT           		  = 9;
	public static final int CREATE_DOCUMENT_OK 				  = 10;
	public static final int CREATE_DOCUMENT_ERROR 			  = 11;
	public static final int FILE_ALREADY_EXISTS 			  = 12;
	
	public static final int DOCUMENTS_LIST					  = 13;
	public static final int DOCUMENTS_LIST_OK 				  = 14;
	public static final int DOCUMENTS_LIST_ERROR 			  = 15;

	public static final int INVITE 							  = 16;
	public static final int INVITE_OK 						  = 17;
	public static final int INVITE_ERROR 					  = 18;

	public static final int EDIT 						 	  = 19;
	public static final int EDIT_OK 						  = 20;
	public static final int EDIT_ERROR 						  = 21;

	public static final int END_EDIT 						  = 22;
	public static final int END_EDIT_OK 					  = 23;
	public static final int END_EDIT_ERROR					  = 24;
	public static final int EDIT_ERROR_ALREADY_EDIT 	 	  = 25;
	public static final int EDIT_ERROR_AUTHORIZATION 		  = 26;

	public static final int SHOW_SECTION 					  = 27;
	public static final int SHOW_SECTION_OK					  = 28;
	public static final int SHOW_SECTION_ERROR 				  = 29;
	public static final int SHOW_SECTION_ERROR_AUTHORIZATION  = 30;

	public static final int SHOW_DOCUMENT 					  = 31;
	public static final int SHOW_DOCUMENT_OK 				  = 32;
	public static final int SHOW_DOCUMENT_ERROR 			  = 33;
	public static final int SHOW_DOCUMENT_ERROR_AUTHORIZATION = 34;

	public static final int NOTIFY_ERROR  					  = 35;

	
	int type;
	byte[] payload;
	
	public Message(int type, int payload) {
		if(type < 0) throw new IllegalArgumentException();
		this.type = type;
		
		// Conversione da int a byte[] usando un buffer
		ByteBuffer b = ByteBuffer.allocate(Integer.BYTES);
		b.putInt(payload);
		this.payload = b.array();
	}
	
	public Message(int type, String payload) {
		if(type < 0) throw new IllegalArgumentException();
		if(payload == null) throw new NullPointerException();
		
		this.type = type;
		this.payload = payload.getBytes();
	}
	
	public Message(int type, byte[] payload) {
		if(type < 0) throw new IllegalArgumentException();
		if(payload == null) throw new NullPointerException();
		
		this.type = type;
		this.payload = payload;
	}
	
	public int getType() {
		return type;
	}
	
	public int getPayloadInt() {
		ByteBuffer b = ByteBuffer.wrap(payload);
		return b.getInt(0);	
	}

	public String getPayloadString() {
		String s = new String(payload);
		return s;
	}
	
	public byte[] getPayloadByte() {
		return payload;
	}
	
}