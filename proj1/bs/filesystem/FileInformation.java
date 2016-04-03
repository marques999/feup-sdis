package bs.filesystem;

import java.io.Serializable;

public class FileInformation implements Serializable
{
	private static final long serialVersionUID = 5637247759122921141L;

	public FileInformation(final String paramId, long paramSize, int paramChunks)
	{
		fileId = paramId;
		fileSize = paramSize;
		numberChunks = paramChunks;
	}

	public FileInformation(final ChunkBackup paramChunks)
	{
		this(paramChunks.getFileId(), paramChunks.getFileSize(), paramChunks.getCount());
	}

	public final String toString(final String fileName)
	{
		return String.format("| %-30s | %10d bytes | %7d |\n", fileName, fileSize, numberChunks);
	}

	//-----------------------------------------------------

	private final String fileId;

	public final String getFileId()
	{
		return fileId;
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