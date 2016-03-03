package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChunkBackup
{
	private File m_file;
	private FileChunk[] m_chunks;
	private String m_fileId;
	private BufferedInputStream m_reader;

	public ChunkBackup(final String paramFile) throws NoSuchAlgorithmException
	{
		m_chunks = null;
		m_reader = null;
		m_file = new File(paramFile);

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
	}

	public String getHash()
	{
		return m_fileId;
	}

	public final FileChunk[] getChunks()
	{
		return m_chunks;
	}

	public int split() throws IOException, NoSuchAlgorithmException
	{
		final int numberChunks = (int) Math.round(m_file.length() / 64000.0) + 1;

		m_chunks = new FileChunk[numberChunks];

		for (int i = 0; i < numberChunks; i++)
		{
			m_chunks[i] = new FileChunk(m_reader, m_fileId, i);
		}

		return numberChunks;
	}
}