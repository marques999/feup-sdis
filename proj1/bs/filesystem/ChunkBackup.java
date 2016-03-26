package bs.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import bs.BackupGlobals;

public class ChunkBackup
{
	public ChunkBackup(final String fileName, int replicationDegree) throws IOException
	{
		final File fileDescriptor = new File("files/" + fileName);

		try
		{
			generateHash(fileDescriptor);
		}
		catch (NoSuchAlgorithmException ex)
		{
			ex.printStackTrace();
		}

		m_size = fileDescriptor.length();
		m_count = (int) (m_size / BackupGlobals.maximumChunkSize + 1);
		m_chunks = new Chunk[m_count];
		m_fileName = fileName;

		try (BufferedInputStream chunkReader = new BufferedInputStream(new FileInputStream(fileDescriptor)))
		{
			System.out.println(fileName + " will be split into " + m_count + " chunks.");

			for (int i = 0; i < m_count; i++)
			{
				m_chunks[i] = new Chunk(chunkReader, m_fileId, i, replicationDegree);
			}
		}
	}
	
	private void generateHash(final File fileDescriptor) throws NoSuchAlgorithmException
	{
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		final StringBuffer sb = new StringBuffer();
		final String digestedFile = String.format("%s/%d/%d",
			fileDescriptor.getName(),
			fileDescriptor.lastModified(),
			fileDescriptor.length());

		md.update(digestedFile.getBytes());

		final byte byteData[] = md.digest();

		for (final byte element : byteData)
		{
			sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
		}

		m_fileId = sb.toString();
	}

	// -----------------------------------------------------

	private final Chunk[] m_chunks;

	public final Chunk[] getChunks()
	{
		return m_chunks;
	}

	// -----------------------------------------------------

	private String m_fileId;

	public final String getFileId()
	{
		return m_fileId;
	}

	// -----------------------------------------------------

	private final String m_fileName;

	public final String getFileName()
	{
		return m_fileName;
	}

	// -----------------------------------------------------

	private final int m_count;

	public final int getCount()
	{
		return m_count;
	}

	// -----------------------------------------------------

	private final long m_size;

	public final long getFileSize()
	{
		return m_size;
	}
}