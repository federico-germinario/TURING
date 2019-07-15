package Server;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import Turing.Document;
import Turing.exceptions.*;

/**
 * Contiene le strutture dati del server e i relativi metodi per la gestione
 * 
 */ 

public class ServerManager {
	
	private final ConcurrentHashMap<String, User> DButenti;         // Database utenti registrati
	private final ConcurrentHashMap<String, Document> DBdocuments;  // Database documenti 
	private HashSet<String> generatedAddresses; 					// Consente di verificare se un indirizzo multicast è gia stato utilizzato
	
	public ServerManager() {
		DButenti = new ConcurrentHashMap<>(); 
		DBdocuments = new ConcurrentHashMap<>();
		generatedAddresses = new HashSet<>();
	}
	
	/**
     * Registrazione utente nel DB
     * 
     * @param username
     * @param password
     * @throws UsernameAlreadyExistsException se l'utente è gia registrato
     */
	public void register(String username, String password) throws UsernameAlreadyExistsException {
		if(username == null || password == null) throw new NullPointerException();
		if(DButenti.putIfAbsent(username,  new User(username, password)) != null) throw new UsernameAlreadyExistsException(username);
	}
	
	/**
     * Gestione login utente
     * 
     * @param 	username
     * @param 	password
     * @return 	Utente registrato
     * @throws 	UsernameNotFoundException se l'utente non è registrato
     * @throws	InvalidPasswordException se la password è errata
     * @throws	AlreadyOnlineException se l'utente è già online
     */
	public User login(String username, String password) throws UsernameNotFoundException, InvalidPasswordException, AlreadyOnlineException {
		if(username == null || password == null) throw new NullPointerException();

		// Recupero l'username
		User user = DButenti.get(username);
		if(user == null) throw new UsernameNotFoundException(username); 
		
		if(!password.equals(user.getPassword())) throw new InvalidPasswordException();
		
		if(!user.setOnline()) throw new AlreadyOnlineException();
	
		return user;

	}
	
	/**
     * Verifica che un indirizzo multicast non sia già stato usato
     * 
     * @param 	address indirizzo multicast
     * @return 	Esito operazione
     */
	public boolean checkAddresses(String address) {
		if(address == null) throw new NullPointerException();
		if(generatedAddresses.contains(address)) return false;
		return generatedAddresses.add(address);
	}
	
	public User getUser(String username) {
		if(username == null) throw new NullPointerException();
		
		return DButenti.get(username);
	}
	
	/**
     * Aggiunge un documento al DB
     * 
     * @param 	nameDocument nome documento
     * @param 	doc documento
     * @return 	Documento aggiunto
     */
	public Document addDocument(String nameDocument, Document doc) {
		if(nameDocument == null || doc == null) throw new NullPointerException();
		return (DBdocuments.putIfAbsent(nameDocument, doc) == null) ? doc : null;
	}
	
	public Document getDocument(String name) {
		if(name == null) throw new NullPointerException();
		return DBdocuments.get(name);
	}
}
