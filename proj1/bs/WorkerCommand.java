package bs;

public class WorkerCommand extends Thread
{
	private final ProtocolCommand MC;
	private final String myName = "CommandWorkerThread";

	public WorkerCommand(final ProtocolCommand mc)
	{
		MC = mc;
	}
	
	@Override
	public void run()
	{
		for(;;)
		{
			final Message receivedMessage = MC.receive();

			if (receivedMessage != null)
			{
				Logger.logCommand(myName, receivedMessage.getType());
				Logger.dumpHeader(myName, receivedMessage);
			}
		}
	}
}