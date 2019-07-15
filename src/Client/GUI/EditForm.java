package Client.GUI;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import Client.ClientManager;
import Turing.Configuration;
import Turing.exceptions.EndEditException;
import Turing.exceptions.InvalidResponseException;

import java.awt.TextArea;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.awt.event.ActionEvent;
import javax.swing.JScrollBar;

/**
 *	Edit form
 * 
 */ 
public class EditForm {
	private JFrame frame;
	private ClientManager clientManager; 
	private String text;
	private int section;
	private String nameDoc;
	private String address;
	
	private JTextArea  inputChat;
	private TextArea textAreaDocument;
	private JButton btnEsciSenzaSalvare;
	private JFrame homeFrame;
	private JButton btnDocumenti;

	public EditForm(ClientManager clientManager, String docContent, int section, String nameDoc, String address, JFrame homeFrame, JButton btnDocumenti) {
		if(clientManager == null || docContent == null || nameDoc == null || address == null) throw new NullPointerException();
		this.clientManager = clientManager;
		this.text = docContent;
		this.section = section;
		this.nameDoc = nameDoc;
		this.address = address;
		this.homeFrame = homeFrame;
		this.btnDocumenti = btnDocumenti;
		initialize();
		frame.setVisible(true);
	}

	public void setVisibleFalse() {
		btnEsciSenzaSalvare.doClick();
	}
	
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Utente: " + clientManager.getUsername() + " [Documento: " + nameDoc + " sezione: " + section + "]");
		frame.setBounds(homeFrame.getX(), homeFrame.getY(), 666, 499);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		btnDocumenti.setEnabled(false);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(335, 10, 305, 384);
		frame.getContentPane().add(scrollPane);
		
		JTextArea  chatArea = new JTextArea();
		scrollPane.setViewportView(chatArea);
		chatArea.setLineWrap(true);
		chatArea.setEditable(false);
		
		// Avvio thread per ricezione messaggi chat
		clientManager.startChatReceiverThread(chatArea, address);
		
		inputChat = new JTextArea();
		inputChat.setLineWrap(true);
		inputChat.setBounds(336, 405, 233, 46);
		frame.getContentPane().add(inputChat);
		
		textAreaDocument = new TextArea();
		textAreaDocument.setText(text);
		textAreaDocument.setBounds(10, 10, 305, 390);
		frame.getContentPane().add(textAreaDocument);
		
		JButton btnInvia = new JButton("Invia");
		btnInvia.setSelected(true);
		btnInvia.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});
		btnInvia.setBounds(576, 406, 64, 45);
		frame.getContentPane().add(btnInvia);
		
		JButton btnEsciESalva = new JButton("Esci e salva");
		btnEsciESalva.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				if(exitEndSave() == 1) {
					JOptionPane.showMessageDialog(frame, "Documento salvato correttamente!", "", JOptionPane.INFORMATION_MESSAGE);;
					clientManager.stopChatReceiverThread();   // Stop thread ricezione messaggi multicast
					btnDocumenti.setEnabled(true);            // Riattivo bottone documenti su homeForm
				}
			}
		});
		btnEsciESalva.setBounds(168, 417, 147, 23);
		frame.getContentPane().add(btnEsciESalva);
		
		btnEsciSenzaSalvare = new JButton("Esci senza salvare");
		btnEsciSenzaSalvare.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exitWithoutSaving();
				clientManager.stopChatReceiverThread();	// Stop thread ricezione messaggi multicast
				btnDocumenti.setEnabled(true);			// Riattivo bottone documenti su homeForm
			}
		});
		btnEsciSenzaSalvare.setBounds(10, 417, 148, 23);
		frame.getContentPane().add(btnEsciSenzaSalvare);
		
		
		JScrollBar scrollBar = new JScrollBar();
		scrollBar.setBounds(623, 10, 17, 384);
		frame.getContentPane().add(scrollBar);
	}
	
	/**
     * Gestione richiesta di invio messaggio in chat
     * 
     */
	private void send() {
		try (DatagramSocket datagramSocket = new DatagramSocket()) 
		{
			String msg = "[" + clientManager.getUsername() + "]" + ": " + inputChat.getText();
			inputChat.setText("");
			InetAddress group = InetAddress.getByName(address);
            DatagramPacket msgPacket = new DatagramPacket(msg.getBytes("UTF-8"), msg.getBytes("UTF-8").length, group, Configuration.CHAT_PORT);
            datagramSocket.send(msgPacket);
		}catch (IOException ex) {
			JOptionPane.showMessageDialog(frame, "Impossibile inviare il messaggio!", "Error Message", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
	
	/**
     * Gestione richiesta fine editing con salvataggio
     * 
     * @return 1 richiesta eseguita con successo, 0 altrimenti
     */
	private int exitEndSave() {
		try {
			clientManager.endEdit(textAreaDocument.getText(), section, nameDoc, 1);
			frame.setVisible(false);
			return 1;
		} catch (IOException | EndEditException | InvalidResponseException e1) {
			JOptionPane.showMessageDialog(frame, "Errore salvataggio documento!", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		}
	}
	
	/**
     *  Gestione richiesta fine editing senza salvataggio
     * 
     * @return 1 richiesta eseguita con successo, 0 altrimenti
     */
	private int exitWithoutSaving() {
		try {
			clientManager.endEdit("", section, nameDoc, 0);
			frame.setVisible(false);
			return 1;
		} catch (IOException | EndEditException | InvalidResponseException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Errore!", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		}
	}
}

