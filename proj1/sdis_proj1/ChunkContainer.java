package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;

public class ChunkContainer
{
	private File m_file;
	private String m_fileId;
	private String m_realId;
	private HashMap<Integer, FileChunk> m_chunks;
	private BufferedInputStream m_reader;

	public ChunkContainer(final File paramFile, boolean readOnly) throws NoSuchAlgorithmException, IOException
	{
		m_reader = null;
		m_chunks = new HashMap<>();
		m_file = paramFile;
		m_readonly = readOnly;
		
		if (m_readonly)
		{
			try
			{
				m_reader = new BufferedInputStream(new FileInputStream(m_file));
			}
			catch (FileNotFoundException ex)
			{
				ex.printStackTrace();
			}
			
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			final StringBuffer sb = new StringBuffer();
			final String digestedFile = String.format("%s/%d/%d", m_file.getName(), m_file.lastModified(), m_file.length());

			md.update(digestedFile.getBytes());

			final byte byteData[] = md.digest();

			for (final byte element : byteData)
			{
				sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
			}

			m_fileId = sb.toString();
			m_realId = paramFile.getName();
			m_size = paramFile.length();
			
			int numberChunks = (int) Math.round(m_file.length() / 64000.0) + 1;

			for (int i = 0; i < numberChunks; i++)
			{
				m_chunks.put(i, new FileChunk(m_reader, m_fileId, i));
			}
		}
		else
		{
			m_realId = m_fileId;
		}
	}
	
	private boolean m_readonly;
	private long m_size;
	
	public final FileChunk getChunk(int chunkId) {
		
		if (m_chunks.containsKey(chunkId)) {
			return m_chunks.get(chunkId);
		}

		return null;
	}
	
	/*
	 * This method removes a chunk from the hash map
	 * @throws BadChunkException
	 */
	public final void removeChunk(int chunkId) {

		// ------------------------------------------
		// 2) check if we can modify container and if chunk exists
		// ------------------------------------------

		if (m_chunks.containsKey(chunkId))
		{
			m_size -= m_chunks.remove(chunkId).getLength();
		}
	}
	
	/*
	 * This method adds a chunk to the hash map
	 * @throws BadChunkException
	 */
	public final void putChunk(final FileChunk chunk) throws BadChunkException{
		
		// -----------------------------------
		// 1) check if we're accepting chunks
		// -----------------------------------
		
		if (m_readonly) {
			return;
		}

		// -----------------------------------
		// 2) check if received a valid chunk
		// -----------------------------------

		if (chunk.getChunkId() < 0 || !chunk.getFileId().equals(m_fileId)) {
			throw new BadChunkException(chunk);
		}

		// ------------------------------------------
		// 3) verify if there are no duplicate chunks
		// ------------------------------------------

		if (!m_chunks.containsKey(chunk.getChunkId())) {
			m_chunks.put(chunk.getChunkId(), chunk);
			m_size += chunk.getLength();
		}
	}

	public final String getOriginalName() {
		return m_realId;
	}
	
	public final String getServerName() {
		return m_fileId;
	}
	
	public int getNumberChunks() {
		return m_chunks.size();
	}
	
	public int getSize() {
		return (int) m_size;
	}

	public final Collection<FileChunk> getCollection() {
		return m_chunks.values();
	}
	
	/*
	 * This method returns the received file chunks as an array of bytes
	 * @throws MissingChunksException, BadChunkException
	 */
	public byte[] join() throws MissingChunksException, BadChunkException {
		
		// ------------------------------------------
		// 1) check if we're accepting join operation
		// ------------------------------------------
		
		if (m_readonly) {
			return null;
		}
				
		// -------------------------------------
		// 2) verify if all chunks were received
		// -------------------------------------
		
		final int numberChunks = m_chunks.size();
		final FileChunk lastChunk = m_chunks.get(numberChunks - 1);

		if (lastChunk == null || !lastChunk.isLast()) {
			throw new MissingChunksException(m_fileId);
		}

		// -------------------------------------------------
		// 3) sort all chunks by id and calculate total size
		// -------------------------------------------------

		final FileChunk[] fileChunks = new FileChunk[numberChunks];

		for (int id = 0; id < numberChunks; id++) {
			
			if (!m_chunks.containsKey(id)) {
				throw new MissingChunksException(m_fileId);
			}

			final FileChunk currentChunk = m_chunks.get(id);

			if (currentChunk.isLast() && id != numberChunks - 1)
			{
				throw new BadChunkException(currentChunk);
			}

			fileChunks[id] = currentChunk;
		}

		// ---------------------------------
		// 4) initialize and fill data array
		// ---------------------------------

		byte[] data = new byte[(int) m_size];
		int bytesWritten = 0;

		for (int id = 0; id < numberChunks; id++) {
			
			int bytesToWrite = fileChunks[id].getLength();

			if (bytesToWrite > 0) {
				System.arraycopy(fileChunks[id].getData(), 0, data, bytesWritten, bytesToWrite);
				bytesWritten += bytesToWrite;
			}
		}

		// ------------------------------------------
		// 5) check file size after joining chunks
		// ------------------------------------------

		if (bytesWritten != m_size) {
			throw new MissingChunksException(m_fileId);
		}

		return data;
	}
}