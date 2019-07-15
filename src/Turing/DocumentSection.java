package Turing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Rappresenta una sezione di un documento
 * 
 */ 
public class DocumentSection {
	private final String path;
	private boolean editing;
	private final Lock editingLock;
	private final Lock fileLock;
	
	public DocumentSection(Document document, String path) { 
		if(document == null || path == null) throw new NullPointerException();
		this.path = path;
		this.editing = false; 
		this.editingLock = new ReentrantLock();
		this.fileLock = new ReentrantLock();
	}
	
	/**
     * Legge la sezione 
     * 
     * @return Testo letto
     * @throws IOException errore nella lettura del file 
     */
	public String read() throws IOException {
			ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
			FileChannel inChannel = FileChannel.open(Paths.get(path), StandardOpenOption.READ);
			try { 
				fileLock.lock();
				try {
					ByteBuffer buffer = ByteBuffer.allocate(1024);
					boolean stop=false;
					while(!stop) {
						int bytesRead = inChannel.read(buffer);
						
						if(bytesRead < 0) stop = true;
						buffer.flip();
						bytestream.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
						buffer.clear();
					}
				}finally {
					fileLock.unlock();
				}
				
			}finally{
				inChannel.close();
			}
			return bytestream.toString();
	}
	
	/**
     * Scrive nella sezione
     * 
     * @param text testo da scrivere sul file
     * @throws IOException errore nella scrittura del file
     */
	public void write(String text) throws IOException {
		if(text == null) throw new NullPointerException();
		FileChannel outChannel = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE);
		try {
			fileLock.lock();
			try {
				byte[] bytes = text.getBytes();
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				outChannel.write(buffer);
			}finally {
				fileLock.unlock();
			}
		}finally{
			outChannel.close();
		}	
	}
	
	public void stopEditing() {
		editingLock.lock();
		editing = false;
		editingLock.unlock();
	}

	/**
     * Verifica se la sezione è in fase di editing, se non lo è, setta la varibile a true
     * 
     * @return true se la sezione è in fase di editing, false altrimenti
     */
	public boolean startEditing() {
		editingLock.lock();
		if(editing) {
			editingLock.unlock();
			return true;
		}
		this.editing = true;
		editingLock.unlock();
		return false;
	}
	
	public boolean isEditing() {
		editingLock.lock();
		try {
			return editing;
		}finally {
			editingLock.unlock();
		}
	}
}