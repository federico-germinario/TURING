package Client;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JTextArea;

import Server.RegistrationService;
import Turing.Configuration;
import Turing.Message;
import Turing.exceptions.AlreadyOnlineException;
import Turing.exceptions.AuthorizationDeniedException;
import Turing.exceptions.CreateDocumentException;
import Turing.exceptions.DocumentsListException;
import Turing.exceptions.EditException;
import Turing.exceptions.EditInProgressException;
import Turing.exceptions.EndEditException;
import Turing.exceptions.InvalidPasswordException;
import Turing.exceptions.InvalidResponseException;
import Turing.exceptions.InviteException;
import Turing.exceptions.LogoutException;
import Turing.exceptions.NotifyException;
import Turing.exceptions.ShowDocumentException;
import Turing.exceptions.ShowSectionException;
import Turing.exceptions.UsernameAlreadyExistsException;
import Turing.exceptions.UsernameNotFoundException;

/**
 * Gestione del client 
 * 
 */ 
public class ClientManager {
	
	private SocketChannel serverSocket;
	private String username;
	private Thread notifyThread;                             // Thread visualizzazione notifiche di invito
	private ChatMulticastReceiver chatMulticastReceiver;     // Thread ricezione messaggi multicast
	private ClientConnection clientConnection;
	private BlockingQueue<String> notifyQueue;
	
	NotifyImpl notifyImpl;
	
	public ClientManager() {
		notifyQueue = new LinkedBlockingQueue<>();  
		notifyImpl = new NotifyImpl(notifyQueue);
	}
	
	public String getUsername() {
		return username;
	}

	/**
     * Inizializza client, apertura socket
     * 
     */
	public void initClient() throws IOException {
        InetSocketAddress address = new InetSocketAddress(Configuration.SERVER_ADDRESS, Configuration.TCP_SERVER_PORT);
       	serverSocket = SocketChannel.open(address);
		System.out.println("[CLIENT] Socket aperto!");
		serverSocket.configureBlocking(false);
		this.clientConnection = new ClientConnection(serverSocket);
	}
	
	/**
     * Gestione richiesta registrazione al servizio tramite RMI
     * 
     * @param user nome utente
     * @param password password
     * @throws RemoteException errore registrazione al servizio
     * @throws NotBoundException se il nome del servizio non è stato trovato
     * @throws UsernameAlreadyExistsException se l'username è già utilizzato
     */
	public void RMIRegisterUser(String user, String password) throws RemoteException, NotBoundException, UsernameAlreadyExistsException {
		if(user == null || password == null) throw new NullPointerException();
		Registry r = LocateRegistry.getRegistry(Configuration.RMI_PORT);
		Remote RemoteObject = r.lookup("RMI-TURING");
		RegistrationService serverObject = (RegistrationService) RemoteObject;
		serverObject.registerUser(user, password);
	}
	
	/**
     * Gestione richiesta registrazione per le notifiche tramite RMI
     * 
     * @throws RemoteException errore registrazione al servizio notifiche
     * @throws NotBoundException se il nome del servizio non è stato trovato
     */
	public void RMIRegisterForNotify() throws RemoteException, NotBoundException {
		if(this.username == null) throw new NullPointerException();
		Registry r = LocateRegistry.getRegistry(Configuration.RMI_PORT);
		Remote RemoteObject = r.lookup("RMI-TURING");
		RegistrationService serverObject = (RegistrationService) RemoteObject;
		UnicastRemoteObject.exportObject(notifyImpl ,0);
		serverObject.registerForNotify(notifyImpl, this.username);
	}
	
	/**
     * Start thread gestione delle notifiche
     * 
     */
	public void startNotifyThread() {
		if(notifyQueue == null) throw new NullPointerException();
		notifyThread = new Thread(new NotifyThread(notifyQueue));
		notifyThread.start();
	}
	
