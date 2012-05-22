/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.hw.edu.iit;

/**
 *
 * @author Sangeetha Ramadurai
 */
import java.io.*;
import java.net.*;
import java.util.*;
import static com.hw.edu.iit.MySimulateWebConstants.*;


public class MySimWebBrowser extends Thread {
	private static int portNumber = 0;	//4022;
	private final static String USAGE_MESSAGE = "MySimWebBrowser -M <Machine Name to connect to> -P <port number to connect to - any number between 1024 to 65536>";
	private final static String INVALID_PORT_NUMBER = "Invalid port number. Please enter a number between 1024 to 65536";
	private static MySimWebBrowser my_browser;
	private int requestForByteStreamNum = -1;
	private int currByteStreamNumFromSrvr = -1;
	private int resendByteStreamNum = -1;
	private int continueFromByteStreamNum = -1;
	String OUTPUT_DIR_LOCATION = null;
	String REQUEST_OBJECT = null;
	int object_number = 0;

	private int mss_val = 0;	//1000;
	private String error_recovery_mode = null; //selective repeat
	private static String browser_type = null;
	private String numberOfObjects = null;
	private String error_segment_number = null;
	private boolean readyToReadNextObject = false;

	private static int my_server_port = 0;		//5025;
	private static String my_host_name = null;		//"fwrtnyw130774";

	private boolean request_to_close_received = false;
	private boolean request_to_close_sent = false;

	private HashMap serverQuery;

	ObjectOutputStream objectToServer;
	ObjectInputStream objectFromServer;

	private int clnt_segment = -1;
	private boolean error_occurred = false;
	private boolean checksum_error = false;
	private boolean exponential_window = false;

	private ArrayList zTCP_data_fromserver;
	private ArrayList zTCP_data_toserver;
	private Socket clientSocket;

	BufferedReader in_cgfile = null; //config file
	PrintStream outd = null;
	PrintStream outz = null;
	File dFile = null;
	File zFile = null;

