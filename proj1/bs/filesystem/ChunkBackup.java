package bs.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChunkBackup {
	
	private static final long maximumChunkSize = 64000;

	public ChunkBackup(final String fileName, int replicationDegree) throws IOException {
		
		BufferedInputStream chunkReader = null;
		File fileDescriptor = new File(fileName);

		chunkReader = new BufferedInputStream(new FileInputStream(fileDescriptor));
		m_size = fileDescriptor.length();
		m_length = (int) (m_size / maximumChunkSize + 1);
		
		System.out.println(fileName + " will be split into " + m_length	+ " chunks.");
		
		try {
			
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			final StringBuffer sb = new StringBuffer();
			final String digestedFile = String.format("%s/%d/%d", fileDescriptor.getName(), fileDescriptor.lastModified(), fileDescriptor.length());

			md.update(digestedFile.getBytes());

			final byte byteData[] = md.digest();

			for (final byte element : byteData) {
				sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
			}
			
			m_fileId = sb.toString();
		}
		catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		
		m_chunks = new Chunk[m_length];
		m_fileName = fileName;

		for (int i = 0; i < m_length; i++) {
			m_chunks[i] = new Chunk(chunkReader, m_fileId, i, replicationDegree);
			System.out.println(i + ": " + m_chunks[i].getLength() + " bytes");
		}
	}
	
	//-----------------------------------------------------
	
	private final Chunk[] m_chunks;
	
	public final Chunk[] getChunks() {
		return m_chunks;
	}
	
	//-----------------------------------------------------
	
	private String m_fileId;
	
	public final String getFileId() {
		return m_fileId;
	}
	
	//-----------------------------------------------------
	
	private String m_fileName;
	
	public final String getFileName() {
		return m_fileName;
	}
	
	//-----------------------------------------------------
	
	private final int m_length;
	
	public final int getCount() {
		return m_length;
	}
	
	//-----------------------------------------------------
	
	private final long m_size;
	
	public final long getFileSize() {
		return m_size;
	}
}