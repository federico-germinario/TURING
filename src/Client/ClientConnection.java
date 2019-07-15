package Client;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import Turing.Connection;
import Turing.Message;

/**
 * Gestione comunicazione tcp client-server 
 * 
 */ 
public class ClientConnection extends Connection {
	private SocketChannel socket; 
	
	public ClientConnection(SocketChannel socket) {
		if(socket == null) throw new NullPointerException();
		this.socket = socket;
	}
	
	 /**
     * Invio e ricezione di un messaggio su un SocketChannel
     * 
     * @param replyMsg messaggio da inviare
     * @return messaggio ricevuto
     * @throws IOException errore invio messaggio
     */
	public synchronized Message sendReceiveTcpMsg(Message replyMsg) throws IOException {
		if(replyMsg == null) throw new NullPointerException();
		
		sendTcpMsg(this.socket, replyMsg);
		return receiveTCPMsg(this.socket);
	}
	

}
