package bs.protocol;

import java.util.Arrays;

import bs.filesystem.Chunk;

public abstract class PayloadMessage extends Message
{
	/*
	 * When present, the body contains the data of a file chunk. The length of
	 * the body is variable. As stated above, if it is smaller than the maximum
	 * chunk size, 64KByte, it is the last chunk in a file. The protocol does
	 * not interpret the contents of the Body. For the protocol its value is
	 * just a byte sequence.
	 */
	private byte[] m_body;

	/*
	 * This field together with the FileId specifies a chunk in the file. The
	 * chunk numbers are integers and should be assigned sequentially starting
	 * at 0. It is encoded as a sequence of ASCII characters corresponding to
	 * the decimal representation of that number, with the most significant
	 * digit first. The length of this field is variable, but should not be
	 * larger than 6 chars. Therefore, each file can have at most one million
	 * chunks. Given that each chunk is 64 KByte, this limits the size of the
	 * files to backup to 64 GByte.
	 */
	private int m_chunkId;

	/*
	 * This field contains the desired replication degree of the chunk. This is
	 * a digit, thus allowing a replication degree of up to 9. It takes one
	 * byte, which is the ASCII code of that digit.
	 */
	private int m_degree;

	protected PayloadMessage(final Chunk paramChunk, int replicationDegree, final String msgVersion)
	{
		super(replicationDegree > 0 ? 6 : 5, paramChunk.getFileId(), msgVersion);
		m_body = paramChunk.getData();
		m_chunkId = paramChunk.getChunkId();
		m_degree = replicationDegree;
	}

	private final byte[] getHeader()
	{
		final String[] m_header = generateHeader();

		m_header[Message.ChunkId] = Integer.toString(m_chunkId);

		if (m_degree > 0)
		{
			m_header[Message.ReplicationDegree] = Integer.toString(m_degree);
		}

		return (String.join(" ", m_header) + "\r\n\r\n").getBytes();
	}

	@Override
	public final byte[] getMessage()
	{
		final byte[] messageHeader = getHeader();
		final byte[] result = Arrays.copyOf(messageHeader, messageHeader.length + m_body.length);

		System.arraycopy(m_body, 0, result, messageHeader.length, m_body.length);

		return result;
	}
}