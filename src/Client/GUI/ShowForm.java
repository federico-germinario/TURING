package Client.GUI;

import javax.swing.JFrame;
import java.awt.TextArea;

/**
 *	Show form
 * 
 * @author Federico Germinario 545081
 */ 
public class ShowForm {

	private JFrame frame;
	private String text;
	private int section;
	private String nameDocument;

	public ShowForm(String text, int section, String nameDocument) {
		this.text = text;
		this.section = section;
		this.nameDocument = nameDocument;
		initialize();
		frame.setVisible(true);
	}
	
	public void setVisibleFalse() {
		frame.setVisible(false);
	}

	private void initialize() {
		frame = new JFrame();
		String s = " Sezione: " + section;
		if(section == -1) s = "";
		frame.setTitle("Documento: " + nameDocument + s);
		frame.setBounds(100, 100, 456, 448);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		TextArea textArea = new TextArea();
		textArea.setText(text);
		textArea.setEditable(false);
		textArea.setBounds(10, 10, 420, 389);
		frame.getContentPane().add(textArea);
	}
}
