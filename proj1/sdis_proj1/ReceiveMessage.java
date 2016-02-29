package sdis_proj1;

import java.util.Arrays;

public class ReceiveMessage
{
	private static String[] processHeader(String paramHeader)
	{
		return paramHeader.trim().split(" ");
	}

	public static void process(byte[] msg) throws VersionMismatchException
	{
		String[] messageHeader = null;
		String convertedMessage = new String(msg);

		byte[] messageBody = null;

		if (convertedMessage.startsWith("PUTCHUNK") || convertedMessage.startsWith("CHUNK"))
		{
			int payloadSeparatorStart = convertedMessage.indexOf("\r\n\r\n");
			int payloadSeparatorEnd = payloadSeparatorStart + "\r\n\r\n".length();		
			messageHeader = processHeader(convertedMessage.substring(0, payloadSeparatorStart));
			messageBody = Arrays.copyOfRange(msg, payloadSeparatorEnd, msg.length);
		}
		else
		{
			messageHeader = processHeader(convertedMessage);
		}
	
		if (messageHeader[0].equals("PUTCHUNK"))
		{
			new PUTCHUNKMessage(messageHeader, messageBody).dump();
		}
		else if (messageHeader[0].equals("CHUNK"))
		{
			new CHUNKSMessage(messageHeader, messageBody).dump();
		}
		else if (messageHeader[0].equals("GETCHUNK"))
		{
			new GETCHUNKMessage(messageHeader).dump();
		}
		else if (messageHeader[0].equals("DELETE"))
		{
			new DELETEMessage(messageHeader).dump();
		}
		else if (messageHeader[0].equals("STORED"))
		{
			new STOREDMessage(messageHeader).dump();
		}
		else if (messageHeader[0].equals("REMOVED"))
		{
			new REMOVEDMessage(messageHeader).dump();
		}
	}
}