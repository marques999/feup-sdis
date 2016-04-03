package bs.protocol;

import bs.Peer;

public abstract class Message
{
	public static final int Type = 0;
	public static final int Port = 0;
	public static final int Version = 1;
	public static final int SenderId = 2;
	public static final int FileId = 3;
	public static final int ChunkId = 4;
	public static final int ReplicationDegree = 5;
	/*
	 * 
	 */
	protected final static String CRLF = "\r\n\r\n";

	/*
	 * This attribute represents message type (DELETE, STORED, PUTCHUNK, ...)
	 * Each sub-protocol can specify its own message types. This field
	 * determines the format of the message and what actions its receivers
	 * should perform. Its value is encoded as a variable length sequence of
	 * ASCII characters.
	 */
	public abstract String getType();

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
	 * This attribute represents the ID of the peer that has sent the message.
	 * Its value is encoded as a variable length sequence of ASCII digits.
	 */
	private final int m_peerId;

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

	protected Message(int messageLength, final String fileId, final String msgVersion)
	{
		m_length = messageLength;
		m_peerId = Peer.getPeerId();
		m_version = msgVersion;
		m_fileId = fileId;
	}

	public final int getPeerId()
	{
		return m_peerId;
	}

	public String[] generateHeader()
	{
		final String[] m_header = new String[m_length];

		m_header[Message.Type] = getType();
		m_header[Message.SenderId] = Integer.toString(m_peerId);
		m_header[Message.Version] = m_version;
		m_header[Message.FileId] = m_fileId;

		return m_header;
	}

	public byte[] getMessage()
	{
		return (String.join(" ", generateHeader()) + Message.CRLF).getBytes();
	}
}