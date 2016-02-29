package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileObject
{
	private File m_file = null;
	private BufferedInputStream m_reader = null;

	public FileObject(final String paramFile)
	{
		m_file = new File(paramFile);
		
		try
		{
			m_reader = new BufferedInputStream(new FileInputStream(m_file));
		}
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
		}
	}

	private String getHash() throws NoSuchAlgorithmException
	{
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		final String digestedFile = String.format("%s/%d/%d", m_file.getName(), m_file.lastModified(), m_file.length());
		final StringBuffer sb = new StringBuffer();	
		
		md.update(digestedFile.getBytes());
		
		byte byteData[] = md.digest();

		for (int i = 0; i < byteData.length; i++)
		{
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}

	public FileChunk[] split() throws IOException, NoSuchAlgorithmException
	{
		final int numberChunks = (int) Math.round(m_file.length() / 64000.0) + 1;
		final String generatedId = getHash();
		final FileChunk[] result = new FileChunk[numberChunks];
		
		for (int i = 0; i < numberChunks; i++)
		{
			result[i] = new FileChunk(m_reader, generatedId, i);
		}

		return result;
	}
}