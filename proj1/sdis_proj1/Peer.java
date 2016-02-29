package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Peer
{
	private static void split(final File f) throws IOException
	{
		final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(f));
		final int numberChunks = (int) Math.ceil(f.length() / 64000.0);
		
		for (int i = 0; i < numberChunks; i++)
		{
			FileChunk fc = new FileChunk(inStream, "example.bin", i);
			System.out.println("chunk number " + i + ", size = " +fc.getLength());
		}
	}

	public static void main(String[] args) throws IOException
	{
		final File f = new File("example.bin");
		split(f);
	//	String mmm = new STOREDMessage("1.0", 4).getMessage();
		
	/*	byte[] mmm = part.getContents();
		for (byte b: mmm)
		{
			System.out.print(String.format("0x%01X", b) + ",");
		}*/
		try
		{
			System.out.println(SHA256.generate("ababab"));
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
