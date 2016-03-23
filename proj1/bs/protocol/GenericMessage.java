package bs.protocol;

import java.util.HashMap;

import bs.BackupSystem;

public class GenericMessage
{
	public static final int Type = 0;
	public static final int Version = 1;
	public static final int SenderId = 2;
	public static final int FileId = 3;
	public static final int ChunkId = 4;
	public static final int ReplicationDegree = 5;
	
	private final static HashMap<String, Integer> typeLength = new HashMap<String, Integer>();

	static {
		typeLength.put("DELETE", 4);
		typeLength.put("PUTCHUNK", 6);
		typeLength.put("STORED", 5);
		typeLength.put("REMOVED", 5);
		typeLength.put("CHUNK", 5);
		typeLength.put("GETCHUNK", 5);
	}
	
	/*
	 * This attribute represents the ID of the server that has sent the message.
	 * Its value is encoded as a variable length sequence of ASCII digits.
	 */
	private final int m_senderId;

	/*
	 * This attribute represents the file identifier for the backup service.
	 * As stated above, it is supposed to be obtained by using the SHA256 cryptographic
	 * hash function. As the name suggests, its length is 256 bit, i.e. 32 bytes, and
	 * should be encoded as a 64 ASCII character sequence. The encoding is as follows:
	 * each byte of the hash value is encoded by the two ASCII characters corresponding
	 * to the hexadecimal representation of that byte. E.g., a byte with value 0xB2
	 * should be represented by the two char sequence 'B' '2'. The entire hash is
	 * represented in big endian order, i.e. from the MSB (byte 31) to the LSB (byte 0).
	 */
	private final String m_fileId;

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
	private int m_chunkId;
	
	/*
	 * This attribute represents the protocol version.
	 * It is a three ASCII char sequence with the format <n>'.'<m>, where <n> and <m>
	 * are the ASCII codes of digits. For example, version 1.0, the one specified in
	 * this document, should be encoded as the char sequence '1'- '.' - '0'.
	 */
	private final String m_version;

	private final String m_type;
	
	/*
	 * This attribute represents the number of fields in the received message.
	 * Its value is encoded as a variable length sequence of ASCII digits
	 */
	private int m_length;

	/*
	 * This field contains the desired replication degree of the chunk.
	 * This is a digit, thus allowing a replication degree of up to 9.
	 * It takes one byte, which is the ASCII code of that digit.
	 */
	private int m_degree;
	

	/*
	 * This constructor can be used parse data from the received messages
	 * @throws VersionMismatchException
	 */
	protected GenericMessage(final String messageType, int messageLength, final String fileId) {
		m_type = messageType;
		m_length = messageLength;
		m_senderId = BackupSystem.getPeerId();
		m_version = BackupSystem.getVersion();
		m_fileId = fileId;
		m_chunkId = -1;
		m_degree = -1;
	}
	
	protected void setChunkId(int chunkId) {
		
		if (m_length > GenericMessage.ChunkId) {
			m_chunkId = chunkId;
		}
	}
	
	protected void setReplicationDegree(int degree) {
		
		if (m_length > GenericMessage.ReplicationDegree) {
			m_degree = degree;
		}
	}
	
	public final String getFileId() {
		return m_fileId;
	}

	/*
	 * This method returns the protocol version
	 */
	public final String getVersion() {
		return m_version;
	}

	
	protected int getChunkId() {
		return m_chunkId;
	}
	
	protected int getDegree() {
		return m_degree;
	}
	
	/*
	 * This method returns the number of message fields
	 */
	public final int getLength() {
		return m_length;
	}
	
	/*
	 * This method returns the ID of the server that has sent the message
	 */
	public final int getSender() {
		return m_senderId;
	}
	
	/*
	 * This constructor can be used to parse data from the received messages
	 * @throws VersionMismatchException
	 */
	protected GenericMessage(final String[] paramHeader) throws UnknownMessageException {
		
		m_type = paramHeader[GenericMessage.Type];
		m_length = paramHeader.length;
		m_senderId = Integer.parseInt(paramHeader[GenericMessage.SenderId]);
		m_fileId = paramHeader[GenericMessage.FileId];
		m_version = paramHeader[GenericMessage.Version];

		if (m_length != typeLength.get(m_type)) {
			throw new UnknownMessageException(m_type); 
		}

		if (m_length > GenericMessage.ChunkId) {
			m_chunkId = Integer.parseInt(paramHeader[GenericMessage.ChunkId]);
		}
		else {
			m_chunkId = -1;
		}
		
		if (m_length > GenericMessage.ReplicationDegree) {
			m_degree = Integer.parseInt(paramHeader[GenericMessage.ReplicationDegree]);
		}
		else {
			m_degree = -1;
		}
	}
}