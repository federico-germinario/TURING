package Turing;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Server.ServerManager;
import Server.User;
import Turing.exceptions.AuthorizationDeniedException;
import Turing.exceptions.EditInProgressException;

/**
 * Rappresenta un documento 
 * 
 */ 

public class Document {
	private final String name;                             // Nome del documento
	private final User creator;                            // Creatore del documento
	private final int nSections;                           // Numero sezioni del documento
	private final List<DocumentSection> sections;          // Sezioni del documento
	private final List<User> allowedUsers;                 // Utenti consentiti all'accesso al documento
	private final String path ;
	private ServerManager serverManager;
	private String chatAddress;                            // Indirizzo multicast gestione chat
	private Lock addressLock;

	public Document(String name,  int nSections, User creator, ServerManager serverManager) {
		if(name == null || creator == null) throw new NullPointerException();
		this.name = name;
		this.creator = creator;
		this.nSections = nSections;
		this.sections = new ArrayList<>(nSections);
		allowedUsers = new ArrayList<>();
		allowedUsers.add(creator);
		path = Configuration.DOCUMENTS_DIRECTORY + name;
		this.serverManager = serverManager;
		this.addressLock = new ReentrantLock();
		chatAddress = genRndMulticastAddress();
	}
	
	public String getNome(){
		return name;
	}
	
	public String getCreator() {
		return creator.getUsername();
	}
	
	public int getSections() {
		return nSections;
	}
	
	public DocumentSection getSection(int section) {
		return sections.get(section);
	}
	
	public String getChatAddress(){
		return chatAddress;
	}
	
	/**
     * Crea il documento con tutte le sue sezioni 
     * 
     * @throws IOException errore nella creazione dei file 
     */
	public void create() throws IOException {
		File dir = new File(path);
		if(!dir.exists()) {
			dir.mkdir();
		
			for(int i=0; i<nSections; i++) {
				File f = new File(path, name + "_" + i + ".txt");
				if(f.exists()) {
					f.delete();
				}
				f.createNewFile();
				sections.add(i, new DocumentSection(this,f.getPath()));
			}
			creator.addDocument(this, 1);   // 1: è il creatore del documento
			serverManager.addDocument(name, this);
		}else {
			throw new FileAlreadyExistsException(path);
		}
	}
	
	/**
     * Aggiunge user alla lista degli utenti consentiti 
     * 
     */
	public void addAllowedUsers(User user) {
		if(user == null) throw new NullPointerException();
		allowedUsers.add(user);
	}
		
	/**
     * Gestione richiesta edit di una sezione, legge il contenuto di una sezione del documento se rispetta tutti i vincoli e ne prende la mutua escusione 
     * 
     * @param user utente che vuole leggere la sezione 
     * @param section sezione da leggere
     * @return contenuto letto dalla sezione 
     * @throws IOException errore nella lettura del file 
     * @throws EditInProgressException se un utente sta editando questa sezione
     * @throws AuthorizationDeniedException se l'utente non ha accesso a questo documento
     */
	public String startEditSection(User user, int section) throws IOException, EditInProgressException, AuthorizationDeniedException {
		if(user == null) throw new NullPointerException();
		if(!allowedUsers.contains(user)) throw new AuthorizationDeniedException();
		DocumentSection docSection = sections.get(section);
		if(docSection.startEditing()) throw new EditInProgressException();
		user.setEditingSection(docSection);
		return docSection.read();
	}
	
	/**
     * Gestione richiesta endEdit, scrivo le modifiche nella sezione (se flag settato)
     * e rilascio la mutua esclusione 
     * 
     * @param text testo da scrivere sulla sezione 
     * @param section sezione da scrivere
     * @param opt se 1 scrivo il contenuto di text nella sezione altrimenti rilascio solo la m.e.
     * @throws IOException errore nella scrittura del file 
     */
	public void endEditSection(String text, int section, User user, int opt) throws IOException {
		if(text == null || user == null) throw new NullPointerException();
		DocumentSection docSection = sections.get(section);
		if(opt == 1) {
			docSection.write(text);
		}
		user.unlockEditingSection();
		docSection.stopEditing();
	}
	
	/**
     * Legge il contenuto di una sezione del documento  
     * 
     * @param user utente che vuole leggere la sezione 
     * @param section sezione da leggere
     * @return informazioni sull'editing della sezione + contenuto letto dalla sezione
     * @throws IOException errore nella lettura del file 
     * @throws AuthorizationDeniedException l'utente non ha accesso a questo documento
     */
	public String showSection(User user, int section) throws IOException, AuthorizationDeniedException {
		if(user == null) throw new NullPointerException();
		if(!allowedUsers.contains(user)) throw new AuthorizationDeniedException();
		DocumentSection docSection = sections.get(section);
		String editing = "Sezione: " + section + " - " + "Nessun utente sta modificando questa sezione";
		if(docSection.isEditing()) editing = "Sezione: " + section + " - " + "Un utente sta modificando questa sezione";
		return "[" + editing + "]\n" + docSection.read();
	}
	
	/**
     * Legge il contenuto del documento  
     * 
     * @param user utente che vuole leggere la sezione 
     * @param nameDocument 	nome del documento da leggere
     * @return informazioni sull'editing di tutte le sezioni del documento + contenuto letto del documento
     * @throws IOException errore nella lettura del file
     * @throws AuthorizationDeniedException l'utente non ha accesso a questo documento
     */
	public String showDocument(User user, String nameDocument) throws IOException, AuthorizationDeniedException {
		if(user == null || nameDocument == null) throw new NullPointerException();
		StringBuilder text = new StringBuilder(); 
		for(int i=0; i<getSections(); i++) {
			text.append(showSection(user, i) + "\n\n");
		}
		return text.toString();
	}
	
	/**
     * Generatore casuale di indirizzi multicast
     * 
     * @return indirizzo multicast 
     */	
	private String genRndMulticastAddress() {
		addressLock.lock();
		try {
			Random rnd = new Random();
			while(true) {
				int r1 = (int)(rnd.nextInt(255) + 1);
				int r2 = (int)(rnd.nextInt(255) + 1);
				String addr = new String();
				addr = "225.0." + Integer.toString(r1) + "." + Integer.toString(r2);
				if(serverManager.checkAddresses(addr)) return addr;
			}
		}finally {
			addressLock.unlock();
		}
	}

}