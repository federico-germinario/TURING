package Server;
import java.rmi.Remote;
import java.rmi.RemoteException;

import Client.NotifyInterface;
import Turing.exceptions.UsernameAlreadyExistsException;

/** 
 * Interfaccia RMI per la registrazione e gestione notifiche 
 * 
 */ 
public interface RegistrationService extends Remote {
	
	void registerUser(String name, String password) throws RemoteException, UsernameAlreadyExistsException;
	void registerForNotify(NotifyInterface client, String username) throws RemoteException;
}
