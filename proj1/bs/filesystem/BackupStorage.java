package bs.filesystem;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import bs.BackupSystem;
import bs.logging.Logger;
import bs.misc.Pair;

public class BackupStorage implements Serializable
{
	private static final long serialVersionUID = 6322327498317924624L;

	public BackupStorage(final File bsdbFile)
	{	
		localChunks = new HashMap<String, ChunkCollection>();
		remoteFiles = new HashMap<String, FileInformation>();
	}
	
	//-----------------------------------------------------

	public void dumpStorage()
	{
		System.out.println("!!! CHUNKS IN DATABASE !!!");	
		localChunks.forEach((fileId, fileInformation) -> {
			System.out.print(fileInformation.toString(fileId));
		});
	}
	
	public final void dumpRestore()
	{
		System.out.println("!!! FILES THAT CAN BE RESTORED !!!");
		System.out.print("+--------------------------------+------------------+---------+\n");
		System.out.print("| Filename                       | Length           | #Chunks |\n");
		System.out.print("+--------------------------------+------------------+---------+\n");	
		remoteFiles.forEach((fileName, fileInformation) -> {
			System.out.print(fileInformation.toString(fileName));
		});	
		System.out.print("+--------------------------------+------------------+---------+\n");
	}
	
	//-----------------------------------------------------
	
	private HashMap<String, FileInformation> remoteFiles;
	
	public final void registerRestore(final ChunkBackup paramChunks)
	{
		Logger.logDebug("registering " + paramChunks.getFileName() + " for restore...");
		remoteFiles.put(paramChunks.getFileName(), new FileInformation(paramChunks));
		BackupSystem.writeStorage();
	}
	
	public synchronized final void unregisterRestore(final String fileName)
	{
		if (remoteFiles.containsKey(fileName))
		{
			remoteFiles.remove(fileName);
			BackupSystem.writeStorage();
		}
	}
	
	public synchronized final FileInformation getRestoreInformation(final String fileId)
	{
		if (remoteFiles.containsKey(fileId))
		{
			return remoteFiles.get(fileId);
		}
		
		return null;
	}
	
	public synchronized final boolean wasBackedUp(final String fileName)
	{
		return remoteFiles.containsKey(fileName);
	}
	
	// -----------------------------------------------------
	
	private long currentSize = 0;
	private long diskCapacity = (long) (2 * 1024 * 1024);
	
	public void setCapacity(long paramBytes)
	{
		diskCapacity = paramBytes;
	}
	
	public long getCapacity()
	{
		return diskCapacity;
	}
	
	public long getUsedSpace()
	{
		return currentSize;
	}
	
	public long getFreeSpace()
	{
		return diskCapacity - currentSize;
	}
	
	// -----------------------------------------------------
	
	public synchronized final Pair<String, Integer> getMostReplicated()
	{
		Pair<Integer, Integer> mostReplicatedChunk = null;
		String mostReplicatedFile = null;

		for (final String fileId : localChunks.keySet())
		{
			final Pair<Integer, Integer> currentFile = localChunks.get(fileId).findMostReplicated();

			if (currentFile.second() > mostReplicatedChunk.second())
			{
				mostReplicatedChunk = currentFile;
				mostReplicatedFile = fileId;
			}
		}

		return new Pair<String, Integer>(mostReplicatedFile, mostReplicatedChunk.first());
	}

	//-----------------------------------------------------

	private HashMap<String, ChunkCollection> localChunks;

	public synchronized void registerPeer(final String fileId, int chunkId, int peerId)
	{
		if (!localChunks.containsKey(fileId))
		{
			Logger.logDebug("adding peerId=" + peerId + " to mirrors list...");
			localChunks.get(fileId).registerPeer(chunkId, peerId);
			BackupSystem.writeStorage();
		}
		else
		{
			//localChunks.put(fileId, new ChunkCollection());
		}
	}

	public synchronized void removePeer(final String fileId, int chunkId, int peerId)
	{
		if (localChunks.containsKey(fileId))
		{
			localChunks.get(fileId).removePeer(chunkId, peerId);
			BackupSystem.writeStorage();
		}
	}

	public synchronized int getReplicationDegree(final String fileId, int chunkId)
	{
		if (localChunks.containsKey(fileId))
		{
			return localChunks.get(fileId).getChunkInformation(chunkId).getReplicationDegree();
		}

		return 0;
	}

	public synchronized int getPeerCount(final String fileId, int chunkId)
	{
		if (localChunks.containsKey(fileId))
		{
			return localChunks.get(fileId).getChunkInformation(chunkId).getCount();
		}

		return 0;
	}
	
	// -----------------------------------------------------
	
	public synchronized ChunkInformation getChunk(final String fileId, int chunkId)
	{
		if (localChunks.containsKey(fileId))
		{
			return (localChunks.get(fileId).getChunkInformation(chunkId));
		}

		return null;
	}
	
	public synchronized boolean putChunk(final Chunk paramChunk)
	{
		final String fileId = paramChunk.getFileId();

		if (localChunks.containsKey(fileId))
		{
			if (localChunks.get(fileId).placeChunk(paramChunk))
			{
				Logger.logDebug("inserting chunk with id=" + paramChunk.getChunkId() + " into database...");
				currentSize += paramChunk.getLength();
				BackupSystem.writeStorage();
			}
			else
			{
				System.out.println("BackupStorage::chunk exists!");
			}
		}
		else
		{
			localChunks.put(fileId, new ChunkCollection());
			localChunks.get(fileId).placeChunk(paramChunk);
			currentSize += paramChunk.getLength();
			Logger.logDebug("inserting chunk with id=" + paramChunk.getChunkId() + " into database...");
			BackupSystem.writeStorage();
		}
		
		return true;
	}

	public synchronized void removeChunk(final String fileId, int chunkId)
	{
		if (localChunks.containsKey(fileId))
		{
			currentSize -= localChunks.get(fileId).removeChunk(chunkId);
			BackupSystem.writeStorage();
		}
	}
	
	public synchronized boolean hasLocalChunks(final String fileId)
	{
		return localChunks.containsKey(fileId);
	}
	
	public synchronized boolean hasChunk(final String fileId, int chunkId)
	{
		return localChunks.containsKey(fileId) && localChunks.get(fileId).chunkExists(chunkId);
	}
		
	// -----------------------------------------------------
	
	public synchronized boolean removeFile(final String fileId)
	{
		final FileManager fmInstance = BackupSystem.getFiles();
		
		if (!localChunks.containsKey(fileId))
		{
			return false;
		}
		
		Integer[] chunkIds = localChunks.get(fileId).getChunkIds();
		
		for (int i = 0; i < chunkIds.length; i++)
		{
			if (fmInstance.deleteChunk(fileId, chunkIds[i]))
			{
				removeChunk(fileId, chunkIds[i]);
			}
			else
			{
				Logger.logError("file system error while deleting chunkId=" + i + "!");
			}
		}
		
		BackupSystem.writeStorage();

		return true;
	}
}