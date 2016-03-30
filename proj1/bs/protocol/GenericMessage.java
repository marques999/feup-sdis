package bs.protocol;

import bs.filesystem.Chunk;

public class GenericMessage
{
	public GenericMessage(final String[] paramHeader)
	{
		this(paramHeader, null, null);
	}
	
	public final Chunk generateChunk()
	{
		return new Chunk(m_body, getFileId(), m_chunkId, m_degree);
	}
	
	public GenericMessage(final String[] paramHeader, final byte[] paramBody)
	{
		this(paramHeader, null, paramBody);
	}
	
	public GenericMessage(final String[] paramHeader, final String[] paramExtra)
	{
		this(paramHeader, paramExtra, null);
	}
	
	private GenericMessage(final String[] paramHeader, final String[] paramExtra, final byte[] paramBody)
	{
		m_type = paramHeader[Message.Type];
		m_length = paramHeader.length;
		m_peerId = Integer.parseInt(paramHeader[Message.SenderId]);
		m_fileId = paramHeader[Message.FileId];
		m_version = paramHeader[Message.Version];

		if (m_length > Message.ChunkId)
		{
			m_chunkId = Integer.parseInt(paramHeader[Message.ChunkId]);
		}
		else
		{
			m_chunkId = -1;
		}

		if (m_length > Message.ReplicationDegree)
		{
			m_degree = Integer.parseInt(paramHeader[Message.ReplicationDegree]);
		}
		else
		{
			m_degree = -1;
		}

		m_body = paramBody;

		if (m_version.equals("2.0") && paramExtra != null)
		{
			if (paramExtra.length > Message.Port)
			{
				m_port = Integer.parseInt(paramExtra[Message.Port]);
			}
			else
			{
				m_port = -1;
			}
		}
		else
		{
			m_port = -1;
		}
	}
	
	public final boolean hasEnhancements()
	{
		return m_version.equals("2.0");
	}

	/*
	 * This attribute represents the ID of the server that has sent the message.
	 * Its value is encoded as a variable length sequence of ASCII digits.
	 */
	private final int m_peerId;
	
	public final int getPeerId()
	{
		return m_peerId;
	}
	
	private final byte[] m_body;
	
	public final byte[] getBody()
	{
		return m_body;
	}

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
	
	public final String getFileId()
	{
		return m_fileId;
	}

	/*
	 * This attribute together with the FileId specifies a chunk in the file.
	 * The chunk numbers are integers and should be assigned sequentially
	 * starting at 0. It is encoded as a sequence of ASCII characters
	 * corresponding to the decimal representation of that number, with the most
	 * significant digit first. The length of this field is variable, but should
	 * not be larger than 6 chars. Therefore, each file can have at most one
	 * million chunks. Given that each chunk is 64 KByte, this limits the size
	 * of the files to backup to 64 GByte.
	 */
	private final int m_chunkId;
	
	public final int getChunkId()
	{
		return m_chunkId;
	}

	/*
	 * This attribute represents the protocol version. It is a three ASCII char
	 * sequence with the format <n>'.'<m>, where <n> and <m> are the ASCII codes
	 * of digits. For example, version 1.0, the one specified in this document,
	 * should be encoded as the char sequence '1'- '.' - '0'.
	 */
	private final String m_version;
	
	public final String getVersion()
	{
		return m_version;
	}
	
	private final String m_type;

	public final String getType()
	{
		return m_type;
	}
	
	/*
	 * This attribute represents the number of fields in the received message.
	 * Its value is encoded as a variable length sequence of ASCII digits
	 */
	private final int m_length;
	
	public final int getLength()
	{
		return m_length;
	}

	/*
	 * This field contains the desired replication degree of the chunk. This is
	 * a digit, thus allowing a replication degree of up to 9. It takes one
	 * byte, which is the ASCII code of that digit.
	 */
	private final int m_degree;
	
	public final int getDegree()
	{
		return m_degree;
	}
	
	private final int m_port;
	
	public final int getPort()
	{
		return m_port;
	}
}