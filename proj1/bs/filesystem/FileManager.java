package bs.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import bs.BackupSystem;

public class FileManager
{
	public FileManager(int peerId)
	{
		LocalDirectory = "files/";
		ChunksDirectory = "backup$" + peerId + "/";
		RestoreDirectory = "restore$" + peerId + "/";
		LocalStorage = BackupSystem.getStorage();
	}

	private final BackupStorage LocalStorage;
	private final String LocalDirectory;
	private final String ChunksDirectory;
	private final String RestoreDirectory;
	
	public final boolean fileExists(final String paramFile)
	{
		final File file = new File(LocalDirectory + paramFile);
		return file.exists() && file.isFile();
	}

	private final boolean directoryExists(final String paramDirectory)
	{
		final File file = new File(paramDirectory);
		return file.exists() && file.isDirectory();
	}

	private final boolean createDirectory(final String paramDirectory)
	{
		return new File(paramDirectory).mkdir();
	}

	public final byte[] readFile(final File paramFile)
	{
		final byte[] fileBuffer = new byte[(int) paramFile.length()];

		try (final FileInputStream inputStream = new FileInputStream(paramFile))
		{
			inputStream.read(fileBuffer);
		}
		catch (IOException ex)
		{
			return null;
		}

		return fileBuffer;
	}

	public final boolean writeFile(final String paramFile, byte[] paramBuffer)
	{
		boolean returnValue = true;
	
		if (!directoryExists(RestoreDirectory))
		{
			if (!createDirectory(RestoreDirectory))
			{
				return false;
			}
		}
		
		try (final FileOutputStream out = new FileOutputStream(RestoreDirectory + paramFile))
		{
			out.write(paramBuffer);
		}
		catch (IOException ex)
		{
			System.err.println(ex.getMessage());
		}

		return returnValue;
	}

	public final boolean deleteFile(final String fileId)
	{
		boolean operationResult = new File(LocalDirectory + fileId).delete();
		
		if (operationResult)
		{
			System.out.println("[INFORMATION] Deleted " + fileId + " from " + LocalDirectory + " folder...");
		}
		else
		{
			System.out.println("[ERROR] Uh-oh, something bad happened while trying to delete " + fileId + "!");
		}
		
		return operationResult;
	}
	
	private final String generateFilename(final String fileId, int chunkId)
	{
		return ChunksDirectory + fileId + "$" + chunkId + ".chunk";
	}
		
	public final boolean deleteChunk(final String fileId, int chunkId)
	{
		final File file = new File(generateFilename(fileId, chunkId));
		return file.delete() && LocalStorage.removeChunk(fileId, chunkId);
	}

	public final boolean writeChunk(final Chunk paramChunk)
	{
		// 1) create output directory
		if (!directoryExists(ChunksDirectory))
		{
			if (!createDirectory(ChunksDirectory))
			{
				return false;
			}
		}

		// 2) write chunk
		final String chunkFilename = generateFilename(paramChunk.getFileId(), paramChunk.getChunkId());
	
		try (final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(chunkFilename)))
		{
			objectOutputStream.writeObject(paramChunk);
		}
		catch (IOException ex)
		{
			return false;
		}

		// 3) update database & disk space
		LocalStorage.putChunk(paramChunk);
		//Peer.getDisk().saveFile(data.length);
		
		return true;
	}

	public final Chunk readChunk(final String fileId, int chunkId)
	{
		final String chunkFilename = generateFilename(fileId, chunkId);
		
		try (final ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(chunkFilename)))
		{
			return (Chunk) objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		return null;
	}
}