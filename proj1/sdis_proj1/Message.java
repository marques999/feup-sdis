package sdis_proj1;

public abstract class Message
{
	/*
	 * This is the id of the server that has sent the message.
	 * This field is useful in many subprotocols.
	 * This is encoded as a variable length sequence of ASCII digits.
	 */
	private int m_senderId;
	
	/*
	 * This is the file identifier for the backup service. As stated above, it
	 * is supposed to be obtained by using the SHA256 cryptographic hash function.
	 * As its name indicates its length is 256 bit, i.e. 32 bytes, and should be
	 * encoded as a 64 ASCII character sequence. The encoding is as follows: each
	 * byte of the hash value is encoded by the two ASCII characters corresponding
	 * to the hexadecimal representation of that byte. E.g., a byte with value 0xB2
	 * should be represented by the two char sequence 'B' '2'. The entire hash is
	 * represented in big endian order, i.e. from the MSB (byte 31) to the LSB (byte 0).
	 */
	private String m_fileId;
	
	/*
	 * This is the version of the protocol. It is a three ASCII char sequence with the
	 * format <n>'.'<m>, where <n> and <m> are the ASCII codes of digits. For example, 
	 * version 1.0, the one specified in this document, should be encoded as the char
	 * sequence '1'- '.' - '0'.
	 */
	private String m_version;

	private int m_size;

	protected Message(final String[] paramHeader) throws VersionMismatchException
	{
		m_size = paramHeader.length;
		m_senderId = Integer.parseInt(paramHeader[MessageHeader.ServerId]);
		m_fileId = paramHeader[MessageHeader.FileId];
		m_version = paramHeader[MessageHeader.Version];

		if (!m_version.equals(Server.getInstance().getVersion()))
		{
			throw new VersionMismatchException(m_version, Server.getInstance().getVersion());
		}
	}

	protected Message(int messageLength, final String fileId)
	{
		m_size = messageLength;
		m_senderId = Server.getInstance().getServerId();
		m_version = Server.getInstance().getVersion();
		m_fileId = fileId;
	}

	public final String getFileId()
	{
		return m_fileId;
	}

	/*
	 * returns the version of the protocol
	 */
	public final String getVersion()
	{
		return m_version;
	}

	/*
	 * returns the message length
	 */
	public final int getLength()
	{
		return m_size;
	}

	public final int getSender()
	{
		return m_senderId;
	}

	public String[] generateHeader()
	{
		final String[] m_header = new String[m_size];

		m_header[MessageHeader.Type] = getType();
		m_header[MessageHeader.ServerId] = Integer.toString(m_senderId);
		m_header[MessageHeader.Version] = m_version;
		m_header[MessageHeader.FileId] = m_fileId;

		return m_header;
	}

	protected void dump()
	{
		System.out.println("+===============================+");
		System.out.println("|            HEADER             |");
		System.out.println("+===============================+");
		System.out.println("Type:\t\t" + getType());
		System.out.println("Version:\t" + m_version);
		System.out.println("Server:\t\t" + m_senderId);
		System.out.println("FileId:\t\t" + m_fileId);
	}

	/*
	 * This is the type of the message.
	 * Each subprotocol specifies its own message types.
	 * This field determines the format of the message and what actions its
	 * receivers should perform. This is encoded as a variable length sequence
	 * of ASCII characters.
	 */
	public abstract String getType();

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

	protected DELETEMessage(String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

	public final String getType()
	{
		return "DELETE";
	}
}