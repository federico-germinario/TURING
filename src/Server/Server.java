package Server;
import java.io.File;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Turing.Configuration;
import Turing.Connection;

/**
 * Main del server
 * 
 * @author Federico Germinario 545081
 */ 

public class Server {
	private static ExecutorService executor;
	private static volatile boolean stop = false;
	private static ServerSocketChannel serverSocketChannel; 
	private static Selector selector;
	private static Registry r;
	
	public static void main(String[] args) {
		ServerManager serverManager = new ServerManager();
		
		// Gestione chiusura server
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	System.out.println("[SERVER] Chiusura server in corso...");
		    	stop = true;
		    	if(selector!=null) selector.wakeup();
		    	stopServer();
		     }
		 });
		
		// Creazione cartella dei documenti 
		File dir = new File(Configuration.DOCUMENTS_DIRECTORY);
		if(dir.exists()){
			deleteDirectory(dir);
		}
		dir.mkdir();
		System.out.println("[SERVER] cartella " + Configuration.DOCUMENTS_DIRECTORY + " creata correttamente");
		
		// Registrazione servizio RMI per la registrazione degli utenti
		try {
			RegistrationServiceImpl reg = new RegistrationServiceImpl(serverManager);
			RegistrationService stub = (RegistrationService) UnicastRemoteObject.exportObject(reg, 0);
	
			// Creazione di un registry 
			LocateRegistry.createRegistry(Configuration.RMI_PORT);
			r = LocateRegistry.getRegistry(Configuration.RMI_PORT);
	
			// Pubblicazione dello stub nel registry 
			r.rebind("RMI-TURING", stub);
		}catch (RemoteException e) {
			System.out.println("[Server] Errore di comunicazione" + e.toString());
			return;
		}
		System.out.println("[SERVER] RMI attivo");
	
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(Configuration.TCP_SERVER_PORT));
			serverSocketChannel.configureBlocking(false);
		
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, null);
		}catch(IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("[SERVER] Socket aperto!"); 
		executor= Executors.newFixedThreadPool(Configuration.NUMBER_THREADS_POOL); 

		while(!stop){
			Set <SelectionKey> readyKeys;
			Iterator <SelectionKey> iterator;
			try {
				selector.select();
				readyKeys = selector.selectedKeys();
				iterator = readyKeys.iterator();
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, null);
			}catch(IOException e) {
				e.printStackTrace();
				return;
			}																							
			
			while(iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				
				try {
					// Nuova connessione
					if(key.isAcceptable()) {
						SocketChannel client = serverSocketChannel.accept();
						if(client == null) continue;
						System.out.println("[SERVER] Connessione accettata da " + client);
						client.configureBlocking(false);
						InfoClient infoClient = new InfoClient();
						client.register(selector, SelectionKey.OP_READ, infoClient); 
					}
				
					// Nuova richiesta
					else if(key.isReadable()) {
						System.out.println("[SERVER] Nuova richiesta!");
						key.interestOps(0);
						ServerTask serverTask = new ServerTask(serverManager, key, selector);
						executor.submit(serverTask);
				
					// Rispondo alla richiesta
					}else if(key.isWritable()) {
						System.out.println("[SERVER] Rispondo alla richiesta!");
						SocketChannel clientSock = (SocketChannel) key.channel();
						InfoClient infoClient = (InfoClient)key.attachment();
						Connection.sendTcpMsg(clientSock, infoClient.replyMsg);
						key.interestOps(SelectionKey.OP_READ); 
					}   
					
				}catch(IOException e) {
					// Disconnessione forzata client
					InfoClient infoClient = (InfoClient) key.attachment();
					if(infoClient.user != null) {
						infoClient.user.setOffline();
						infoClient.user.unlockEditingSection(); 
					}
				
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}	
		}	
	}
	
	/**
     * Elimina una directory
     * 
     * @param path path directory da eliminare
     * @return esito eliminazione
     */
	private static boolean deleteDirectory(File path) {
		File[] files = path.listFiles();
        for(int i=0;i<files.length; i++) {
        	if(files[i].isDirectory()) {
        		deleteDirectory(files[i]);
        	}else {
        		files[i].delete();
        	}
         }
        return(path.delete());
	}
	
	/**
     * Metodo invocato per la chiusura del server
     * 
     */
	private static void stopServer(){
		try {
			if(serverSocketChannel!=null) {
				r.unbind("RMI-TURING");
				if(selector != null) selector.close();        
				serverSocketChannel.close();
				serverSocketChannel.socket().close();
			}
			if(executor != null) {
				executor.shutdown();
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // Aspetto la terminazione dell' executor
			}
			System.out.print("[SERVER] Server chiuso!");
			}catch(Exception e) {
				e.printStackTrace();
				return;
			}
		}
	
}


