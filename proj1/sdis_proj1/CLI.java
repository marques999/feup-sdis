public class CLI {

	public static final boolean DEBUG = true;
	
	// machine states
	public static final int INIT = 0;
	public static final int PEER_PARSE = 1;
	public static final int RESTORE_CMD = 2;
	public static final int DELETE_CMD = 3;
	public static final int RECLAIM_CMD = 4;
	public static final int BACKUP_CMD = 5;
	public static final int BACKUP_DEG = 6;
	public static final int FINAL = 7;
	
	public static int currentState = INIT;	
	public static String[] peerData;
	
	public static void main(String[] args) {
		
		// verify if number of arguments passed is valid
		if(args.length > 4 || args.length < 3)
		{
			System.out.println("[ERROR]: Invalid number of arguments!" + args.length);
			System.exit(1);
		}
		
		// state machine ...
		while(currentState != FINAL)
		{	
			switch(currentState)
			{
			case INIT:
				peerData = parseAccessPoint(args[0]);
				break;
			case PEER_PARSE:
				parseCommand(args[1]);
				break;
			// states missing
			default:
				System.out.println("[ERROR]: Point of no return reached!");
				System.exit(1);
			}
		}
	}

	// parse the command
	private static void parseCommand(String arg){
		if(arg.equals("RESTORE") || arg.equals("RESTOREENH")){		
			currentState = RESTORE_CMD;
			System.out.println("[STATE]: ENTER RESTORE_CMD");
		} else if(arg.equals("DELETE") || arg.equals("DELETEENH")){
			currentState = DELETE_CMD;
			System.out.println("[STATE]: ENTER DELETE_CMD");
		} else if(arg.equals("RECLAIM") || arg.equals("RECLAIMENH")){
			currentState = RECLAIM_CMD;
			System.out.println("[STATE]: ENTER RECLAIM_CMD");
		} else if(arg.equals("BACKUP") || arg.equals("BACKUPENH")){
			currentState = BACKUP_CMD;
			System.out.println("[STATE]: ENTER BACKUP_CMD");
		} else {
			System.out.println("[ERROR]: Invalid command!");
			System.exit(1);
		}
		
		if(DEBUG)
		{
			System.out.println("[SUCCESS]: CMD[" + arg + "]");
		}
	}
	
	// parse the backup replication degree
	private static int parseReplicationDegree(String arg) {
		int degree = Integer.parseInt(arg);
		if (degree <= 0) {
			System.out.println("[ERROR]: Invalid replication degree!");
			System.exit(1);
		}

		if (DEBUG) {
			System.out.println("[SUCCESS]: DEGREE[" + degree + "]");
		}

		return degree;
	}

	// parse the peer access point information
	private static String[] parseAccessPoint(String access) {
		String[] accessData = new String[2];
		int commaPos = access.indexOf(':');
		if (commaPos == -1 || commaPos == 0) {
			accessData[0] = "localhost";
			accessData[1] = access;
		} else {
			accessData[0] = access.substring(0, commaPos);
			accessData[1] = access.substring(commaPos + 1);
		}
		if (accessData[0].length() == 0) {
			System.out.println("[ERROR]: Invalid IP address!");
			System.exit(1);
		}
		if (accessData[1].length() == 0) {
			System.out.println("[ERROR]: Invalid port number!");
			System.exit(1);
		}
		if (DEBUG) {
			System.out.println("[SUCCESS]: IP[" + accessData[0] + "] PORT[" + accessData[1] + "]");
			System.out.println("[STATE]: ENTER PEER_PARSE");
		}
		
		currentState = PEER_PARSE;
		return accessData;
	}
}
