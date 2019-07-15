package Client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.concurrent.BlockingQueue;

/**
 * Gestione callback per ricezione notifiche.
 * 
 */ 
public class NotifyImpl extends RemoteObject implements NotifyInterface {
	
	private static final long serialVersionUID = 1L;
	private BlockingQueue<String> notifyQueue;
	
	public NotifyImpl (BlockingQueue<String> notifyQueue){
		this.notifyQueue = notifyQueue;
	}

	@Override
	// Metodo richiamato dal server per inoltro notifica al client
	public void notifyEvent(String notify) throws RemoteException {
		this.notifyQueue.add(notify);
	}

}
