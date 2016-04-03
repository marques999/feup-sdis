package bs.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import bs.PeerGlobals;

public class ChunkBackup
{
	public ChunkBackup(final String myFile, int replicationDegree) throws IOException
	{
		final File fileDescriptor = new File("files/" + myFile);

		try
		{
			generateHash(fileDescriptor);
		}
		catch (NoSuchAlgorithmException ex)
		{
			ex.printStackTrace();
		}

		fileSize = fileDescriptor.length();
		numberChunks = (int) (fileSize / PeerGlobals.maximumChunkSize + 1);
		chunksArray = new Chunk[numberChunks];
		fileName = myFile;

		try (BufferedInputStream chunkReader = new BufferedInputStream(new FileInputStream(fileDescriptor)))
		{
			System.out.println(myFile + " will be split into " + numberChunks + " chunks.");

			for (int i = 0; i < numberChunks; i++)
			{
				chunksArray[i] = new Chunk(chunkReader, fileId, i, replicationDegree);
			}
		}
	}

	private void generateHash(final File fileDescriptor) throws NoSuchAlgorithmException
	{
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		final StringBuffer sb = new StringBuffer();
		final String digestedFile = String.format("%s/%d", fileDescriptor.getName(), fileDescriptor.length());

		md.update(digestedFile.getBytes());

		final byte byteData[] = md.digest();

		for (final byte element : byteData)
		{
			sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
		}

		fileId = sb.toString();
	}

	//-----------------------------------------------------

	private final Chunk[] chunksArray;

	public final Chunk[] getChunks()
	{
		return chunksArray;
	}

	//-----------------------------------------------------

	private String fileId;

	public final String getFileId()
	{
		return fileId;
	}

	//-----------------------------------------------------

	private final String fileName;

	public final String getFileName()
	{
		return fileName;
	}

	//-----------------------------------------------------

	private final int numberChunks;

	public final int getCount()
	{
		return numberChunks;
	}

	//-----------------------------------------------------

	private final long fileSize;

	public final long getFileSize()
	{
		return fileSize;
	}
}