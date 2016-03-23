package bs.filesystem;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import bs.misc.Pair;


public class BackupStorage implements Serializable
{
	private static final long serialVersionUID = 6322327498317924624L;

	public BackupStorage(final File bsdbFile)
	{	
		chunkDatabase = new HashMap<String, ChunkCollection>();
		remoteFiles = new HashMap<String, FileInformation>();
		remoteFiles.put("example.bin", new FileInformation("ca9aad8ad890adg0gdagad", 1024000, 5));
		remoteFiles.put("256bytes.hex", new FileInformation("abc78fea0fea98ca098ca708ed", 252490, 2));
	}
	
	//-----------------------------------------------------

	public void dumpStorage()
	{
		System.out.println("!!! CHUNKS IN DATABASE !!!");	
		chunkDatabase.forEach((fileId, fileInformation) -> {
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
		remoteFiles.put(paramChunks.getFileId(), new FileInformation(paramChunks));
	}
	
	public synchronized final boolean unregisterRestore(final String fileName)
	{
		if (!remoteFiles.containsKey(fileName))
		{
			return false;
		}
		
		remoteFiles.remove(fileName);
		
		return true;
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
	
	//-----------------------------------------------------
	
	private HashMap<String, ChunkCollection> chunkDatabase;
	
	//-----------------------------------------------------
	

	
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

		for (final String fileId : chunkDatabase.keySet())
		{
			final Pair<Integer, Integer> currentFile = chunkDatabase.get(fileId).findMostReplicated();

			if (currentFile.second() > mostReplicatedChunk.second())
			{
				mostReplicatedChunk = currentFile;
				mostReplicatedFile = fileId;
			}
		}

		return new Pair<String, Integer>(mostReplicatedFile, mostReplicatedChunk.first());
	}
	
	//---------------------

	public synchronized void registerPeer(final String fileId, int chunkId, int peerId)
	{
		if (chunkDatabase.containsKey(fileId))
		{
			chunkDatabase.get(fileId).registerPeer(chunkId, peerId);
		}
	}
	
	public synchronized void removePeer(final String fileId, int chunkId, int peerId)
	{
		if (chunkDatabase.containsKey(fileId))
		{
			chunkDatabase.get(fileId).removePeer(chunkId, peerId);
		} 
	}
	
	
	public synchronized int getReplicationDegree(final String fileId, int chunkId)
	{
		if (chunkDatabase.containsKey(fileId))
		{
			return chunkDatabase.get(fileId).getChunk(chunkId)
					.getReplicationDegree();
		}

		return 0;
	}

	public synchronized int getPeerCount(final String fileId, int chunkId)
	{
		if (chunkDatabase.containsKey(fileId))
		{
			return chunkDatabase.get(fileId).getChunk(chunkId).getCount();
		}

		return 0;
	}
	
	// -----------------------------------------------------
	
	public synchronized ChunkInformation getChunk(final String fileId, int chunkId)
	{
		if (chunkDatabase.containsKey(fileId))
		{
			return (chunkDatabase.get(fileId).getChunk(chunkId));
		}

		return null;
	}
	
	public synchronized boolean putChunk(final Chunk paramChunk)
	{
		final String fileId = paramChunk.getFileId();

		if (!chunkDatabase.containsKey(fileId))
		{
			chunkDatabase.put(fileId, new ChunkCollection());
		}
		
		final boolean newChunk =  chunkDatabase.get(fileId).putChunk(paramChunk);
		
		if (newChunk)
		{
			currentSize += paramChunk.getLength();
		}
		else
		{
			System.out.println("BackupStorage::chunk exists!");
		}
		
		return newChunk;
	}

	public synchronized boolean removeChunk(final String fileId, int chunkId)
	{
		if (!chunkDatabase.containsKey(fileId))
		{
			return false;
		}
		
		currentSize -= chunkDatabase.get(fileId).removeChunk(chunkId);
			
		return true;	
	}
	
	// -----------------------------------------------------
	
	public synchronized boolean removeFile(final String fileId)
	{
		if (!chunkDatabase.containsKey(fileId))
		{
			return false;
		}
	
		currentSize -= chunkDatabase.remove(fileId).getSize();
		
		return true;
	}
}