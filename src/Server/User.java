package Server;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Client.NotifyInterface;
import Turing.Document;
import Turing.DocumentSection;

/**
 * Rappresenta un utente 
 * 
 */ 
public class User {
	private final String username; 
	private final String password;
	private boolean online; 
	private NotifyInterface callBack;
	private ArrayList<Document> documents;               // Documenti editabili dall'utente
	private HashMap<String, Document> ownedDocuments;    // Documenti creati dall'utente
	private ArrayList<String> pendentNotify;             // Notifiche pendenti
	DocumentSection editingSection;                      // Sezione che l'utente sta editando
	private final Lock onlineLock = new ReentrantLock();
	private final Lock notifyLock = new ReentrantLock();
	

	public User(String name, String password){
		if(name == null || password == null) throw new NullPointerException();
		this.username = name;
		this.password = password;
		this.online = false;
		this.documents = new ArrayList<>();
		this.ownedDocuments = new HashMap<>();
		this.pendentNotify = new ArrayList<>();
	}
	
	/**
	 * Imposta la sezione che l'utente sta attualmente editando
	 * 
	 */
	  public void setEditingSection(DocumentSection section) {
	    this.editingSection = section;
	  }
	  
	  
	  public void unlockEditingSection() {
		  if(editingSection != null) {
			  editingSection.stopEditing();
			  editingSection = null;
		  }
	  }
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean isOnline() {
		onlineLock.lock();
		try{
			return online;
		}
		finally {
			onlineLock.unlock();
        }
	}

	/**
     * Imposta il flag online a true
     * 
     * @return true se il flag non era settato, altrimenti false (già online) 
     */
	public boolean setOnline() {
		onlineLock.lock();
		try {
			if(!this.online) {
				this.online = true;
				return true;
			}
			return false;
		}finally {
			onlineLock.unlock();
        }
	}
	
	/**
     * Imposta il flag online a false
     * 
     * @return true se l'operazione è avvenuta correttamente, altrimenti false (già offline) 
     */
		public boolean setOffline() {
			onlineLock.lock();
			try {
				if(this.online) {
					this.online = false;
					return true;
				}
				return false;
			}finally {
				onlineLock.unlock();
	        }
		}
	
   /**
	* Verifica se il documento doc è editabile dall'utente
	* 
	* @return Esito operazione 
	*/
	public boolean isInDocuments(Document doc) {
		if(doc == null) throw new NullPointerException();
		return documents.contains(doc);
	}
	
	/**
     * Aggiunge l'utente nella lista degli utenti consentiti all'accesso al documento,
     * aggiunge il documento nella lista dei documenti dell'utente e 
     * aggiunge il documento nella lista dei documenti creati dall'utente (se flag è settato)
     * 
     * @param doc documento da aggiungere
     * @param i se 1 this è il creatore del documento altrimenti no 
     */
	public void addDocument(Document doc, int i) {
		if(doc == null) throw new NullPointerException();
		doc.addAllowedUsers(this);
		documents.add(doc);
		
		if(i==1) ownedDocuments.put(doc.getNome(), doc);
	}
	
	public ArrayList<Document> getDocuments(){
		return documents;
	}
	
	/**
     * Verifica che l'utente sia il creatore del documento e che quest'ultimo non sia presente nella lista
     * dei documenti consentiti dell'utente ospite
     * 
     * @param nameDocument  nome documento 
     * @param userGuest		nome utente da invitare
     * @return Documento 
     */
	public Document getDocumentEndVerification(String nameDocument, User userGuest) {
		if(nameDocument == null || userGuest == null) throw new NullPointerException();
		Document doc = ownedDocuments.get(nameDocument);
		if(doc != null && !userGuest.isInDocuments(doc)) {
			return doc;
		}
		return null;
	}
	
	/**
     * Aggiunge un messaggio alla lista dei messaggi pendenti
     * 
     * @param notify messaggio da inviare
     */
	public void addPendentNotify(String notify) {
		if(notify == null) throw new NullPointerException();
		notifyLock.lock();
		pendentNotify.add(notify);
		notifyLock.unlock();
	}
	
	/**
     * Restituisce tutti i messaggi pendenti dell'utente
     * 
     * @return Messaggi pendenti dell'utente
     */
	public ArrayList<String> getPendentNotify() {
		notifyLock.lock();
		ArrayList<String> tmp = new ArrayList<String>(pendentNotify);
		pendentNotify.clear();
		notifyLock.unlock();
		
		return tmp;
	}
	
	public void setCallBack(NotifyInterface callBack) {
		if(callBack == null) throw new NullPointerException();
		this.callBack = callBack;
	}
	
	public NotifyInterface getCallBack() {
		return callBack;
	}
	
}
