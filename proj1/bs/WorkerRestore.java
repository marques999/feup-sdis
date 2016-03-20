package bs;

public class WorkerRestore extends Thread
{
	private final ProtocolCommand MC;
	private final ProtocolRestore MDR;
	private final String thisThread = "RestoreWorkerThread";
	
	public WorkerRestore(final ProtocolCommand mc, final ProtocolRestore mdr)
	{
		MC = mc;
		MDR = mdr;
	}
	
	@Override
	public void run()
	{
		for(;;)
		{
			final Message receivedMessage = MDR.receive();
			
			if (receivedMessage != null)
			{
				Logger.logCommand(thisThread, receivedMessage.getType());
				Logger.dumpPayload(thisThread, receivedMessage);
				MC.sendREMOVEDResponse((SimpleMessage) receivedMessage);
			}
		}
	}
}
