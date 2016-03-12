package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileChunk
{
	private static final int MAXIMUM_CHUNK_SIZE = 64000;

	public FileChunk(final BufferedInputStream paramStream, final String paramFile,	int paramChunk) throws IOException {
		
		m_data = new byte[MAXIMUM_CHUNK_SIZE];

		final int bytesRead = paramStream.read(m_data);
		final int chunkSize = bytesRead < 0 ? 0 : bytesRead;

		m_data = Arrays.copyOf(m_data, chunkSize);
		setParameters(paramFile, paramChunk, chunkSize);
	}
	
	private int m_chunkId;
	private int m_size;
	private byte[] m_data;

	public FileChunk(final String[] paramHeader, final byte[] paramBuffer) {
		m_data = paramBuffer;
		setParameters(paramHeader[Message.FileId],
			Integer.parseInt(paramHeader[Message.ChunkId]),
			m_data.length);
	}

	public FileChunk(final String fileId, final int chunkId, final byte[] paramBuffer) {
		m_data = paramBuffer;
		setParameters(fileId, chunkId, m_data.length);
	}

	private String m_fileId;

	private void setParameters(final String fileId, final int chunkId, final int chunkSize) {
		m_fileId = fileId;
		m_chunkId = chunkId;
		m_size = m_data.length;
	}

	public final byte[] getData() {
		return m_data;
	}

	public final int getLength() {
		return m_size;
	}

	public final int getChunkId() {
		return m_chunkId;
	}

	public final String getFileId() {
		return m_fileId;
	}

	public final boolean isFirst() {
		return m_chunkId == 0;
	}

	public final boolean isLast() {
		return m_size < MAXIMUM_CHUNK_SIZE;
	}
}