	/**
     * Stop thread gestione delle notifiche
     * 
     */
	public void stopNotifyThread() {
		notifyThread.interrupt();
	}
	
	/**
     * Start thread gestione ricezione messaggi chat multicast
     * 
     * @param chatArea area su cui scrivere i messaggi ricevuti
     * @param address indirizzo multicast su cui si vuole ricevere i messaggi
     */
	public void startChatReceiverThread(JTextArea chatArea, String address) {
		if(chatArea == null || address == null) throw new NullPointerException();
		chatMulticastReceiver = new ChatMulticastReceiver(chatArea, address);
		Thread chatMulticastReceiverThread = new Thread(chatMulticastReceiver);
		chatMulticastReceiverThread.start();
	}
	
	/**
     * Stop thread gestione ricezione messaggi chat multicast
     * 
     */
	public void stopChatReceiverThread() {
		chatMulticastReceiver.close();
	}
	
	/**
     * Gestione richiesta login 
     * 
     * @param username 
     * @param password
     * @throws IOException errore invio/ricezione messaggio
     * @throws InvalidPasswordException se la password non è valida
     * @throws UsernameNotFoundException se l'username non è stato trovato
     * @throws InvalidResponseException se la risposta del server non è valida
     * @throws AlreadyOnlineException se l'utente è gia online
	 * @throws NotifyException se non è stato possibile notificare i messaggi pendenti
	 * @throws NotBoundException 
     */
	public void login(String username, String password) throws IOException, InvalidPasswordException, UsernameNotFoundException, InvalidResponseException, AlreadyOnlineException, NotifyException, NotBoundException {
		if(username == null || password == null) throw new NullPointerException();
		this.username = username;
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di login");
		Message msgSend = new Message(Message.LOGIN, username + "\n" + password);
		
		// Registazione servizio notifiche
		this.RMIRegisterForNotify();
		
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
    	switch (msgReceived.getType()) {
            case Message.LOGIN_OK:
            	break;
            case Message.LOGIN_ERROR_USERNAME:
            	UnicastRemoteObject.unexportObject(notifyImpl, true);
            	throw new UsernameNotFoundException();
            case Message.LOGIN_ERROR_ALREADY_ONLINE:
            	UnicastRemoteObject.unexportObject(notifyImpl, true);
            	throw new AlreadyOnlineException();
            case Message.LOGIN_ERROR_PASSWORD:
            	UnicastRemoteObject.unexportObject(notifyImpl, true);
            	throw new InvalidPasswordException();
            case Message.NOTIFY_ERROR:
            	throw new NotifyException();
            default:
            	UnicastRemoteObject.unexportObject(notifyImpl, true);
                throw new InvalidResponseException();
        }
    }
	
