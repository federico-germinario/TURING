package Client.GUI;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import Client.ClientManager;
import Turing.exceptions.AlreadyOnlineException;
import Turing.exceptions.InvalidPasswordException;
import Turing.exceptions.InvalidResponseException;
import Turing.exceptions.NotifyException;
import Turing.exceptions.UsernameAlreadyExistsException;
import Turing.exceptions.UsernameNotFoundException;

import javax.swing.JPasswordField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;
import java.awt.Font;

/**
 *	Login form
 * 
 */ 

public class LoginForm {

	private JFrame frame;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private ClientManager clientManager;
	
	
	public LoginForm(ClientManager clientManager) {
		if(clientManager == null) throw new NullPointerException();
		this.clientManager = clientManager;
		initialize();
		frame.setVisible(true);
	}
	
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("TURING");
		frame.setBounds(100, 100, 415, 271);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.getContentPane().setLayout(null);               
		
		JLabel lblUsername = new JLabel("Username");
		lblUsername.setBounds(73, 73, 105, 21);
		frame.getContentPane().add(lblUsername);
		
		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(73, 118, 86, 14);
		frame.getContentPane().add(lblPassword);
		
		usernameField = new JTextField();
		usernameField.setBounds(188, 73, 115, 21);
		frame.getContentPane().add(usernameField);
		usernameField.setColumns(10);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(188, 118, 115, 21);
		frame.getContentPane().add(passwordField);
		
		JButton btnLogin = new JButton("Login");
		
		btnLogin.addActionListener(new ActionListener() {
			@SuppressWarnings("unused")
			public void actionPerformed(ActionEvent arg0) {
				
				// Controllo username/password immessi dall'utente
				if(usernameField.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame, "Utente non valido! Riprova", "Error Message", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if(passwordField.getPassword().length == 0) {
					JOptionPane.showMessageDialog(frame, "Password non valida! Riprova", "Error Message", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if(login() == 0) return; 
				clientManager.startNotifyThread();
				
				// Login eseguito con successo 
				HomeForm homeForm = new HomeForm(clientManager, frame);
				frame.setVisible(false);
			}
		});
		btnLogin.setBounds(73, 175, 89, 23);
		frame.getContentPane().add(btnLogin);
		
		JButton btnRegistrazione = new JButton("Registrazione");
		btnRegistrazione.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				// Controllo username/password immessi dall'utente
				if(usernameField.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame, "Utente non valido! Riprova", "Error Message", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if(passwordField.getPassword().length == 0) {
					JOptionPane.showMessageDialog(frame, "Password non valida! Riprova", "Error Message", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				register();
				btnLogin.doClick();
			}
		});
		btnRegistrazione.setBounds(188, 175, 115, 23);
		frame.getContentPane().add(btnRegistrazione);
		
		JLabel lblTuring = new JLabel("TURING");
		lblTuring.setFont(new Font("Tahoma", Font.PLAIN, 30));
		lblTuring.setBounds(136, 11, 126, 31);
		frame.getContentPane().add(lblTuring);
	}
	
	/**
     * Richiesta di login al server
     * 
     * @return 1 se login è valido, 0 altrimenti
     */
	private int login() {
		try {
			clientManager.login(usernameField.getText(), new String(passwordField.getPassword()));
			return 1;
	       	} catch (IOException | InvalidResponseException | NotBoundException e) {
	       		e.printStackTrace();
				JOptionPane.showMessageDialog(frame, "Login Fallito, riprovare!", "Error Message", JOptionPane.ERROR_MESSAGE);;
				return 0;
	       	}catch (UsernameNotFoundException e) {
				JOptionPane.showMessageDialog(frame, "Username non valido, riprovare!", "Error Message", JOptionPane.ERROR_MESSAGE);;
				return 0;
	       	}catch(AlreadyOnlineException e) {
	       		JOptionPane.showMessageDialog(frame, "Utente già online!", "Error Message", JOptionPane.ERROR_MESSAGE);;
				return 0;
	       	}catch (InvalidPasswordException e) {
				JOptionPane.showMessageDialog(frame, "Password errata, riprovare!", "Error Message", JOptionPane.ERROR_MESSAGE);;
				return 0;
	       	} catch (NotifyException e) {
	       		JOptionPane.showMessageDialog(frame, "Impossibile visualizzare notifiche pendenti!", "Error Message", JOptionPane.ERROR_MESSAGE);;
				return 1;
			}
	}
 
	/**
     * Richiesta di registrazione e registrazione callback al server 
     * 
     */
	private void register(){
		try {
			clientManager.RMIRegisterUser(usernameField.getText(), new String(passwordField.getPassword()));
			JOptionPane.showMessageDialog(frame, "Registrazione avvenuta con successo", "", JOptionPane.INFORMATION_MESSAGE);
		}catch(RemoteException | NotBoundException | NullPointerException e) { 
			JOptionPane.showMessageDialog(frame, "Registrazione FALLITA: " + e.getMessage(), "Error Message", JOptionPane.ERROR_MESSAGE);
			return;
		} catch(UsernameAlreadyExistsException e) {
			JOptionPane.showMessageDialog(frame, "Registrazione FALLITA: Utente già registrato", "Error Message", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
}

