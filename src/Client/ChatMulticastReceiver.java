package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.swing.JTextArea;

import Turing.Configuration;

/**
 * Thread ricezione messaggi chat multicast
 * 
 */ 

public class ChatMulticastReceiver implements Runnable{
	private JTextArea chatArea;
	private String adress;
	private MulticastSocket ms;
	private boolean stop = false;
	public final int LENGTH = 256;
	
	public ChatMulticastReceiver(JTextArea chatArea, String adress) {
		if(chatArea == null || adress == null) throw new NullPointerException();
		this.chatArea = chatArea;
		this.adress = adress;
	}
	
	public void run() {
		System.out.println("[ChatMulticastReceiverThread - " + Thread.currentThread() + "] Start");
		try {
			ms = new MulticastSocket(Configuration.CHAT_PORT);
			DatagramPacket dp = new DatagramPacket(new byte[LENGTH], LENGTH);
			
			InetAddress group = InetAddress.getByName(adress);
			ms.joinGroup(group);
			
			while (!stop) {
				ms.setSoTimeout(100000000);              
				ms.receive(dp);
				String s = new String(new String(
						dp.getData(),
						dp.getOffset(),
						dp.getLength(),
						"UTF-8"));
				
				chatArea.append(s + "\n");
			}
		}catch (IOException e) {
		} finally {
			ms.close();
		}
		System.out.println("[ChatMulticastReceiverThread - " + Thread.currentThread() + "] Stop");
	}
	
	public void close() {
		stop = true;
		if(ms!= null) ms.close();
	}

}
