
package sdis_proj1;

public abstract class SimpleMessage extends Message {

	/*
	 * This attribute together with the FileId specifies a chunk in the file.
	 * The chunk numbers are integers and should be assigned sequentially
	 * starting at 0. It is encoded as a sequence of ASCII characters
	 * corresponding to the decimal representation of that number, with the
	 * most significant digit first. The length of this field is variable,
	 * but should not be larger than 6 chars. Therefore, each file can have
	 * at most one million chunks. Given that each chunk is 64 KByte, this
	 * limits the size of the files to backup to 64 GByte.
	 */
	private final int m_chunkId;

	protected SimpleMessage(final String fileId, int chunkId) {
		super(5, fileId);
		m_chunkId = chunkId;
	}

	protected SimpleMessage(final String[] paramHeader) throws VersionMismatchException {
		super(paramHeader);
		m_chunkId = Integer.parseInt(paramHeader[Message.ChunkId]);
	}

	@Override
	protected void dump() {
		super.dump();	
		System.out.println("\tChunkNo: " + m_chunkId);
	}

	@Override
	public final byte[] getMessage() {
		final String[] m_header = generateHeader();
		m_header[Message.ChunkId] = Integer.toString(m_chunkId);
		return (String.join(" ", m_header) + "\r\n\r\n").getBytes();
	}
}

class STOREDMessage extends SimpleMessage {
	
	public STOREDMessage(final String fileId, int chunkId) {
		super(fileId, chunkId);
	}

	protected STOREDMessage(final String[] paramHeader) throws VersionMismatchException {
		super(paramHeader);
	}

	@Override
	public final String getType() {
		return "STORED";
	}
}

class GETCHUNKMessage extends SimpleMessage {
	
	public GETCHUNKMessage(final String fileId, int chunkId) {
		super(fileId, chunkId);
	}

	protected GETCHUNKMessage(final String[] paramHeader) throws VersionMismatchException {
		super(paramHeader);
	}

	public final Message getResponse(final FileChunk paramChunk) {
		
		if (Protocol.DEBUG) {
			System.out.println("[DEBUG] getResponse()::generating CHUNK response");
		}
		
		return new CHUNKMessage(paramChunk);
	}
	
	@Override
	public final String getType() {
		return "GETCHUNK";
	}
}

class REMOVEDMessage extends SimpleMessage {

	public REMOVEDMessage(final String fileId, int chunkId) {
		super(fileId, chunkId);
	}

	protected REMOVEDMessage(final String[] paramHeader) throws VersionMismatchException {
		super(paramHeader);
	}

	@Override
	public final String getType() {
		return "REMOVED";
	}
}