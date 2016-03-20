package bs;

public abstract class Message
{
	/*
	 * Message types enumeration
	 */
	public static final int Type = 0;
	public static final int Version = 1;
	public static final int SenderId = 2;
	public static final int FileId = 3;
	public static final int ChunkId = 4;
	public static final int ReplicationDegree = 5;

	/*
	 * This attribute represents the ID of the server that has sent the message.
	 * Its value is encoded as a variable length sequence of ASCII digits.
	 */
	private final int m_senderId;

	/*
	 * This attribute represents the file identifier for the backup service. As
	 * stated above, it is supposed to be obtained by using the SHA256
	 * cryptographic hash function. As the name suggests, its length is 256 bit,
	 * i.e. 32 bytes, and should be encoded as a 64 ASCII character sequence.
	 * The encoding is as follows: each byte of the hash value is encoded by the
	 * two ASCII characters corresponding to the hexadecimal representation of
	 * that byte. E.g., a byte with value 0xB2 should be represented by the two
	 * char sequence 'B' '2'. The entire hash is represented in big endian
	 * order, i.e. from the MSB (byte 31) to the LSB (byte 0).
	 */
	private final String m_fileId;

	/*
	 * This attribute represents the protocol version. It is a three ASCII char
	 * sequence with the format <n>'.'<m>, where <n> and <m> are the ASCII codes
	 * of digits. For example, version 1.0, the one specified in this document,
	 * should be encoded as the char sequence '1'- '.' - '0'.
	 */
	private final String m_version;

	/*
	 * This attribute represents the number of fields in the received message.
	 * Its value is encoded as a variable length sequence of ASCII digits
	 */
	private final int m_length;

	/*
	 * This constructor can be used parse data from the received messages
	 * @throws VersionMismatchException
	 */
	protected Message(final String[] paramHeader) throws VersionMismatchException
	{
		m_length = paramHeader.length;
		m_senderId = Integer.parseInt(paramHeader[Message.SenderId]);
		m_fileId = paramHeader[Message.FileId];
		m_version = paramHeader[Message.Version];

		if (!m_version.equals(BackupSystem.getVersion()))
		{
			throw new VersionMismatchException(m_version, BackupSystem.getVersion());
		}
	}

	/*
	 * This constructor can be used to generate a message to be sent
	 */
	protected Message(int messageLength, final String fileId)
	{
		m_length = messageLength;
		m_senderId = BackupSystem.getPeerId();
		m_version = BackupSystem.getVersion();
		m_fileId = fileId;
	}

	public final String getFileId()
	{
		return m_fileId;
	}

	/*
	 * This method returns the protocol version
	 */
	public final String getVersion()
	{
		return m_version;
	}

	/*
	 * This method returns the number of message fields
	 */
	public final int getLength()
	{
		return m_length;
	}

	/*
	 * This attribute represents message type (DELETE, STORED, PUTCHUNK, ...)
	 * Each sub-protocol can specify its own message types. This field
	 * determines the format of the message and what actions its receivers
	 * should perform. Its value is encoded as a variable length sequence of
	 * ASCII characters.
	 */
	public abstract String getType();

	/*
	 * This method returns the ID of the server that has sent the message
	 */
	public final int getSender()
	{
		return m_senderId;
	}

	public String[] generateHeader()
	{
		final String[] m_header = new String[m_length];

		m_header[Message.Type] = getType();
		m_header[Message.SenderId] = Integer.toString(m_senderId);
		m_header[Message.Version] = m_version;
		m_header[Message.FileId] = m_fileId;

		return m_header;
	}

	protected void dump()
	{
		System.out.println("\tType: " + getType());
		System.out.println("\tVersion: " + m_version);
		System.out.println("\tSenderId: " + m_senderId);
		System.out.println("\tFileId: " + m_fileId);
	}

	public byte[] getMessage()
	{
		return (String.join(" ", generateHeader()) + "\r\n\r\n").getBytes();
	}
}

class DELETEMessage extends Message
{
	public DELETEMessage(final String fileId)
	{
		super(4, fileId);
	}

	protected DELETEMessage(final String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "DELETE";
	}
}