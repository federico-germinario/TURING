package Client.GUI;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import Client.Client;
import Client.ClientManager;
import Turing.exceptions.AuthorizationDeniedException;
import Turing.exceptions.CreateDocumentException;
import Turing.exceptions.DocumentsListException;
import Turing.exceptions.EditException;
import Turing.exceptions.EditInProgressException;
import Turing.exceptions.InvalidResponseException;
import Turing.exceptions.InviteException;
import Turing.exceptions.LogoutException;
import Turing.exceptions.NotifyException;
import Turing.exceptions.ShowDocumentException;
import Turing.exceptions.ShowSectionException;

import javax.swing.JButton;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;

import java.awt.Font;
import javax.swing.JLabel;

/**
 *	Home form
 * 
 * @author Federico Germinario 545081
 */ 

public class HomeForm {

	private JFrame frame;
	private JFrame loginFrame;
	private ClientManager clientManager;
	private ShowForm showForm;
	private EditForm editForm;
	private JButton btnDocumenti;

	public HomeForm(ClientManager clientManager, JFrame loginFrame) {
		if(clientManager == null) throw new NullPointerException();
		this.clientManager = clientManager;
		this.loginFrame = loginFrame;
		initialize();
		frame.setVisible(true);
	}

	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Home");
		frame.setBounds(loginFrame.getX(), loginFrame.getY(), 440, 226);
		
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		 frame.addWindowListener(new WindowAdapter() {
		        @Override
		        public void windowClosing(WindowEvent e) {
		        	clientManager.stopNotifyThread();
		        	System.exit(0);
		        }
		    });
		
