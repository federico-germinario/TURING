package Client;

import java.io.IOException;

import javax.swing.JOptionPane;

import Client.GUI.LoginForm;

/**
 * Main del client
 * 
 * @author Federico Germinario 545081
 */ 
public class Client {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		ClientManager clientManager = new ClientManager();
		try {
			clientManager.initClient();
			LoginForm loginForm = new LoginForm(clientManager);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Server offline!");
		}
	}		
}
	

