package Server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import Turing.Message;
import Turing.exceptions.AlreadyOnlineException;
import Turing.exceptions.AuthorizationDeniedException;
import Turing.exceptions.EditInProgressException;
import Turing.exceptions.InvalidPasswordException;
import Turing.exceptions.UsernameNotFoundException;
import Turing.Connection;
import Turing.Document;

/**
 * Task del server che elabora le risposte alle richieste di un singolo client
 * 
 */ 

public class ServerTask implements Runnable {
	private ServerManager serverManager;
	private InfoClient infoClient;
	private SelectionKey key;
	private Selector selector;
	
	public ServerTask(ServerManager serverManager, SelectionKey key, Selector selector) {
		if(serverManager == null || key == null || selector == null) throw new NullPointerException();
		this.serverManager = serverManager;
		this.infoClient = (InfoClient) key.attachment();
		this.key = key;
		this.selector = selector;
	}
	
	@Override
	public void run() {
		try {
			infoClient.requestMsg = Connection.receiveTCPMsg((SocketChannel) key.channel());
		} catch (IOException e1) {
			// Disconessione forzata client
			if(infoClient.user != null) {
				infoClient.user.setOffline();
				infoClient.user.unlockEditingSection(); 
			}
			return;
		}
		// Legge il tipo di richiesta effettuata dal client
		int op = infoClient.requestMsg.getType();
		
		// Gestione delle richieste del client
		switch(op) {
			case Message.LOGIN:
				login();
				break;
			case Message.LOGOUT:
				logout();
				break;
			case Message.CREATE_DOCUMENT:
				createDocument();
				break;
			case Message.DOCUMENTS_LIST:
				documentList();
				break;
			case Message.INVITE:
				inviteUser();
				break;
			case Message.EDIT:
				edit();
				break;
			case Message.END_EDIT:
				endEdit();
				break;
			case Message.SHOW_SECTION:
				showSection();
				break;
			case Message.SHOW_DOCUMENT:
				showDocument();
				break;
			default:
				System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta non riconosciuta!");
				break;
		}	
	    {}
	}
	
	/**
     * Gestione richiesta di login. Ricevo: username + "\n" + password
     * 
     */
	private void login(){
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta login");
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] login = new String(infoClient.requestMsg.getPayloadString()).split("\n", 2);
		String username = login[0];
		String password = login[1];
			
