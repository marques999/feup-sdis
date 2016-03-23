package bs.actions;

import bs.filesystem.Chunk;
import bs.server.ProtocolCommand;

public class BackupHelper extends Thread
{
	private static final long waitingTime = 1000;
	private static final int maximumAttempts = 5;

	private final Chunk m_chunk;
	private final ProtocolCommand m_commandChannel;

	public BackupHelper(final Chunk paramChunk, final ProtocolCommand mc)
	{
		m_chunk = paramChunk;
		m_commandChannel = mc;
	}
	
	@Override
	public void run()
	{
		m_commandChannel.registerConfirmations(m_chunk);

		int currentAttempt = 0;
		boolean done = false;
		
		while (!done)
		{
			m_commandChannel.clearRegistration(m_chunk);

			Peer.getCommandForwarder().sendPUTCHUNK(m_chunk);

			try
			{
				System.out.println("Waiting for STOREDs for " + waitingTime + "ms");
				Thread.sleep(waitingTime);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			int confirmedRepDeg = m_commandChannel.getConfirmations(m_chunk);

			System.out.println(confirmedRepDeg + " peers have backed up chunk no. "
					+ m_chunk.getChunkId() + ". (desired: "
					+ m_chunk.getReplicationDegree() + " )");

			if (confirmedRepDeg < m_chunk.getReplicationDegree())
			{
				currentAttempt++;

				if (currentAttempt > maximumAttempts)
				{
					System.out.println("[INFORMATION] reached maximum number of attempts to backup chunk with desired replication degree.");
					done = true;
				}
				else
				{
					System.out.println("[INFORMATION] desired replication degree was not reached. Trying again...");
					//waitingTime *= 2;
				}
			}
			else
			{
				System.out.println("[INFORMATION] reached desired replication degree!");
				done = true;
			}
		}

		m_commandChannel.unregisterConfirmations(m_chunk);
	}
}