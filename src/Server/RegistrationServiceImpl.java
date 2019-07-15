package Server;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

import Client.NotifyInterface;
import Turing.exceptions.UsernameAlreadyExistsException;

/**
 * RMI per la registrazione e gestione notifiche 
 * 
 */ 
public class RegistrationServiceImpl extends RemoteServer implements RegistrationService{
	private static final long serialVersionUID = 1L;
	private final ServerManager userManager;
	
	public RegistrationServiceImpl(ServerManager userManager) throws RemoteException {
		this.userManager = userManager;
	}
	
	@Override
	public void registerUser(String name, String password) throws RemoteException, UsernameAlreadyExistsException {
		if(name == null || password == null) throw new NullPointerException();
		userManager.register(name, password);
	}
	
	@Override
	public void registerForNotify(NotifyInterface callBack, String username) throws RemoteException {
		User user = userManager.getUser(username);
		if(user != null) user.setCallBack(callBack);
	}
}