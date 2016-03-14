public class CLI {

	public static final boolean DEBUG = true;

	// machine states
	public static final int INIT = 0;
	public static final int PEER_PARSE = 1;
	public static final int READ_CMD = 2;
	public static final int BACKUP_CMD = 3;
	public static final int BACKUP_DEG = 4;
	public static final int FINAL = 5;

	public static int currentState = INIT;
	public static int degree;
	public static String[] peerData;

	public static void main(String[] args) {

		// verify if number of arguments passed is valid
		if(args.length > 4 || args.length < 3)
		{
			System.out.println("[ERROR]: Invalid number of arguments!");
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
			case READ_CMD:
				parseOperand(args[2]);
				break;
			case BACKUP_CMD:
				parseOperand(args[2]);
				break;
			case BACKUP_DEG:
				if(args.length != 4)
				{
					System.out.println("[ERROR]: Invalid number of arguments!");
					System.exit(1);
				}
				degree = parseReplicationDegree(args[3]);
				break;
			default:
				System.out.println("[ERROR]: Point of no return reached!");
				System.exit(1);
			}
		}

		if(DEBUG)
		{
			System.out.println("[PARSER]: Command parsed with success!");
		}
	}

	// parse the first operand
	private static void parseOperand(String arg){
		// TODO: validate argument
		if(currentState == BACKUP_CMD)
		{
			 currentState = BACKUP_DEG;
			 if(DEBUG)
			 {
				 System.out.println("[STATE]: ENTER BACKUP_DEG");
			 }
			 return;
		} else {
			currentState = FINAL;
			if(DEBUG)
			{
				System.out.println("[STATE]: ENTER FINAL");
			}
		}
	}

	// parse the command
	private static void parseCommand(String arg){
		if(arg.equals("RESTORE") || arg.equals("RESTOREENH")){
			currentState = READ_CMD;
			System.out.println("[STATE]: ENTER RESTORE_CMD");
		} else if(arg.equals("DELETE") || arg.equals("DELETEENH")){
			currentState = READ_CMD;
			System.out.println("[STATE]: ENTER DELETE_CMD");
		} else if(arg.equals("RECLAIM") || arg.equals("RECLAIMENH")){
			currentState = READ_CMD;
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

		currentState = FINAL;
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
