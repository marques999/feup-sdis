package bs;

import java.io.IOException;

import bs.logging.Logger;

public class PeerEnhanced
{
	public static void main(final String[] args) throws IOException
	{
		if (PeerGlobals.checkArguments(args.length))
		{
			Peer.initializePeer(args, false);
			Peer.setEnhancements(true);
		}
		else
		{
			Logger.logError("invalid number of arguments given, please enter the following:");
			System.out.println("    (1) PeerEnhanced <PeerId> <Host>");
			System.out.println("    (2) PeerEnhanced <PeerId> <Host> <McPort> <MdbPort> <MdrPort>");
			System.out.println("    (3) PeerEnhanced <PeerId> <McHost> <McPort> <MdbHost> <MdbPort> <MdrHost> <MdrPort>");
			System.exit(1);
		}
	}
}