+==============================================================================+
|                           PEER INSTRUCTIONS & USAGE                          |
+==============================================================================+

	REGULAR PEER WITH BASE PROTOCOL (version 1.0)
	----------------------------------------------------------------------------
	(1) java bs.Peer <PeerId> <Host>
	(2) java bs.Peer <PeerId> <Host> <McPort> <MdbPort> <MdrPort>
	(3) javs bs.Peer <PeerId> <McHost> <McPort> <MdbHost> <MdbPort> <MdrHost> <MdrPort>
	
	REGULAR PEER WITH ENHANCED PROTOCOL (version 2.0)
	----------------------------------------------------------------------------
	(1) java bs.PeerEnhanced <PeerId> <Host>
	(2) java bs.PeerEnhanced <PeerId> <Host> <McPort> <MdbPort> <MdrPort>
	(3) java bs.PeerEnhanced <PeerId> <McHost> <McPort> <MdbHost> <MdbPort> <MdrHost> <MdrPort>
	
	INITIATOR PEER (protocol version depends on the test interface)
	----------------------------------------------------------------------------
	(1) java bs.InitiatorPeer <PeerId> <Host>
	(2) java bs.InitiatorPeer <PeerId> <Host> <McPort> <MdbPort> <MdrPort>
	(3) java bs.InitiatorPeer <PeerId> <McHost> <McPort> <MdbHost> <MdbPort> <MdrHost> <MdrPort>
		
	ARGUMENTS
	----------------------------------------------------------------------------
	<PeerId>	: unique peer identifier (remote object name for the initiator peer)
	<Host>		: multicast group address for control, backup and restore channels
	<McHost>	: multicast group address for the control channel
	<McPort>	: multicast control channel port
	<MdbHost>	: multicast group address for the backup channel
	<MdbPort>	: multicast backup channel port
	<MdrHost>	: multicast group address for the restore channel
	<MdrPort>	: multicast restore channel port
	
	DEFAULTS
	----------------------------------------------------------------------------
	<McPort>	: 9050
	<MdbPort>	: 9051
	<MdrPort>	: 9052

+==============================================================================+
|                      TEST INTERFACE INSTRUCTIONS & USAGE                     |
+==============================================================================+

	(1) java bs.test.TestApp <PeerId> BACKUP <FileName> <ReplicationDegree>
	(2) java bs.test.TestApp <PeerId> RESTORE[ENH] <FileName>
	(3) java bs.test.TestApp <PeerId> DELETE[ENH] <FileName>
	(4) java bs.test.TestApp <PeerId> RECLAIM[ENH] <Amount(Bytes)>
	
	[ENH] denotes an enhanced command
		-> if this suffix is appended to a command, the initiator peer will
		   try to use all the enhancements available for that command, given
		   that the other peers are also supporting them (bs.PeerEnhanced)
	
+==============================================================================+
|                         OTHER REMARKS & OBSERVATIONS                         |
+==============================================================================+
	
	-> the backup enhancement does not require any special treatment by the 
	   initiator peer, and therefore it has no enhanced command associated;
	-> peers who desire to use the enhanced backup sub-protocol must invoke
	   the enhanced peer application (bs.PeerEnhanced) in order to activate
	   these protocol enhancements;
	-> delete and restore enhancements require exchanging additional messages
	   between the initiator peer and the remaining peers; since these messages
	   have a different protocol version (2.0), at least one peer must have
	   protocol enhancements activated in order to receive these enhanced
	   messages, otherwise they will be ignored;