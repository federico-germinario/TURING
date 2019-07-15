package Turing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Gestione comunicazione tcp server-client
 * 
 */ 

public class Connection {
	
	 /**
     * Invio di un messaggio su un SocketChannel
     * 
     * @param socket SocketChannel su cui inviare il messaggio
     * @param replyMsg messaggio da inviare
     * @throws IOException errore invio messaggio
     */
	public static void sendTcpMsg(SocketChannel socket, Message replyMsg) throws IOException {
		if(socket == null || replyMsg == null) throw new NullPointerException();
		
		// Serializzazione del messaggio
		byte[] b = getByteFromObject(replyMsg);
		
		// Buffer che contine la dimensione del messaggio
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);  
        sizeBuffer.putInt(b.length);
        
        sizeBuffer.flip();
        
        // Invio la dimensione del messaggio
        while(sizeBuffer.hasRemaining())
        	socket.write(sizeBuffer);
        
        // Invio il messaggio 
        ByteBuffer msgBuffer = ByteBuffer.wrap(b);
        while(msgBuffer.hasRemaining())
        	socket.write(msgBuffer);
    }
	
	 /**
     * Ricezione di un messaggio su SocketChannel
     * 
     * @param socket SocketChannel su cui ricevere il messaggio
     * @return messaggio 
     * @throws IOException errore ricezione messaggio
     */
   public static Message receiveTCPMsg(SocketChannel socket) throws IOException {

	   // Ricezione dimensione del messaggio
	   ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
	   while(sizeBuffer.remaining() != 0){
		   int read = socket.read(sizeBuffer);
		   if(read == -1) throw new IOException();
	   }
	   
	   sizeBuffer.flip();
	   
	   // Ricezione messaggio serializzato
	   ByteBuffer msgBuffer = ByteBuffer.allocate(sizeBuffer.getInt(0));
	   while(msgBuffer.remaining() != 0){
		   int read = socket.read(msgBuffer);
		   if(read == -1) throw new IOException();
	   }
  
	   // Deserializzazione del messaggio 
	   return (Message) getObjectFromByte(msgBuffer.array());	
	   
   }
   
   /**
    * Serializzazione oggetto
    * 
    * @param o oggetto da serializzare
    * @return oggetto serializzato
    */	
	private static byte[] getByteFromObject(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try(ObjectOutputStream out = new ObjectOutputStream(bos);){
			out.writeObject(o);
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
       }
		return bos.toByteArray();
		
	}
	
	// Deserializza un oggetto
	 /**
    * Deserializzazione oggetto
    * 
    * @param b byte da deserializzare
    * @return oggetto de-serializzato
    */	
	private static Object getObjectFromByte(byte[] b) {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		
		try(ObjectInputStream in = new ObjectInputStream(bis);){
			return in.readObject();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
       }
		catch(ClassNotFoundException ex){
			ex.printStackTrace();
			return null;
		}
	}
   
}
