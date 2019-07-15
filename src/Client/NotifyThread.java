package Client;

import java.util.concurrent.BlockingQueue;

import javax.swing.JOptionPane;

/**
 * Thread per la visualizzazione della notifica di invito documento a un utente
 * 
 */ 

public class NotifyThread implements Runnable{
	private BlockingQueue<String> notifyQueue;
	
	public NotifyThread(BlockingQueue<String> notifyQueue){
		this.notifyQueue = notifyQueue;
	}

	@Override
	public void run() {
		System.out.println("[NotifyThread - " + Thread.currentThread() + "] Start");
		while(true) {
			try {
				String[] s = notifyQueue.take().split("\n", 2);
				String userGuest = s[0]; 
				String msg = s[1];
				JOptionPane.showMessageDialog(null, msg, userGuest , JOptionPane.INFORMATION_MESSAGE);
			} catch (InterruptedException e) {
				System.out.println("[NotifyThread - " + Thread.currentThread() + "] Stop");
				return;
			}
		}
	}

}
