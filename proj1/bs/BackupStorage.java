package bs;

import java.util.HashMap;

public class BackupStorage
{
	private static HashMap<String, ChunkContainer> myDatabase;
	
	static
	{
		myDatabase = new HashMap<String, ChunkContainer>();
	}
	
	public synchronized static boolean placeChunk(final FileChunk chunk)
	{
		final String fileId = chunk.getFileId();

		if (!myDatabase.containsKey(fileId))
		{
			myDatabase.put(fileId, new ChunkContainer(fileId));
		}
		
		final ChunkContainer myContainer = myDatabase.get(fileId);
		
		try
		{
			myContainer.putChunk(chunk);
		}
		catch (BadChunkException e)
		{
			return false;
		}
		
		myContainer.dump();
		
		return true;
	}
	
	public synchronized static void decreaseCount(final String fileId, int chunkId)
	{
		if (myDatabase.containsKey(fileId))
		{
			myDatabase.get(fileId).decreaseCount(chunkId);
		} 
	}
	
	public synchronized static void increaseCount(final String fileId, int chunkId)
	{
		if (myDatabase.containsKey(fileId))
		{
			myDatabase.get(fileId).increaseCount(chunkId);
		}
	}
	
	public synchronized static boolean deleteFile(final String fileId)
	{
		if (!myDatabase.containsKey(fileId))
		{
			return false;
		}
	
		myDatabase.get(fileId).removeAll();
		myDatabase.remove(fileId);
		
		return true;
	}
}