		try {
			User user = serverManager.login(username, password);
			
			// Gestione notifiche pendenti
			ArrayList<String> pendentNotify= user.getPendentNotify();
			for(int i=0; i<pendentNotify.size(); i++) {
				user.getCallBack().notifyEvent(pendentNotify.get(i));
			}
			
			replyMsg = new Message(Message.LOGIN_OK, "");
			infoClient.user = user;
			} catch (RemoteException e) {
				replyMsg = new Message(Message.NOTIFY_ERROR, "");
			} catch (UsernameNotFoundException e) {
				replyMsg = new Message(Message.LOGIN_ERROR_USERNAME, "");
			} catch (AlreadyOnlineException e) {
				replyMsg = new Message(Message.LOGIN_ERROR_ALREADY_ONLINE, "");
			} catch (InvalidPasswordException e) {
				replyMsg = new Message(Message.LOGIN_ERROR_PASSWORD, "");
			}
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();
	}
	
	/**
     * Gestione richiesta di logout. Ricevo: username
     * 
     */
	private void logout() {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta logout");
		Message replyMsg;
		User user = serverManager.getUser(infoClient.requestMsg.getPayloadString());
		if(user != null) {
			user.unlockEditingSection();
			user.setOffline();
			replyMsg = new Message(Message.LOGOUT_OK, "");
		}else {
			replyMsg = new Message(Message.LOGOUT_ERROR, "");
		}
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();
	}
	
	/**
     * Gestione richiesta creazione nuovo documento. Ricevo: nameDocument + "\n" + nSections + "\n" + username
     * 
     */
	private void createDocument()  {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta creazione documento" );
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] parse = new String(infoClient.requestMsg.getPayloadString()).split("\n");
		String nameDocument = parse[0];
		User user = serverManager.getUser(parse[2]);
		if(user == null) {
			replyMsg = new Message(Message.CREATE_DOCUMENT_ERROR, "");
		}else {
			try {
				int nSections = Integer.parseInt(parse[1]);
				Document document = new Document(nameDocument, nSections, user, serverManager);
				document.create();
				replyMsg = new Message(Message.CREATE_DOCUMENT_OK, "");
			}catch(FileAlreadyExistsException e) {
				replyMsg = new Message(Message.FILE_ALREADY_EXISTS, "");
			} catch (NumberFormatException | IOException e) {
				replyMsg = new Message(Message.CREATE_DOCUMENT_ERROR, "");
			}
		
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();
	}
}
	
	/**
     * Gestione richiesta invio lista documenti. Ricevo: username
     * 
     */
	private void documentList() {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta lista documenti" );
		Message replyMsg;
		
		User user = serverManager.getUser(infoClient.requestMsg.getPayloadString());
		if(user == null) { 
			replyMsg = new Message(Message.DOCUMENTS_LIST_ERROR, "");
		}else {
			ArrayList<Document> documents = user.getDocuments();
			StringBuilder s = new StringBuilder();
			for(int i = 0; i<documents.size(); i++) {
				s.append(documents.get(i).getNome() + "[" + documents.get(i).getSections() + "]" +" (Creatore: " + documents.get(i).getCreator() + ")\n"); 
			}
			replyMsg = new Message(Message.DOCUMENTS_LIST_OK, s.toString());
		}
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();
	}
	
	/**
     * Gestione richiesta invito. Ricevo: nameDocument + "\n" + guest + "\n" + username
     * 
     */
	private void inviteUser(){
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta di invito" );
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] parse = new String(infoClient.requestMsg.getPayloadString()).split("\n");
		String nameDocument = parse[0];
		User userGuest = serverManager.getUser(parse[1]);
		User user = serverManager.getUser(parse[2]);
		if(userGuest != null && user != null) {
			
			// Verifico l'autorizzazione di user alla condivisione del documento e che userGuest non abbia già l'accesso al documento
			Document doc = user.getDocumentEndVerification(nameDocument, userGuest);
			if(doc != null) {
				userGuest.addDocument(doc, 0); // Aggiungo il documento alla lista dei file editabili di userGuest (0 = non sono il creatore del documento)
				
				// Gestione notifica
				if(userGuest.isOnline()) {
					try {
						userGuest.getCallBack().notifyEvent("Username: " + userGuest.getUsername() + "\n" + "L'utente " + user.getUsername() + " ha condiviso con te il documento: " + doc.getNome());
					} catch (RemoteException e) {
						replyMsg = new Message(Message.NOTIFY_ERROR, "");
						infoClient.replyMsg = replyMsg;
						key.attach(infoClient);
						key.interestOps(SelectionKey.OP_WRITE);
						this.selector.wakeup();	
						return;
					}
					
				}else {
					userGuest.addPendentNotify("Username: " + userGuest.getUsername() + "\n" + "L'utente " + user.getUsername() + " ha condiviso con te il documento: " + doc.getNome());
				}
				replyMsg = new Message(Message.INVITE_OK, "");
				infoClient.replyMsg = replyMsg;
				key.attach(infoClient);
				key.interestOps(SelectionKey.OP_WRITE);
				this.selector.wakeup();	
				return;
			}
		}
		replyMsg = new Message(Message.INVITE_ERROR, "");
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();	
	}
	
	/**
     * Gestione richiesta editing documento. Ricevo: section + "\n" + nameDocument + "\n" + username
     * 
     */
	private void edit() {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta di editing");
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] parse = new String(infoClient.requestMsg.getPayloadString()).split("\n");
		String nameDocument = parse[1];
		User user = serverManager.getUser(parse[2]);
		Document doc = serverManager.getDocument(nameDocument);
		if(user!= null && doc != null) {
			String docText;
			try {
				int section = Integer.parseInt(parse[0]);
				docText = doc.getChatAddress() + "\n" + doc.startEditSection(user, section);
				replyMsg = new Message(Message.EDIT_OK, docText);
			} catch (IOException | NumberFormatException e1) {
				replyMsg = new Message(Message.EDIT_ERROR, "");
			} catch (EditInProgressException e1) {
				replyMsg = new Message(Message.EDIT_ERROR_ALREADY_EDIT, "");
			} catch (AuthorizationDeniedException e1) {
				replyMsg = new Message(Message.EDIT_ERROR_AUTHORIZATION, "");
			}
		}else {
			replyMsg = new Message(Message.EDIT_ERROR, "");
		}
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();	
	}
	
	/**
     * Gestione richiesta fine editing documento. Ricevo: option + "\n" + section + "\n" + nameDocument + "\n" + username + "\n" + docText
     * 
     */
	private void endEdit() {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta di fine editing");
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] parse = new String(infoClient.requestMsg.getPayloadString()).split("\n", 5);
		String nameDocument = parse[2];
		User user = serverManager.getUser(parse[3]);
		Document doc = serverManager.getDocument(nameDocument);
		if(nameDocument != null || user != null || doc != null) {
			try {
				int opt = Integer.parseInt(parse[0]);
				int section = Integer.parseInt(parse[1]);
				String docText = parse[4];
				doc.endEditSection(docText, section, user, opt);
				replyMsg = new Message(Message.END_EDIT_OK, "");
			} catch (IOException | NumberFormatException e1) {
				replyMsg = new Message(Message.END_EDIT_ERROR, "");
			}
		}else {
			replyMsg = new Message(Message.END_EDIT_ERROR, "");
		}
		
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();	
	}
	
	/**
     * Gestione richiesta visione sezione di un documento. Ricevo: section + "\n" + nameDocument + "\n" + username
     * 
     */
	private void showSection() {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta visione sezione");
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] parse = new String(infoClient.requestMsg.getPayloadString()).split("\n");
		String nameDocument = parse[1];
		User user = serverManager.getUser(parse[2]);
		Document doc = serverManager.getDocument(nameDocument);
		if(nameDocument != null || user != null || doc != null) {
			try {
				int section = Integer.parseInt(parse[0]);
				String docText = doc.showSection(user, section);
				replyMsg = new Message(Message.SHOW_SECTION_OK, docText);
				} catch (AuthorizationDeniedException e) {
					replyMsg = new Message(Message.SHOW_SECTION_ERROR_AUTHORIZATION, "");
				} catch (IOException | NumberFormatException e) {
					replyMsg = new Message(Message.SHOW_SECTION_ERROR, "");
				}
		}else {
			replyMsg = new Message(Message.SHOW_SECTION_ERROR, "");
		}
		
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();
	}
	
	/**
     * Gestione richiesta visione di un documento. Ricevo: nameDocument + "\n" + username
     * 
     */
	private void showDocument() {
		System.out.println("[ServerTask - " + Thread.currentThread().getName() + "] Richiesta visione documento" );
		Message replyMsg;
		
		// Parsing messaggio ricevuto
		String[] parse = new String(infoClient.requestMsg.getPayloadString()).split("\n");
		String nameDocument = parse[0];
		User user = serverManager.getUser(parse[1]);
		Document doc = serverManager.getDocument(nameDocument);
		if(nameDocument != null || user != null || doc != null) {
			try {
				String docText = doc.showDocument(user, nameDocument);
				replyMsg = new Message(Message.SHOW_DOCUMENT_OK, docText);
			} catch (IOException e) {
				replyMsg = new Message(Message.SHOW_DOCUMENT_ERROR, "");
			} catch (AuthorizationDeniedException e) {
				replyMsg = new Message(Message.SHOW_DOCUMENT_ERROR_AUTHORIZATION, "");
			}
		}else {
			replyMsg = new Message(Message.SHOW_DOCUMENT_ERROR, "");
		}
		
		infoClient.replyMsg = replyMsg;
		key.attach(infoClient);
		key.interestOps(SelectionKey.OP_WRITE);
		this.selector.wakeup();	
	}
	
}
