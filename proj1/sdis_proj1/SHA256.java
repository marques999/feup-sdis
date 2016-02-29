package sdis_proj1;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256
{
	public static String generate(String fileName) throws NoSuchAlgorithmException
	{
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		final StringBuffer sb = new StringBuffer();
		byte byteData[] = md.digest();
		
		md.update(fileName.getBytes());

		for (int i = 0; i < byteData.length; i++)
		{
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}
}