	/**
     * Gestione richiesta logout 
     * 
     * @throws IOException errore invio/ricezione messaggio
     * @throws LogoutException se il logout è fallito
     * @throws InvalidResponseException se la risposta del server non è valida
     */
	public void logout() throws IOException, LogoutException, InvalidResponseException {
		if(this.username == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di logout");
		Message msgSend = new Message(Message.LOGOUT, username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
    	switch (msgReceived.getType()) {
            case Message.LOGOUT_OK:
            	serverSocket.close();
            	System.out.println("[CLIENT] Socket chiuso!");
            	break;
            case Message.LOGOUT_ERROR:
            	throw new LogoutException();
            default:
                throw new InvalidResponseException();
        }
	}
	
	/**
     * Gestione richiesta creazione documento
     * 
     * @param name nome documento
     * @param nSections numero sezioni del documento
     * @throws IOException errore invio/ricezione messaggio
     * @throws InvalidResponseException se la risposta del server non è valida
	 * @throws CreateDocumentException se la richiesta è fallita
     */
	public void createDocument(String name, int nSections) throws IOException, InvalidResponseException, CreateDocumentException {
		if(name == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di creazione documento: " + name);
		Message msgSend = new Message(Message.CREATE_DOCUMENT, name + "\n" + nSections + "\n" + this.username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
    	switch (msgReceived.getType()) {
            case Message.CREATE_DOCUMENT_OK:
            	break;
            case Message.CREATE_DOCUMENT_ERROR:
            	throw new CreateDocumentException();
            case Message.FILE_ALREADY_EXISTS:
            	throw new FileAlreadyExistsException(null);
            default:
                throw new InvalidResponseException();
        }
	}
	
	/**
     * Gestione richiesta lista dei documenti 
     * ricevo: nameDocument[nSections] (Creatore: creatorName)\n
     * 
     * @return Lista dei documenti + informazioni associate
     * @throws IOException errore invio/ricezione messaggio
     * @throws InvalidResponseException se la risposta del server non è valida
     * @throws DocumentsListException se la richiesta è fallita
     */
	public String getListDocuments() throws IOException, InvalidResponseException, DocumentsListException {
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta lista documenti");
		Message msgSend = new Message(Message.DOCUMENTS_LIST, username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
    	switch (msgReceived.getType()) {
            case Message.DOCUMENTS_LIST_OK:
            	return msgReceived.getPayloadString();
            case Message.DOCUMENTS_LIST_ERROR:
            	throw new DocumentsListException();
            default:
                throw new InvalidResponseException();
        }
	}
	
	/**
     * Gestione richiesta invito 
     * 
     * @param nameDocument nome documento
     * @param guest username da invitare
     * @throws IOException errore invio/ricezione messaggio
     * @throws InvalidResponseException se la risposta del server non è valida
	 * @throws NotifyException se non è stato possibile inviare la notifica di invito 
     * @throws DocumentNotRecognizingException se la richiesta è fallita
     */
	public void inviteUser(String nameDocument, String guest) throws IOException, InvalidResponseException, InviteException, NotifyException {
		if(nameDocument == null || guest == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di invito documento: " + nameDocument + " a " + guest);
		Message msgSend = new Message(Message.INVITE, nameDocument + "\n" + guest + "\n" + username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
		switch (msgReceived.getType()) {
        	case Message.INVITE_OK:
        		break;
        	case Message.INVITE_ERROR:
        		throw new InviteException();
        	case Message.NOTIFY_ERROR:
        		throw new NotifyException();
        	default:
        		throw new InvalidResponseException();
		}
	}
	
	/**
     * Gestione richiesta editing di una sezione di un documento 
     * ricevo: addressMulticast + "\n" + docText
     * 
     * @param section sezione da editare
     * @param nameDocument nome documento da editare
     * @return Contenuto della sezione del documento da editare
     * @throws IOException errore invio/ricezione messaggio
     * @throws EditException se la richiesta è fallita
     * @throws EditInProgressException se un utente sta editando la sezione 
     * @throws AuthorizationDeniedException se non si dispone dell' autorizzazione per accedere al documento
     * @throws InvalidResponseException se la risposta del server non è valida
     */
	public String edit(int section, String nameDocument) throws IOException, EditException, EditInProgressException, AuthorizationDeniedException, InvalidResponseException {
		if(nameDocument == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di editing documento: " + nameDocument + "[" + section + "]");
		Message msgSend = new Message(Message.EDIT, section + "\n" + nameDocument + "\n" + username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
		switch (msgReceived.getType()) {
			case Message.EDIT_OK: 
				return msgReceived.getPayloadString();
			case Message.EDIT_ERROR:
        		throw new EditException();
			case Message.EDIT_ERROR_ALREADY_EDIT:
				throw new EditInProgressException();
			case Message.EDIT_ERROR_AUTHORIZATION:
        		throw new AuthorizationDeniedException();
        	default:
        		throw new InvalidResponseException();
		}
	}
	
	/**
     * Gestione richiesta di fine editing di una sezione di un documento 
     * 
     * @param text nuovo contenuto della sezione
     * @param section sezione 
     * @param nameDocument nome documento 
     * @param opt se 1 richiesta di salvare il nuovo contenuto nella sezione, altrimenti non salva le modifiche
     * @throws IOException errore invio/ricezione messaggio
     * @throws EndEditException se la richiesta è fallita
     * @throws InvalidResponseException se la risposta del server non è valida
     */
	public void endEdit(String text, int section, String nameDocument, int opt) throws IOException, EndEditException, InvalidResponseException {
		if(text == null || nameDocument == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di fine editing documento: " + nameDocument + "[" + section + "]");
		Message msgSend = new Message(Message.END_EDIT, opt + "\n" + section + "\n" + nameDocument + "\n" + username + "\n" + text);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
		switch (msgReceived.getType()) {
			case Message.END_EDIT_OK: 
				break;
			case Message.END_EDIT_ERROR:
        		throw new EndEditException();
        	default:
        		throw new InvalidResponseException();
		}	
	}
    	
	/**
     * Gestione richiesta di visione di una sezione 
     * ricevo: "[infoEditing]\n" + docText
     * 
     * @param section sezione da visulizzare
     * @param nameDocument nome documento 
     * @param opt se 1 richiesta di salvare il nuovo contenuto nella sezione, altrimenti non salva le modifiche
     * @return Contenuto della sezione + informazioni sull'editing 
     * @throws IOException errore invio/ricezione messaggio
     * @throws ShowSectionException se la richiesta è fallita
     * @throws AuthorizationDeniedException se non si dispone dell' autorizzazione per accedere al documento
     * @throws InvalidResponseException se la risposta del server non è valida
     */
	public String showSection(int section, String nameDocument) throws IOException, ShowSectionException, AuthorizationDeniedException, InvalidResponseException {
		if(nameDocument == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di visualizzazione sezione documento: " + nameDocument + "[" + section + "]");
		Message msgSend = new Message(Message.SHOW_SECTION, section + "\n" + nameDocument + "\n" + username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
		switch (msgReceived.getType()) {
			case Message.SHOW_SECTION_OK: 
				return msgReceived.getPayloadString();
			case Message.SHOW_SECTION_ERROR:
        		throw new ShowSectionException();  
			case Message.SHOW_SECTION_ERROR_AUTHORIZATION:
        		throw new AuthorizationDeniedException();
        	default:
        		throw new InvalidResponseException();
		}
	}
	
	/**
     * Gestione richiesta di visione di un documento 
     * ricevo: "[infoEditing]\n" + docText + "\n"
     * 
     * @param nameDocument nome documento da visulizzare
     * @return Contenuto del documento + informazioni sull'editing per ogni sezione
     * @throws IOException
     * @throws ShowDocumentException se la richiesta è fallita
     * @throws AuthorizationDeniedException se non si dispone dell' autorizzazione per accedere al documento
     * @throws InvalidResponseException se la risposta del server non è valida
     */
	public String showDocument(String nameDocument) throws IOException, ShowDocumentException, AuthorizationDeniedException, InvalidResponseException {
		if(nameDocument == null) throw new NullPointerException();
		System.out.println("[CLIENT-" + this.username + "] Invio richiesta di visualizzazione documento: " + nameDocument);
		Message msgSend = new Message(Message.SHOW_DOCUMENT, nameDocument + "\n" + username);
		Message msgReceived = clientConnection.sendReceiveTcpMsg(msgSend);
		
		switch (msgReceived.getType()) {
			case Message.SHOW_DOCUMENT_OK: 
				return msgReceived.getPayloadString();
			case Message.SHOW_DOCUMENT_ERROR:
        		throw new ShowDocumentException();

			case Message.SHOW_DOCUMENT_ERROR_AUTHORIZATION:
        		throw new AuthorizationDeniedException();
        	default:
        		throw new InvalidResponseException();
		}
	}
    	
 }