	public MySimWebBrowser () {
		init ();
		try {
			clientSocket = new Socket (my_host_name, my_server_port);
			run();
		}
		catch (IOException e) {
			manage_connection ();
			System.out.println("Connect to server failed. Exception occurred");
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run()
	{
		do {
			try {
				zTCP_data_toserver = prepare_zTCP_toserver ();

				//Send Data to Server
				objectToServer = new ObjectOutputStream ( clientSocket.getOutputStream() );
				objectToServer.writeObject(zTCP_data_toserver);


				//Get data from Server
				objectFromServer = new ObjectInputStream ( clientSocket.getInputStream() );
				zTCP_data_fromserver = new ArrayList<Object> ();
				zTCP_data_fromserver = (ArrayList) objectFromServer.readObject();
				process_zTCP_fromserver ();
			}
			catch (Exception e) {
				manage_connection ();
				System.out.println ("Exception occurred");
				e.printStackTrace();
				System.exit(1);
			}
		}
		while ( !request_to_close_received );

		if (request_to_close_received) {
			send_FINACK_toserver ();
			send_FIN_Segment ();
		}
//		manage_connection ();
	}


	private void init () {
		//TODO: Client GUI i/p to decide 1.0 vs 1.1 client
		String dataFileName = null;
		String zTCPFileName = null;
		boolean invalidConfig = false;
		String line = null;
		String fName = CONFIG_FILE_LOCATION.concat(CONFIG_FILE_NAME);
		exponential_window = false;

		File cgFile = new File ( fName );
		if ( !( cgFile.exists() && cgFile.isFile() ) ) {
			System.out.println ("Missing configuration information. Looking for file: " + fName);
		  	proceed_to_exit ();
		}

		//Open the config file
		try {
			in_cgfile = new BufferedReader ( new InputStreamReader
		   											( new FileInputStream ( cgFile ) ) );
		   	while ( ( line = in_cgfile.readLine() ) != null ) {
		   		int ln = line.length();
		   		if (ln > 0) {
		   			String[] locStr = line.split ( "=", 2 );
		   			if ( locStr.length == 2 ) {

		   				if ( locStr[0].compareTo("SERVER_PORT") == 0 ) {
		   					if ( ! isNullOrEmpty(locStr[1]) && validatePortNumber(locStr[1]) )
		   						my_server_port = Integer.parseInt(locStr[1]);
		   				}
		   				else if ( locStr[0].compareTo("HOST_NAME") == 0 ) {
		   					if ( ! isNullOrEmpty(locStr[1]) )
		   						my_host_name = locStr[1];
		   				}
		   				else if ( locStr[0].compareTo(CLIENTLOCATION) == 0 ) {
		   					if ( !isNullOrEmpty(locStr[1]) && validateLocation(locStr[1]) )
		   						OUTPUT_DIR_LOCATION = locStr[1];
		   					else
		   						invalidConfig = true;
		   				}
		   				else if ( locStr[0].compareTo("WEB_BROWSER_PORT") == 0 ) {
		   					if ( ! isNullOrEmpty(locStr[1]) && validatePortNumber(locStr[1]) )
		   						portNumber = Integer.parseInt(locStr[1]);
		   				}
		   				/*else if ( locStr[0].compareTo(BROWSERTYPE) == 0 ) {
		   					browser_type = locStr[1];
		   					if ( !( browser_type.compareTo("1.0") == 0 ) &&
		   						 !( browser_type.compareTo("1.1") == 0 ) ) {
		   						 invalidConfig = true;
		   						 System.out.println ("Browser Type is either 1.0 or 1.1");
		   					}
		   				}*/
		   				else if ( locStr[0].compareTo(MSSVALUE) == 0 ) {
		   					mss_val = Integer.parseInt(locStr[1]);
		   					if (mss_val <= 0) {
		   						invalidConfig = true;
		   						System.out.println ("Please enter a MSS value > 0");
		   					}
		   				}
		   				else if ( locStr[0].compareTo(NUMOBJREQUEST) == 0 ) {
		   					numberOfObjects = locStr[1];
		   					int val = Integer.parseInt(numberOfObjects);
		   					if ( val <= 0 || val > 3 ) {
		   						invalidConfig = true;
		   						System.out.println ("Please enter a number between 1 to 3 both inclusive");
		   					}
		   				}
		   				else if ( locStr[0].compareTo(ERRSEGMENTNUMBER) == 0 ) {
		   					error_segment_number = locStr[1];
		   					int val = Integer.parseInt(error_segment_number);
		   					if ( val  < 0 ) {
		   						invalidConfig = true;
		   						System.out.println ("Please enter number a number >= 0");
		   					}
		   				}
		   				else if ( locStr[0].compareTo("EXPONENTIAL_WINDOW_SIZE") == 0 ) {
			   				if ( ! isNullOrEmpty(locStr[1]) )
			   					exponential_window = ( locStr[1].compareTo("true") == 0 ) ? true : false;
			   			}

		   			}
		   		}
		   	} //while

		   	if ( my_server_port == 0 || my_host_name == null
		   			|| numberOfObjects == null || mss_val <= 0
		   			|| OUTPUT_DIR_LOCATION == null || portNumber == 0 )
		   		invalidConfig = true;

		   	in_cgfile.close ();
		}
		catch (IOException io) {
			System.out.println ("Config file problem");
			proceed_to_exit ();
		}

		if ( invalidConfig ) {
			System.out.println ("Missing configuration information");
		  	proceed_to_exit ();
		}


		if ( browser_type.compareTo("1.0") == 0 ) {
			dataFileName = CLIENT_A_DATAFILE;
			zTCPFileName = CLIENT_A_ZTCP_FILE;
		}
		else {
			dataFileName = CLIENT_B_DATAFILE;
			zTCPFileName = CLIENT_B_ZTCP_FILE;
		}

		String dFileName = OUTPUT_DIR_LOCATION.concat(dataFileName);
		dFile = new File ( dFileName );
		String zFileName = OUTPUT_DIR_LOCATION.concat(zTCPFileName);
		zFile = new File ( zFileName  );

		try {
			outd = new PrintStream ( new FileOutputStream (dFile) );
			outz = new PrintStream ( new FileOutputStream (zFile) );
		}
		catch (FileNotFoundException e) {
			e.getMessage();
			proceed_to_exit ();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			proceed_to_exit ();
		}

		if ( dFile != null && zFile != null ) {
			if ( ! dFile.isFile() || ! dFile.exists() ||
				! zFile.isFile() || ! zFile.exists() ) {
				System.out.println ( "Output file init error" );
				proceed_to_exit ();
			}
		}
		System.out.println ("Data file is : " + dFileName);
		System.out.println ("zTCP Segment file is : " + zFileName);

		//Open the file(s) to write to
		prepareToReceivePayload (cgFile);

		outz.println ();
		outz.println(OBJECT_BEGIN_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL);
		outz.println ();

		outd.println ();
		outd.println(OBJECT_BEGIN_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL);
		outd.println ();

	}

	void prepareToReceivePayload (File configFile) {
		//Open file(s) to write to
		String objFileName = null;
		String line = null;
		boolean invalid = false;

		object_number += 1;
		if (object_number == 1)
			objFileName = REQUEST_FOR_OBJ1;
		else if (object_number == 2)
			objFileName = REQUEST_FOR_OBJ2;
		else if (object_number == 3)
			objFileName = REQUEST_FOR_OBJ3;
		else {
			System.out.println ("Invalid object request");
			invalid = true;
		}

		if (!invalid) {
			try {
				in_cgfile = new BufferedReader ( new InputStreamReader
							( new FileInputStream ( configFile ) ) );
				while ( ( line = in_cgfile.readLine() ) != null ) {
					int ln = line.length();
					if (ln > 0) {
						String[] locStr = line.split ( "=", 2 );
						if ( locStr.length == 2 ) {
							if ( locStr[0].compareTo(objFileName) == 0 ) {
								REQUEST_OBJECT = locStr[1];
								if ( REQUEST_OBJECT == null )
									invalid = true;
							}
						}
					}
				}
				in_cgfile.close();
			}
			catch (IOException io) {
				System.out.println ("Config file problem");
				proceed_to_exit ();
			}
		}
		if ( invalid ) {
			System.out.println ("Missing or Invalid configuration information");
		  	proceed_to_exit ();
		}

	}


	private ArrayList prepare_zTCP_toserver () {
		ArrayList dataToServer = new ArrayList<Object> ();
		My_zTCP_Data cData = new My_zTCP_Data ();
		boolean isSynSegment = false;
		String payload = new String(EMPTY_STRING);
		int len = payload.length();
		//= new byte[len];
		byte[] payload_data = payload.getBytes();

		//If ready to read next object for browser type 1.1
		if ( readyToReadNextObject )
		    initToReadNextObject ();

		if (! error_occurred)
			clnt_segment += 1;

		if (clnt_segment == 0) {
			isSynSegment = true;
			cData.setSynBit( 1 );
			cData.setACKNumber( 0 );
		}
		else
			cData.setSynBit( 0 );

		cData.setSourcePort( portNumber );
		cData.setDestinationPort( 80 );
		cData.setSequenceNumber( 0 );
		cData.setACKBit( 1 );
		cData.setFinBit( 0 );

		if (! isSynSegment) {
			int size = 0;
			if (requestForByteStreamNum == 0 && ( clnt_segment == 1 ) ) {

				cData.setACKNumber( 0 );
				payload = new String ( REQUEST_OBJECT );		//("cs.iit.edu.htm");
				len = payload.length();
				payload_data = payload.getBytes();

				String mystr = new String(payload_data);
				System.out.println ("Requesting: " + mystr);
			}
			else {
				if (!checksum_error &&  resendByteStreamNum == -1 ) {
					if (exponential_window && continueFromByteStreamNum != -1) {
						size = ( continueFromByteStreamNum + mss_val ) ;
						cData.setACKNumber( size );
						requestForByteStreamNum = size;
						continueFromByteStreamNum = -1;
					}
					else {
						size = ( currByteStreamNumFromSrvr + mss_val ) ;
						cData.setACKNumber( size );
						requestForByteStreamNum = size;
					}
				}
				else {
					if ( resendByteStreamNum != -1 ) {
						cData.setACKNumber(resendByteStreamNum);
						resendByteStreamNum = -1;
					}
					else
						cData.setACKNumber(requestForByteStreamNum);
				}

				//System.out.println ("Tracing: Client requesting SeqNum " + requestForByteStreamNum + " from server");
			}
		}

		//Add payload to zTCP segment
		cData.setPayload( payload_data );

		if (request_to_close_received) {
			cData.setFinBit( 1 );

			outd.println ();
			outd.println(OBJECT_END_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL);
			outd.println ();

			outz.println ();
			outz.println(OBJECT_END_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL);
			outz.println ();
		}

		cData.setCheckSum("0000");
		cData.setMSS( mss_val );	//MSS value
		dataToServer.add( cData );


		if ( serverQuery != null &&
			 ( ! serverQuery.isEmpty() ) ) {
			dataToServer.add( serverQuery );
		}

		return dataToServer;
	}

	private void process_zTCP_fromserver () {

		writeDataFromServer ();

	}

	private void writeDataFromServer () {
		My_zTCP_Data zData = null;
		ArrayList fromServer = new ArrayList<Object> ();
		fromServer = zTCP_data_fromserver;
		String objNum = null;
		String objStatus = null;
		boolean finBitSet = false;

		if ( serverQuery != null && !( serverQuery.isEmpty() ) )
			serverQuery.clear();
		int sz = fromServer.size();

		serverQuery = new HashMap <String, String> ();
		outz.println();
		outz.println(BEGIN_RECEIVE_WINDOW_COMMENT);
		outz.println();
		for ( Object hObj : fromServer ) {

			//prepare response for server query if server has issued a query
			if ( hObj instanceof HashMap ) {
				serverQuery = (HashMap) hObj;
				if ( serverQuery != null ) {
					if ( serverQuery.containsKey("ErrorRecovery Mode") )
						serverQuery.put("ErrorRecovery Mode", "0");	// 0 - Selective Repeat
					if ( serverQuery.containsKey( "ErrorSegment Number" ) )
						serverQuery.put("ErrorSegment Number", error_segment_number); // 0 - No segments with error;
					if ( serverQuery.containsKey( "Number Of Objects" ) )
						serverQuery.put("Number Of Objects", numberOfObjects);
					if ( serverQuery.containsKey( "Browser Type" ) )
						serverQuery.put("Browser Type", browser_type);
				}
			}
			//End of preparing response for server query

			//Get zTCP Segments from server and write to file(s)
			if ( hObj instanceof My_zTCP_Data ) {
				zData = (My_zTCP_Data) hObj;
				if ( zData == null ) {
					error_occurred = true;
					System.out.println ("Error in getting zTCP from Server");
					proceed_to_exit ();
				}
				else {
					writeToFile ( zData );
					finBitSet = zData.isFinBitSet();
				}
			}

			//Check for Object serviced information from server (for 1.1 browser requests)
			if ( hObj instanceof HashMap ) {
				HashMap objectServiced = new HashMap <String, String> ();
				objectServiced = (HashMap) hObj;
				if ( objectServiced.containsKey("Object Number") )
					objNum = (String) objectServiced.get("Object Number");
				if ( objectServiced.containsKey("Object Status") )
					objStatus = (String) objectServiced.get("Object Status");

				if ( objNum != null && objStatus != null &&
						( objStatus.compareTo("COMPLETE") == 0 ) ) {

					outd.println();
					outd.println( OBJECT_END_SEPERATOR + REQUEST_OBJECT + " (" + objNum + ")" + OBJECT_TAIL );
					outd.println ();

					outz.println();
					outz.println( OBJECT_END_SEPERATOR + REQUEST_OBJECT + " (" + objNum + ")" + OBJECT_TAIL );
					outz.println();

					int objNumber = Integer.parseInt(objNum);
					//If FIN Bit is not set, server has more objects to send
					if (finBitSet) {
						request_to_close_received = true;
						readyToReadNextObject = false;
					}
					if ( !finBitSet && objNumber <= 3)
					    readyToReadNextObject = true;
					else
						readyToReadNextObject = false;
				}
			}

		} //for
		outz.println();
		outz.println(END_RECEIVE_WINDOW_COMMENT);
		outz.println();

	/*	int sz = fromServer.size();
	/	if ( sz > 1 ) {
			serverQuery = new HashMap <String, String> ();
			for ( Object hObj : fromServer ) {
			//Object hObj =  fromServer.get( sz-1 );
				if ( hObj instanceof HashMap ) {
					serverQuery = (HashMap) hObj;

					if ( serverQuery != null ) {
						if ( serverQuery.containsKey("ErrorRecovery Mode") )
							serverQuery.put("ErrorRecovery Mode", "0");	// 0 - Selective Repeat
						if ( serverQuery.containsKey( "ErrorSegment Number" ) )
							serverQuery.put("ErrorSegment Number", error_segment_number); // 0 - No segments with error;
						if ( serverQuery.containsKey( "Number Of Objects" ) )
							serverQuery.put("Number Of Objects", numberOfObjects);
						if ( serverQuery.containsKey( "Browser Type" ) )
							serverQuery.put("Browser Type", browser_type);
					}
				}
			} //for
		}
		//End of preparing response to server query

		if ( sz >= 1 ) {
			for (int ix = 0; ix <= sz-1; ix++) {
				Object zObj = fromServer.get(ix);
				if ( zObj instanceof My_zTCP_Data ) {
					zData = (My_zTCP_Data) zObj;
					if ( zData == null ) {
						error_occurred = true;
						System.out.println ("Error in getting zTCP from Server");
						proceed_to_exit ();
					}
					else {
						writeToFile ( zData );
					}
				}
			}
		} //sz>=1

		*/
	}


	void initToReadNextObject () {
		//Initialize these variables TO BE ABLE TO read next object
		requestForByteStreamNum = 0;
		currByteStreamNumFromSrvr = -1;
		resendByteStreamNum = -1;
		continueFromByteStreamNum = -1;
		clnt_segment = 0;

		//Prepare the output file
		String fName = CONFIG_FILE_LOCATION.concat(CONFIG_FILE_NAME);
		File cgFile = new File ( fName );

		prepareToReceivePayload (cgFile);
		readyToReadNextObject = false;

		outd.println();
		outd.println(OBJECT_BEGIN_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL);
		outd.println();

		outz.println();
		outz.println(OBJECT_BEGIN_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL);
		outz.println();
	}


	void writeToFile (My_zTCP_Data cData) {
		int synBit = -1;
		int finBit = -1;

		outz.println();
		outz.println(BEGIN_OF_SEGMENT);
		outz.println();

		synBit = ( cData.isSynBitSet() ) ? 1 : 0;
		finBit = ( cData.isFinBitSet() ) ? 1 : 0;

		if (clnt_segment == 0) {
			if ( synBit != 1 ) {
					error_occurred = true;
					System.out.println ("TCP Handshake with server failed - client exciting");
					proceed_to_exit();
			}
			else {	//Client can Begin to request data from server
				requestForByteStreamNum = 0;

			}
		}

		if ( finBit == 1 )
				request_to_close_received = true;
		try {

			//Get Payload
			byte[] pay_load = cData.getPayload();
			int bLen = pay_load.length;

			//Get Checksum after reading payload
			checksum_error = false;
			String ckSum = cData.getCheckSum();
			if (ckSum.compareTo("0000") != 0)
				checksum_error = true;

			int recentSrvrSeqNum = cData.getSequenceNumber();

			if (!checksum_error ) {
				if (exponential_window) { //Happens if selective repeat segment from server is received
					if ( currByteStreamNumFromSrvr != -1
						&& recentSrvrSeqNum < currByteStreamNumFromSrvr ) {
						// currByteStreamNumFromSrvr remains unchanged.
						continueFromByteStreamNum = currByteStreamNumFromSrvr;
					}
				}
			}

			currByteStreamNumFromSrvr = recentSrvrSeqNum;

			if ( checksum_error ) {
				//error_occurred = true;
				//Selective Repeat being enforced
				requestForByteStreamNum = currByteStreamNumFromSrvr; //(requestForByteStreamNum + 1) - mss_val;
				resendByteStreamNum = requestForByteStreamNum;
				System.out.println ("Checksum error detected. Client expecting ACK#: " + resendByteStreamNum);
			}

			int sPort = cData.getSourcePort();
			outz.println("Source Port: " + sPort);

			int dPort = cData.getDestinationPort();
			outz.println("Destination Port: " + dPort);

			int sNum = cData.getSequenceNumber();
			outz.println("Sequence Number: " + sNum);

			int ackNum = cData.getACKNumber();
			outz.println("ACK Number: " + ackNum);

			int ackBit = ( cData.isACKBitSet() ) ? 1 : 0;
			outz.println("ACK Bit: " + ackBit);

			outz.println("SYN Bit: "+ synBit);

			outz.println("FIN Bit: "+ finBit);

			outz.println("Checksum: " + ckSum);

			int opt = cData.getMSS();	//MSS
			outz.println("Option: " + opt);


			//Print Payload
			outz.println("Payload: ");
			outz.write(pay_load);

			outz.println();
			outz.println(END_OF_SEGMENT);
			outz.println();

			outd.write(pay_load);
		}
		catch (IOException io) {
			io.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}


	public static void main(String[] args) {

		System.out.println ("Please type either 1 or 2 to select your option");
		System.out.println("1. Comp-a (HTTP/1.0)");
		System.out.println("2. Comp-b (HTTP/1.1)");

		try {
		BufferedReader reader = new BufferedReader (
									new InputStreamReader (System.in) );


			String inputRead = reader.readLine();
			browser_type = null;

			if ( inputRead.compareTo("1") == 0 )
				browser_type = "1.0";
			else if ( inputRead.compareTo("2") == 0 )
				browser_type = "1.1";
			else {
				System.out.println ("Invalid option. Please type either 1 or 2");
				System.exit(1);
			}
			System.out.println ("Browser type selected is: " + browser_type);

		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit (1);
		}


		if (args.length > 0)
	    	validateArgs (args);

		my_browser = new MySimWebBrowser ();
	}


	/////////Private Methods//////////////////////

	void proceed_to_exit () {
		manage_connection ();
		System.exit(1);
	}

	private static boolean validateArgs( String[] args ) {

		if (args == null || args.length < 2) {
			return false;
		}

		String option = args[ 1 ];
		String portStr = args[ 2 ];

		//Check if portNumber is supplied
		if ( isNullOrEmpty(option) || isNullOrEmpty(portStr) || option != "-P" ) {
			System.out.println(USAGE_MESSAGE);
			return false;
		}

		if ( validatePortNumber (portStr) ) {
			portNumber = Integer.parseInt(portStr);
			return true;
		}
		return false;


		/*portNumber = Integer.parseInt(portStr);
		if (portNumber < 1024  || portNumber > 65536) {
			System.out.println(INVALID_PORT_NUMBER);
			return false;
		}

		return true; */
	}

	private static boolean isNullOrEmpty (String str) {
		if( str == null || "".equals( str.trim() ) )
			return true;
		return false;
	}

	private static boolean validatePortNumber (String portNo) {
		int portNum = Integer.parseInt(portNo);
		if (portNum < 1024  || portNum > 65536) {
			System.out.println(INVALID_PORT_NUMBER);
			return false;
		}
		return true;
	}

	private static boolean validateLocation ( String loc ) {
		File dirLoc = new File (loc);
		if ( ! dirLoc.exists() || ! dirLoc.isDirectory() ) {
			System.out.println ("The specified location " + dirLoc + " does not exist OR is not a directory");
			return false;
		}

		return true;
	}

	void send_FINACK_toserver () {
		try {
			zTCP_data_toserver = prepare_FIN_toserver ();

			//Send FIN ACK to Server
			objectToServer = new ObjectOutputStream ( clientSocket.getOutputStream() );
			objectToServer.writeObject(zTCP_data_toserver);
		}
		catch (Exception e) {
			manage_connection ();
			System.out.println ("Exception occurred");
			e.printStackTrace();
		}
	}

	void send_FIN_Segment () {
		try {
			zTCP_data_toserver = prepare_FIN_toserver ();

			//Send FIN Segment to Server
			objectToServer = new ObjectOutputStream ( clientSocket.getOutputStream() );
			objectToServer.writeObject(zTCP_data_toserver);


			//Get FIN ACK from Server
			objectFromServer = new ObjectInputStream ( clientSocket.getInputStream() );
			zTCP_data_fromserver = new ArrayList <Object> ();
			zTCP_data_fromserver = (ArrayList) objectFromServer.readObject();
			process_FINSegment_fromserver ();
			manage_connection ();
		}
		catch (Exception e) {
			manage_connection ();
			System.out.println ("Exception occurred");
			e.printStackTrace();
		}

	}


	private void doSynSegmentProcessing () {

	}

	private void manage_connection () {
		//Take care of connection tear down
		 //Closing connection

		outd.flush();
	    outd.close();

	    outz.flush();
	    outz.close();

		try {
			if ( objectFromServer != null )
				objectFromServer.close();
			if ( objectToServer != null )
				objectToServer.close();
			if ( clientSocket != null )
				clientSocket.close();
			if ( in_cgfile != null )
				in_cgfile.close ();

			System.out.println ("Browser TCP connection closed");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}

	}


	private ArrayList prepare_FIN_toserver () {
		ArrayList dataToServer = new ArrayList<Object> ();
		My_zTCP_Data zData = new My_zTCP_Data ();

		zData.setSynBit( 0 );
		zData.setSourcePort( portNumber );
		zData.setDestinationPort( 80 );
		zData.setSequenceNumber( 0 );
		zData.setACKBit( 1 );
		zData.setFinBit( 1 );
		String payload = new String ( EMPTY_STRING );
		byte[] pay_load = payload.getBytes();
		zData.setPayload( pay_load );
		zData.setACKNumber( 0 );
		zData.setCheckSum("0000");
		zData.setMSS( mss_val );	//MSS value

		dataToServer.add( zData );
		return dataToServer;
	}

	private void process_FINSegment_fromserver () {
		ArrayList fromServer = zTCP_data_fromserver;
		My_zTCP_Data zData = null;

		if ( fromServer.size() >= 1 ) {
			 Object zObj = fromServer.get(0);
			 if ( zObj instanceof My_zTCP_Data ) {
				 zData = (My_zTCP_Data)zObj;
				 if ( zData == null ) {
					 error_occurred = true;
					 System.out.println ("Error in process FIN segment from Server");
					 proceed_to_exit ();
				 }
			 }
		}

		int fynBit = ( zData.isFinBitSet() ) ? 1 : 0;
		if ( fynBit == 1 ) {
				System.out.println ("Received FIN Segment ACK from Server. Web Browser closing TCP now...");
				writeToFile (zData);

				outd.println();
				outd.println( OBJECT_END_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL );
				outd.println ();

				outz.println();
				outz.println( OBJECT_END_SEPERATOR + REQUEST_OBJECT + OBJECT_TAIL );
				outz.println();
		}


	}



	////////////////private Getters and Setters ///////////////////////


	boolean isSynSegment () {

		boolean isSyn = false;


		return isSyn;

	}

	boolean isFinBitSet () {
		boolean isFinBit = false;


		return isFinBit;
	}


	void getBrowserType () {



	}

	//MSS negotiated with Web Browser
	int getMSS () {

		int mss = -1;

		return mss;

	}



	void getErrRecoveryMode () {



	}

	void getErrSegmentNumber () {



	}

	void getNumOfObjects () {



	}



}