		JLabel lblNewLabel = new JLabel("Benvenuto/a " + clientManager.getUsername() + "!");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 24));
		lblNewLabel.setBounds(84, 11, 256, 31);
		frame.getContentPane().add(lblNewLabel);
		
		JButton btnCreaDocumento = new JButton("Crea documento");
		btnCreaDocumento.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(newDocument() == 1) JOptionPane.showMessageDialog(frame, "Documento creato correttamente!", "", JOptionPane.INFORMATION_MESSAGE);
			}
		});
	
		btnCreaDocumento.setBounds(49, 83, 139, 23);
		frame.getContentPane().add(btnCreaDocumento);
		
		JButton btnInvitaUtente = new JButton("Invita utente");
		btnInvitaUtente.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(inviteUser() == 1) JOptionPane.showMessageDialog(frame, "Invito eseguito correttamente", "", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		btnInvitaUtente.setBounds(49, 133, 139, 23);
		frame.getContentPane().add(btnInvitaUtente);
		
		btnDocumenti = new JButton("Documenti");
		btnDocumenti.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String list = getListDocuments();  // Stringa restituita: nameDocument[nSections] (Creatore: creatorName)\n
				if(list != null) {
					String listDocument[] = list.split("\n");
					String document = (String) JOptionPane.showInputDialog(frame, "Documenti", "Documenti", JOptionPane.QUESTION_MESSAGE, null, listDocument, "");
					if(document == null || document.isEmpty()) return;
					
					// Parsing selezione
					String nameDocument = document.substring(0, document.indexOf("[")).trim();
					if(nameDocument == null) return;
					try {
						int nSections = Integer.parseInt(document.substring(document.indexOf("[") + 1, document.indexOf("]")).trim());
						String[] listSections = new String[nSections];
						for(int i=0; i<nSections; i++) {
							listSections[i]= Integer.toString(i);
						}
						String[] options = {"Edita", "Mostra documento", "Mostra sezione"};
						int code = JOptionPane.showOptionDialog(frame, "Hai selezionato il documento: " + nameDocument + "\nCosa vuoi fare?", "", 0, JOptionPane.QUESTION_MESSAGE, null, options, "Edita");
						switch(code) {
							case 0: 
								edit(nameDocument, listSections);
						      	break;
						    case 1: 
						      	showDocument(nameDocument);	
								break;
						    case 2: 
						    	showSection(nameDocument, listSections);	
						    	break;
						}
					}catch(NumberFormatException e) {
						return;
					}
				}else {
					JOptionPane.showMessageDialog(frame, "Errore visualizzazione documenti!", "Error Message", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		btnDocumenti.setBounds(225, 83, 139, 23);
		frame.getContentPane().add(btnDocumenti);
		
		JButton btnLogout = new JButton("Logout");
		btnLogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				// Chiudo le finestre aperte 
				if(showForm != null) {
					showForm.setVisibleFalse();
					showForm = null;
				}
				if(editForm != null) {
					editForm.setVisibleFalse();
					editForm = null;
				}
				try {
					clientManager.logout();
					clientManager.stopNotifyThread(); // Fermo il thread visualizzazione notifiche
				} catch (IOException | LogoutException | InvalidResponseException e) {
					JOptionPane.showMessageDialog(frame, "Errore logout!", "Error Message", JOptionPane.ERROR_MESSAGE);
					return;
				}
				Client.main(null);
				frame.setVisible(false);
			}
		});
		btnLogout.setBounds(225, 133, 139, 23);
		frame.getContentPane().add(btnLogout);
	}
	
	/**
     * Richiesta di creazione documento al server
     * 
     * @return 1 se la richiesta è adata a buon fine, 0 altrimenti
     */
	private int newDocument(){
		String name = JOptionPane.showInputDialog(frame, "Nome del documento");
		if (name == null || name.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Nome non valido! Riprovare", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		}
		
		try {
			int nSections = Integer.parseInt(JOptionPane.showInputDialog(frame, "Numero di sezioni"));
			clientManager.createDocument(name, nSections);
			return 1;	
		} catch(NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Numero non valido! Riprovare", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		}catch(FileAlreadyExistsException e) {
			JOptionPane.showMessageDialog(frame, "Documento già esistente!", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		} catch (IOException | InvalidResponseException | CreateDocumentException e) {
			JOptionPane.showMessageDialog(frame, "Errore creazine documento!", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		}
	}
	
	/**
     * Richiesta invito al server
     * 
     * @return 1 se la richiesta è adata a buon fine, 0 altrimenti
     */
	private int inviteUser() {
		String nameDocument = JOptionPane.showInputDialog(frame, "Nome del documento da condividere:");
		if(nameDocument == null) return 0;
		String guest = JOptionPane.showInputDialog(frame, "Utente da invitare");
		if(guest == null) return 0;
		
		try {
			clientManager.inviteUser(nameDocument, guest);
			return 1;
		} catch (IOException | InvalidResponseException | InviteException e) {
			JOptionPane.showMessageDialog(frame, "Errore invito, riprovare!", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 0;
		} catch (NotifyException e) {
			JOptionPane.showMessageDialog(frame, "Impossibile notificare l'invito", "Error Message", JOptionPane.ERROR_MESSAGE);
			return 1;
		}
	}
	
	/**
     * Richiesta lista documenti al server
     * 
     * @return lista documenti, null in caso di fallimento
     */
	private String getListDocuments() {
			try {
				return clientManager.getListDocuments();
			} catch (IOException | DocumentsListException | InvalidResponseException e) {
				JOptionPane.showMessageDialog(frame, "Errore visualizzazione lista documenti!", "Error Message", JOptionPane.ERROR_MESSAGE);
				return null;
			}
	}
	
	/**
     * Gestione richiesta editing di una sezione al server
     * 
     * @param nameDocument nome del documento
     * @param listSections lista possibili sezioni da editare
     */
	@SuppressWarnings("unused")
	private void edit(String nameDocument, String[] listSections) {
		if(nameDocument == null || listSections == null) throw new NullPointerException();
		try {
  			int section = Integer.parseInt((String) JOptionPane.showInputDialog(frame, "Scegli la sezione da editare", "Edit", JOptionPane.QUESTION_MESSAGE, null, listSections, "0"));
  			String s[] = clientManager.edit(section, nameDocument).split("\n", 2); 
  			String address = s[0];
  			String docContent = s[1];
  			editForm = new EditForm(clientManager, docContent, section, nameDocument, address, frame, btnDocumenti);
      		
  			}catch(NumberFormatException e) {
  				JOptionPane.showMessageDialog(frame, "Sezione non valida! Riprovare", "Error Message", JOptionPane.ERROR_MESSAGE);
  				return;
  			}catch(InvalidResponseException | IOException | EditException e) {
      			JOptionPane.showMessageDialog(frame, "Errore editing documento!", "Error Message", JOptionPane.ERROR_MESSAGE);
      			return;
			} catch (EditInProgressException e) {
				JOptionPane.showMessageDialog(frame, "Sezione occupata! Riprovare più tardi", "Error Message", JOptionPane.ERROR_MESSAGE);
				return;
			} catch (AuthorizationDeniedException e) {
				JOptionPane.showMessageDialog(frame, "Autorizzazione negata!", "Error Message", JOptionPane.ERROR_MESSAGE);
				return;
			}
	}
	
	/**
     * Gestione richiesta visione di un documento al server
     * 
     * @param nameDocument nome del documento da visionare
     */
	@SuppressWarnings("unused")
	private void showDocument(String nameDocument) {
		if(nameDocument == null) throw new NullPointerException();
		
		try {
			showForm = new ShowForm(clientManager.showDocument(nameDocument), -1, nameDocument);
		} catch (IOException| ShowDocumentException | InvalidResponseException e1) {
			JOptionPane.showMessageDialog(frame, "Errore visualizzazione documento!", "Error Message", JOptionPane.ERROR_MESSAGE);
		} catch (AuthorizationDeniedException e1) {
			JOptionPane.showMessageDialog(frame, "Errore visualizzazione documento! Autorizzazione non valida", "Error Message", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
     * Gestione richiesta visione di una sezione al server
     * 
     * @param nameDocument nome del documento
     * @param listSections lista possibili sezioni da visualizzare
     */
	@SuppressWarnings("unused")
	private void showSection(String nameDocument, String[] listSections) {
		if(nameDocument == null || listSections == null) throw new NullPointerException();
		
		try {
			int section = Integer.parseInt((String) JOptionPane.showInputDialog(frame, "Scegli la sezione da mostrare", "Edit", JOptionPane.QUESTION_MESSAGE, null, listSections, "0"));
			String docContent = clientManager.showSection(section, nameDocument);
			if(docContent == null) {
				JOptionPane.showMessageDialog(frame, "Errore visualizzazione sezione!", "Error Message", JOptionPane.ERROR_MESSAGE);
				return;
			}
			showForm = new ShowForm(docContent, section, nameDocument);
			
			} catch (NumberFormatException | IOException | ShowSectionException | InvalidResponseException e) {
				JOptionPane.showMessageDialog(frame, "Errore visualizzazione sezione!", "Error Message", JOptionPane.ERROR_MESSAGE);
			} catch (AuthorizationDeniedException e) {
				JOptionPane.showMessageDialog(frame, "Errore visualizzazione sezione! Autorizzazione non valida", "Error Message", JOptionPane.ERROR_MESSAGE);
			}
	}
}
