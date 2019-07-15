package Client;

import java.rmi.*;

/**
 * Interfaccia gestione callback per ricezione notifiche.
 * 
 */ 
public interface NotifyInterface extends Remote {
	
	// Metodo invocato dal server per notificare un evento al client 
	public void notifyEvent(String notify) throws RemoteException;

}
