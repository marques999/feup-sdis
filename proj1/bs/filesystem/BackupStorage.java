package bs.filesystem;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import bs.BackupSystem;
import bs.logging.Logger;

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
	
	public final boolean registerRestore(final ChunkBackup paramChunks)
	{
		Logger.logDebug("registering " + paramChunks.getFileName() + " for restore...");
		
		synchronized (remoteFiles)
		{
			remoteFiles.put(paramChunks.getFileName(), new FileInformation(paramChunks));
		}
		
		BackupSystem.writeStorage();
		
		return true;
	}
	
	public final boolean unregisterRestore(final String fileName)
	{
		synchronized (remoteFiles)
		{
			if (remoteFiles.containsKey(fileName))
			{
				remoteFiles.remove(fileName);
				BackupSystem.writeStorage();
			}
		}
		
		return true;
	}
	
	public final FileInformation getRestoreInformation(final String fileId)
	{
		synchronized (remoteFiles)
		{
			if (remoteFiles.containsKey(fileId))
			{
				return remoteFiles.get(fileId);
			}
		}

		return null;
	}

	// -----------------------------------------------------
	
	private long currentSize = 0;
	private long diskCapacity = (long) (2 * 1024 * 1024);
	
	public void setCapacity(long paramBytes)
	{
		diskCapacity = paramBytes;
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

	private HashMap<String, ChunkCollection> localChunks;

	public synchronized Chunk getMostReplicated()
	{
		if (localChunks.isEmpty())
		{
			return null;
		}

		int mostReplicatedChunk = -1;
		int mostReplicatedDegree = Integer.MIN_VALUE;
		
		String mostReplicatedFile = null;

		for (final String fileId : localChunks.keySet())
		{
			final HashMap<Integer, ChunkInformation> currentFile = localChunks.get(fileId).getChunks();

			for (int chunkId : currentFile.keySet())
			{
				final ChunkInformation currentChunk = currentFile.get(chunkId);
				int currentDegree = currentChunk.getCount() - currentChunk.getReplicationDegree();
				
				if (currentDegree > mostReplicatedDegree)
				{
					mostReplicatedFile = fileId;
					mostReplicatedChunk = chunkId;
					mostReplicatedDegree = currentDegree;
				}
			}
		}
		
		if (mostReplicatedFile == null || mostReplicatedChunk < 0)
		{
			return null;
		}

		return BackupSystem.getFiles().readChunk(mostReplicatedFile, mostReplicatedChunk);
	}

	public synchronized void registerPeer(final String fileId, int chunkId, int peerId)
	{
		if (localChunks.containsKey(fileId))
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

	private synchronized boolean registerChunk(final Chunk paramChunk, boolean localChunk)
	{
		final String fileId = paramChunk.getFileId();

		if (!localChunks.containsKey(fileId))
		{
			localChunks.put(fileId, new ChunkCollection());
		}
		
		long deltaBytes = localChunks.get(fileId).placeChunk(paramChunk, localChunk);
		
		if (deltaBytes >= 0)
		{		
			Logger.logDebug("inserting chunk with id=" + paramChunk.getChunkId() + " into database...");
			
			if (localChunk)
			{
				currentSize += deltaBytes;
			}
			
			BackupSystem.writeStorage();
		}
		else
		{
			Logger.logWarning("chunk with id=" + paramChunk.getChunkId() + " already exists!");
		}
		
		return true;
	}
	
	public synchronized boolean registerLocalChunk(final Chunk paramChunk)
	{
		return registerChunk(paramChunk, true);
	}
	
	public synchronized boolean registerTemporaryChunk(final Chunk paramChunk)
	{
		return registerChunk(paramChunk, false);
	}
	
	public synchronized void removeChunk(final String fileId, int chunkId)
	{
		if (localChunks.containsKey(fileId))
		{
			currentSize -= localChunks.get(fileId).removeChunk(chunkId);
			BackupSystem.writeStorage();
		}
	}
	
	public synchronized boolean hasLocalChunk(final String fileId, int chunkId)
	{
		if (localChunks.containsKey(fileId))
		{
			return localChunks.get(fileId).localChunkExists(chunkId);
		}
		
		return false;
	}
		
	// -----------------------------------------------------
	
	public synchronized boolean removeFile(final String fileId)
	{	
		if (!localChunks.containsKey(fileId))
		{
			return false;
		}
		
		final FileManager fmInstance = BackupSystem.getFiles();
		final ChunkCollection chunkCollection = localChunks.get(fileId);
		final Integer[] chunkIds = chunkCollection.getChunkIds();

		for (int i = 0 ; i < chunkIds.length; i++)
		{
			if (fmInstance.deleteChunk(fileId, chunkIds[i]))
			{
				currentSize -= chunkCollection.removeChunk(chunkIds[i]);		
			}
			else
			{
				Logger.logError("file system error while deleting chunkId=" + chunkIds[i] + "!");
			}
		}
		
		if (chunkCollection.isEmpty())
		{
			localChunks.remove(fileId);
		}
		
		BackupSystem.writeStorage();

		return true;
	}
}