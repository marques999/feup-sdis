package bs.filesystem;

import java.io.Serializable;

public class FileInformation implements Serializable {
	
	private static final long serialVersionUID = 5637247759122921141L;
	
	//-----------------------------------------------------
	
	public FileInformation(final String fileId, long fileSize, int numberChunks)  {
		m_fileId = fileId;
		m_fileSize = fileSize;
		m_chunks = numberChunks;
	}
	
	public FileInformation(final ChunkBackup paramChunks) {
		this(paramChunks.getFileId(), paramChunks.getFileSize(), paramChunks.getCount());
	}
	
	public String toString(final String fileName) {
		return String.format("| %-30s | %10d bytes | %7d |\n", fileName, m_fileSize, m_chunks);
	}
	
	//-----------------------------------------------------
	
	private final String m_fileId;
	
	public final String getFileId() {
		return m_fileId;
	}
	
	//-----------------------------------------------------
	
	private final int m_chunks;
	
	public final int getCount() {
		return m_chunks;
	}	
	
	//-----------------------------------------------------
	
	private final long m_fileSize;
	
	public final long getFileSize() {
		return m_fileSize;
	}
}