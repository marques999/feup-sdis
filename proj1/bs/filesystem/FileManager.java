package bs.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import bs.BackupSystem;
import bs.logging.Logger;

public class FileManager
{
	private final BackupStorage LocalStorage;
	private final String LocalDirectory;
	private final String ChunksDirectory;
	private final String RestoreDirectory;

	public FileManager(int peerId)
	{
		LocalDirectory = "files/";
		createDirectory(LocalDirectory);
		ChunksDirectory = "backup$" + peerId + "/";
		createDirectory(ChunksDirectory);
		RestoreDirectory = "restore$" + peerId + "/";
		createDirectory(RestoreDirectory);
		LocalStorage = BackupSystem.getStorage();
	}

	public final boolean fileExists(final String paramFile)
	{
		final File file = new File(LocalDirectory + paramFile);
		return file.exists() && file.isFile();
	}

	public final boolean chunkExists(final String fileId, int chunkId)
	{
		final File file = new File(generateFilename(fileId, chunkId));
		return file.exists() && file.isFile();
	}

	private final boolean createDirectory(final String paramDirectory)
	{
		final File file = new File(paramDirectory);

		if (!file.exists() || !file.isDirectory())
		{
			return file.mkdir();
		}

		return true;
	}

	public final boolean writeFile(final String paramFile, byte[] paramBuffer)
	{
		boolean returnValue = true;

		if (!createDirectory(RestoreDirectory))
		{
			return false;
		}

		try (final FileOutputStream out = new FileOutputStream(RestoreDirectory + paramFile))
		{
			out.write(paramBuffer);
		}
		catch (IOException ex)
		{
			System.err.println(ex.getMessage());
			returnValue = false;
		}

		return returnValue;
	}

	public final boolean deleteFile(final String fileId)
	{
		return new File(LocalDirectory + fileId).delete();
	}

	private final String generateFilename(final String fileId, int chunkId)
	{
		return ChunksDirectory + fileId + "$" + chunkId + ".chunk";
	}

	public final boolean deleteChunk(final String fileId, int chunkId)
	{
		final File file = new File(generateFilename(fileId, chunkId));

		Logger.logDebug("deleting " + file.getName() + "...");

		if (file.exists())
		{
			return file.delete();
		}

		return false;
	}

	public final boolean writeChunk(final Chunk paramChunk)
	{
		if (!createDirectory(ChunksDirectory))
		{
			return false;
		}

		final String chunkFilename = generateFilename(paramChunk.getFileId(), paramChunk.getChunkId());

		try (final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(chunkFilename)))
		{
			objectOutputStream.writeObject(paramChunk);
		}
		catch (IOException ex)
		{
			return false;
		}

		return LocalStorage.registerLocalChunk(paramChunk);
	}

	public final Chunk readChunk(final String fileId, int chunkId)
	{
		final String chunkFilename = generateFilename(fileId, chunkId);

		try (final ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(chunkFilename)))
		{
			return (Chunk) objectInputStream.readObject();
		}
		catch (IOException | ClassNotFoundException ex)
		{
			return null;
		}
	}
}