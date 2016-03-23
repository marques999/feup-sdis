package bs.filesystem;

import java.util.HashMap;

public class ChunkRestore {
	
	private final String m_fileId;
	private final HashMap<Integer, Chunk> m_chunksmap;

	public ChunkRestore(final FileInformation restoreInformation) {
		m_fileId = restoreInformation.getFileId();
		m_fileSize = restoreInformation.getFileSize();
		m_length = restoreInformation.getCount();
		m_chunksmap = new HashMap<Integer, Chunk>();
	}
	
	private int m_length;
	private long m_fileSize;

	public final void put(final Chunk paramChunk) throws BadChunkException {
		
		// ------------------------------------
		// VERIFICAR SE CHUNK RECEBIDO É VÁLIDO
		// ------------------------------------

		int chunkId = paramChunk.getChunkId();

		if (chunkId < 0 || !paramChunk.getFileId().equals(m_fileId))
		{
			throw new BadChunkException(paramChunk);
		}

		// ------------------------------------------
		// VERIFICAR SE NÃO EXISTEM CHUNKS REPETIDOS
		// ------------------------------------------

		if (!m_chunksmap.containsKey(chunkId))
		{
			m_chunksmap.put(chunkId, paramChunk);
		}
	}

	public byte[] join() throws MissingChunkException, BadChunkException {
	
		int lastChunkId = m_length - 1;
	
		// --------------------------------------------
		// VERIFICAR SE OS CHUNKS FORAM TODOS RECEBIDOS
		// --------------------------------------------

		final Chunk lastChunk = m_chunksmap.get(lastChunkId);

		if (lastChunk == null || !lastChunk.isLast()) {
			throw new MissingChunkException(m_fileId);
		}

		// -------------------------------------------------
		// ORDENA CHUNKS POR ID, CALCULANDO TAMANHO 
		// -------------------------------------------------

		final Chunk[] fileChunks = new Chunk[m_length];

		for (int chunkId = 0; chunkId < m_length; chunkId++) {
			
			if (!m_chunksmap.containsKey(chunkId)) {
				throw new MissingChunkException(m_fileId);
			}

			final Chunk currentChunk = m_chunksmap.get(chunkId);

			if (currentChunk.isLast() && chunkId != lastChunkId) {
				throw new BadChunkException(currentChunk);
			}

			fileChunks[chunkId] = currentChunk;
		}

		// --------------------------------------------------
		// CONCATENA OS BYTES DOS CHUNKS RECEBIDOS NUM ARRRAY
		// --------------------------------------------------

		byte[] m_buffer = new byte[(int) m_fileSize];
		int bytesWritten = 0;

		for (int id = 0; id < m_length; id++) {
			
			int bytesToWrite = (int) fileChunks[id].getLength();

			if (bytesToWrite > 0) {
				System.arraycopy(fileChunks[id].getData(), 0, m_buffer, bytesWritten, bytesToWrite);
				bytesWritten += bytesToWrite;
			}
		}
		
		// --------------------------------------------------------
		// VERIFICAR TAMANHO DO BUFFER APÓS CONCATENAÇÃO DOS CHUNKS
		// --------------------------------------------------------

		if (bytesWritten != m_fileSize) {
			throw new MissingChunkException(m_fileId);
		}

		return m_buffer;
	}
}