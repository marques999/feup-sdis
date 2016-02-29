package sdis_proj1;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Peer
{
	public static void main(String[] args) throws IOException
	{
		final FileObject f_aligned = new FileObject("example.bin");
		
		FileChunk[] chunks = null;
		
		try
		{
			chunks = f_aligned.split();
			byte[] command = new CHUNKSMessage(chunks[7]).getMessage();
			ReceiveMessage.process(command);
			byte[] secondCommand = new DELETEMessage("example.bin").getMessage();
			ReceiveMessage.process(secondCommand);
		}
		catch (NoSuchAlgorithmException | VersionMismatchException e1)
		{
			e1.printStackTrace();
		}
	}
}