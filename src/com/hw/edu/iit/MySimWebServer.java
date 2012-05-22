/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.hw.edu.iit;

/**
 *
 * @author Sangeetha Ramadurai
 */
import java.net.*;
import java.io.*;
import java.util.*;
import static com.hw.edu.iit.MySimulateWebConstants.*;

public class MySimWebServer extends Thread {

	private static int portNumber = 0;	//5025;
	private final static String USAGE_MESSAGE = "MySimWebServer -P <port number - any number between 1024 to 65536>";
	private final static String INVALID_PORT_NUMBER = "Invalid port number. Please enter a number between 1024 to 65536";
	private static MySimWebServer server;

	private String INPUT_DIR_LOCATION = null;
	private long totalLength = 0;
	byte[] inputBuffer = null;
	BufferedReader inputReader = null;
	File ipFile = null;

	private int srvr_lastSeqNumSent;
	private int srvr_segmentNumber;
	private int currPos = 0;
	private int recoveryPos = 0;
	private boolean request_to_close_sent = false;

	private ObjectInputStream objectFromClient;
	private ObjectOutputStream objectToClient;
	private ServerSocket welcomeSocket;
	private Socket connectionSocket;

	private ArrayList zTCP_data_fromclient, zTCP_data_toclient;
	private HashMap queryResponse;
	private HashMap queryClient ;
	private HashMap objectServiced;

	//Incoming client connection variables
	private static int expectedClientSeqNum = 0;
	private String brs_Type;			//0 - HTTP/1.0, 1 - HTTP/1.1
	private int brs_MSS;
	private int brs_errorRecoveryMode;	//0 - Selective Repeat, 1 - GoBackN
	private int brs_errorSegmentNumber;
	private int brs_numObjRequest;

	private boolean exponential_window;
	private int window_size;

	private int objectNumber;
	private String objectStatus;

	private String payloadRequested;
	private String lastPayloadRequested;



	public MySimWebServer () {
		init();
		try {
			welcomeSocket = new ServerSocket (portNumber);
			run();
		}
		catch (IOException e) {
			manage_client_connection ();
			System.out.println("Server did not start. Exception occurred");
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run()
	{
		try {
				System.out.println("Server up and ready... Waiting for requests...");
			    connectionSocket = welcomeSocket.accept();
			    System.out.println("Connection accepted");

			    do {
			    	//Get data from client
			    	objectFromClient = new ObjectInputStream ( connectionSocket.getInputStream() );
			    	zTCP_data_fromclient = new ArrayList <Object> ();
			    	zTCP_data_fromclient = (ArrayList) objectFromClient.readObject();

			    	//Process data from client and prepare data to send back to client
			    	zTCP_data_toclient = process_zTCP_fromclient ();

			    	//TODO: revisit this
		    		//Send data to client
		    		objectToClient = new ObjectOutputStream ( connectionSocket.getOutputStream() );
		    		objectToClient.writeObject(zTCP_data_toclient);

		    		//reset contents after issuing the query
		    		if ( queryClient != null &&
		    				! queryClient.isEmpty() )
		    			queryClient.clear();

			    	//Connection Management based on WebBrowser Type 1.0/1.1
			    	//manage_client_connection (brs_Type);

			    }	//do
			    while ( !request_to_close_sent );

			    enter_FIN_WAIT1 ();
				//manage_client_connection ();
		}
		catch (Exception e) {
				System.out.println ("Exception occurred");
				e.printStackTrace();
				manage_client_connection ();
		}

	}	//run

	void init () {
		brs_errorRecoveryMode = -1;
		brs_errorSegmentNumber = -1;
		brs_numObjRequest = -1;
		brs_Type = "";
		exponential_window = false;
		window_size = 1;

		lastPayloadRequested = null;

		//Object serviced information
		objectNumber = 0;
		objectStatus = "INCOMPLETE";

		srvr_segmentNumber = 0;
		srvr_lastSeqNumSent = -1;

		//Server side query to client - NumberOfObjects, ErrorRecoveryMode,
	    //ErrorSegmentNumber and Browser Type
	    //Hashmap with the below queries created once for each client
		queryClient  = new HashMap<String, String> ();
		queryClient.put("ErrorRecovery Mode", " ");
		queryClient.put("ErrorSegment Number", " ");
		queryClient.put("Number Of Objects", " ");
		queryClient.put("Browser Type", " ");


		//Open the config file
		String line = null;
		INPUT_DIR_LOCATION = null;
		String fName = CONFIG_FILE_LOCATION.concat(CONFIG_FILE_NAME);
		File cgFile = new File ( fName );
		if ( !( cgFile.exists() && cgFile.isFile() ) ) {
			System.out.println ("Missing configuration information. Looking for file: " + fName);
		  	proceed_to_exit ();
		}
		try {
			BufferedReader in = new BufferedReader ( new InputStreamReader
		   											( new FileInputStream ( cgFile ) ) );
		   	while ( ( line = in.readLine() ) != null ) {
		   		String[] locStr = line.split ( "=", 2 );
		   		if ( locStr.length == 2 ) {
		   			if ( locStr[0].compareTo(SERVERLOCATION) == 0 ) {
		   				if ( ! isNullOrEmpty(locStr[1]) && validateLocation(locStr[1]) )
		   					INPUT_DIR_LOCATION = locStr[1];
		   			}
		   			else if ( locStr[0].compareTo("EXPONENTIAL_WINDOW_SIZE") == 0 ) {
		   				if ( ! isNullOrEmpty(locStr[1]) )
		   					exponential_window = ( locStr[1].compareTo("true") == 0 ) ? true : false;
		   			}
		   			else if ( locStr[0].compareTo("SERVER_PORT") == 0 ) {
	   					if (! isNullOrEmpty(locStr[1]) && validatePortNumber(locStr[1]) )
	   						portNumber = Integer.parseInt(locStr[1]);
	   				}
		   		}
		   	}
			in.close ();
		}
		catch (IOException io) {
			System.out.println ("Config file problem");
			io.printStackTrace();
			proceed_to_exit ();
		}

		if ( INPUT_DIR_LOCATION == null || portNumber == 0 ) {
			System.out.println ("Missing configuration information");
		  	proceed_to_exit ();
		}

	}


	public static void main(String[] args) {

		//Testing Begins

	//	TestReadWrite ();
		//Testing Ends

		if (args.length > 0)
	    	validateArgs (args);

	    server = new MySimWebServer ();

	}


	/////////Private Methods//////////////////////

	void proceed_to_exit () {
		manage_client_connection ();
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

		return true;
		*/
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

	private ArrayList process_zTCP_fromclient () {
		int clientPort = 0;
		int srvrSeqNum = 0;
		boolean finSet = isFinBitSet();
		boolean synSet = isSynSegment();
		ArrayList toClient = null;

		brs_MSS = getMSS ();			//Negotiated MSS with client
		clientPort = getClientPort ();	//Source Port# of the incoming client request
		srvrSeqNum = getExpectedServerSeqNum (); //Sequence number of the next segment client expects from server

		if ( finSet || synSet )	//Client has sent a FIN segment/FIN ACK
			resetWindowSize ();

		if ( !finSet ){	//if client has not requested for close connection
			payloadRequested = getpayLoadFromClient ();
			if ( payloadRequested == null )
				proceed_to_exit();

			getQueryResponse();

			if (srvr_lastSeqNumSent == srvrSeqNum) {
				//Error occurred resend data that's expected by client
				System.out.println ("Error detected by client.. Resend data for segment with byteStreamNumber starting at " + srvr_lastSeqNumSent);
				resetWindowSize ();
			}
			else if (srvrSeqNum < srvr_lastSeqNumSent) {
				System.out.println ("Error detected by client.. Resend the specific data segment to client with byteStreamNumber starting at " + srvrSeqNum);
				resetWindowSize ();
			}

		}

		toClient = prepareDataToClient (clientPort, srvrSeqNum, finSet);

		if ( exponential_window && !finSet && !synSet ) {
			if ( getBrowserType().compareTo("1.1") == 0 ) {
				String oStatus = "INCOMPLETE";
				if ( objectServiced != null && !objectServiced.isEmpty() ) {
					if (objectServiced.containsKey("Object Status"))
						oStatus = (String) objectServiced.get("Object Status");
							if (oStatus.compareTo("COMPLETE") == 0)
								resetWindowSize ();
				}
				else
					calculateWindowSize ();
			}
			else
				calculateWindowSize ();
		}

		return toClient;
	}

	void getQueryResponse () {
		int sz = zTCP_data_fromclient.size();

		if ( queryResponse != null )
			queryResponse.clear();
		if ( sz > 1 ) {
			queryResponse = new HashMap <String, String> ();
			queryResponse = (HashMap) zTCP_data_fromclient.get( sz-1 );
			if ( queryResponse != null ) {

				//Get BrowserType
				if ( queryResponse.containsKey("Browser Type") )
					brs_Type = (String) queryResponse.get("Browser Type");
				//Get ErrorRecovery Mode
				if ( queryResponse.containsKey("ErrorRecovery Mode") ) {
					String errorRecoveryMode = (String) queryResponse.get("ErrorRecovery Mode");
					brs_errorRecoveryMode = Integer.parseInt(errorRecoveryMode);
				}
				//Get ErrorSegment Number
				if ( queryResponse.containsKey("ErrorSegment Number") ) {
					String errorSegmentNumber = (String) queryResponse.get("ErrorSegment Number");
					brs_errorSegmentNumber = Integer.parseInt(errorSegmentNumber);
				}
				//Get Number Of Objects
				if ( queryResponse.containsKey("Number Of Objects") ) {
					String numObjRequestStr = (String) queryResponse.get("Number Of Objects");
					brs_numObjRequest = Integer.parseInt(numObjRequestStr);
				}

				System.out.println ("Server interacting with Browser Type: " + brs_Type);
				String Mode = ( brs_errorRecoveryMode == 0 ) ?  "Selective Repeat" : "GoBackN";
				System.out.println ("ErrorRecovery Mode negotiated with client is: " + Mode);
				System.out.println ("Number of objects requested by client is: " + brs_numObjRequest);
				System.out.println ("Error segment number to simulate is: " + brs_errorSegmentNumber);

			}
		}
	}

	private ArrayList prepareDataToClient (int toPort, int srvrSeqNum, boolean finBitSet) {

		//TODO: Check that the String parameters are not-null/not-empty

		//Sequence number (32 bits) – has a dual role
		//If the SYN flag is present then this is the initial sequence number and the first data byte is the sequence number plus 1
		//if the SYN flag is not present then the first data byte is the sequence number
		boolean isSynSeg = isSynSegment();
		int errSegment = 0;
		int sz = 0;
		int wsz = getWindowSize ();
		zTCP_data_toclient = new ArrayList <Object> ();

		int eofPayload = 0; //0 - payload available, -1  - End Of Payload

		if (exponential_window)
			System.out.println ( "Current window size is: " + wsz);

		int seqNumToSend = srvrSeqNum;
		for ( int ix = 0; ( ix < wsz && eofPayload != -1 ); ix++ ) {
			My_zTCP_Data cData = new My_zTCP_Data ();

			String payLoad = new String(EMPTY_STRING);
			int len = payLoad.length();
			byte[] payload_data = payLoad.getBytes();

			cData.setSourcePort ( portNumber );
			cData.setDestinationPort ( toPort );
			cData.setACKBit ( 1 );
			cData.setFinBit( 0 );
			cData.setMSS( brs_MSS );

			cData.setCheckSum("0000");	//No Error

			//	Check if client has asked for close connection
			if ( finBitSet ) {
				cData.setFinBit( 1 );		//close connection
				request_to_close_sent = true;
			}

			//Check if it is a SYN Segment
			if ( isSynSeg ) {
				doSynSegmentProcessing (cData);
			}
			else {
				//System.out.println( "Responding to client request..." );
				cData.setSynBit( 0 );
				cData.setSequenceNumber(seqNumToSend);
				cData.setACKNumber( 0 ); //first data byte expected from client
				srvr_segmentNumber += 1;
				srvr_lastSeqNumSent = seqNumToSend;
			}

			//Checksum

			//Payload Data
			//(ToByteStream-FromByteStream)+1 == brs_MSS
			if ( isSynSeg || finBitSet ) {
				cData.setPayload(payload_data);
			}
			else if ( !isSynSeg &&  !finBitSet ) {
				int payloadFrom = 0;
				int payloadTo = 0;

				if ( payloadRequested.compareTo(new String( EMPTY_STRING)) != 0 )
					lastPayloadRequested = new String( EMPTY_STRING);

				//New Object Request
				if ( payloadRequested.compareTo(new String( EMPTY_STRING)) != 0 ) {
					System.out.println ("PayloadRequested is: " + payloadRequested);

					//Init for next object -- better done in preparePayLoadForClient
					//Init for next object complete

					String fToOpen = INPUT_DIR_LOCATION.concat( payloadRequested );
					ipFile = new File ( fToOpen );
					if ( ! ipFile.exists() || ! ipFile.isFile() ) {
						System.out.println ("Error: " + "Unable to locate " + fToOpen);
						proceed_to_exit ();
					}

					totalLength = ipFile.length();
					currPos = 0;

					try {
						FileInputStream fStream = new FileInputStream (ipFile);

						//inputReader = new BufferedReader ( new InputStreamReader
				   			//			( new FileInputStream ( ipFile ) ) );

						//Read complete file contents into a buffer
						int tLen = (int)totalLength;
						inputBuffer = new byte[tLen];
						int totalRead = fStream.read(inputBuffer, 0, tLen);
						if ( totalRead != tLen ) {
							System.out.println("Init error");
							proceed_to_exit ();
						}
					}
					catch ( IOException io ) {
						System.out.println ("Exception occurred on trying to read " + payloadRequested);
						io.printStackTrace();
						proceed_to_exit ();
					}
				}	//New Object Request

				payloadRequested = new String (EMPTY_STRING);	//resetting the payload string
				payloadFrom = seqNumToSend;

				eofPayload = preparePayLoadForClient (inputReader, payloadFrom, cData);

				//simulate checksum error only if we have data to send
				if (! cData.isFinBitSet() ) {
					//Checksum
					if ( ( ( brs_errorSegmentNumber = getErrSegmentNumber() ) != 0 ) &&
							( srvr_segmentNumber == brs_errorSegmentNumber ) &&
							( srvr_segmentNumber > 0 ) )	{	//Segment number to have corrupted checksum
						cData.setCheckSum("0001");	//Sending corrupted segment
					}
				}
			} // !FIN && !SYN

			zTCP_data_toclient.add(cData);

			seqNumToSend = seqNumToSend + brs_MSS;
		} 	//for loop

		if ( eofPayload == -1 )
			resetWindowSize ();

		//Add HashMap object if not empty
		if ( ! queryClient.isEmpty() )
			zTCP_data_toclient.add( queryClient  );

		//Object Serviced information
		if ( objectServiced != null && ! objectServiced.isEmpty() )
			zTCP_data_toclient.add( objectServiced );

		return zTCP_data_toclient;
	}


	int preparePayLoadForClient (BufferedReader ip, int from, My_zTCP_Data zData) {
		char[] sBuffer = new char[brs_MSS];
		int numRead = -1;
		String eStr = new String ( EMPTY_STRING );
		byte[] payload_data = null;
		int payloadStatus = 0; //0 - No error, -1 - All read, EOF

		if ( objectServiced != null )
			objectServiced.clear();

		// cbuf - Destination buffer,
		// off - Offset at which to start storing characters,
		// len - Maximum number of characters to read
		//read (cbuf, off, len)

		try {
			if ( from > totalLength ) {
				payload_data = eStr.getBytes();

				payloadStatus = -1;
				if ( getBrowserType().compareTo("1.0") == 0 ) {
					System.out.println("All data read");
					zData.setFinBit( 1 );
					//payloadStatus = -1;
				}
				else if ( getBrowserType().compareTo("1.1") == 0 ) {
					int totalObjRequested = getNumOfObjects();

					//Init for next object
					srvr_segmentNumber = 0;
					srvr_lastSeqNumSent = -1;
					resetWindowSize ();
					//Init for next object complete

					objectNumber += 1;	//One object read complete
					objectStatus = "COMPLETE";


					//Check for the numOfObjects requested and how many has already been sent
					if ( objectNumber >=  totalObjRequested ) {
						System.out.println("All data read");
						zData.setFinBit( 1 );
						payloadStatus = -1;
					}

					if (objectNumber <= 3) {
						objectServiced = new HashMap <String, String> ();
						Integer nObj = new Integer(objectNumber);
						String objStr = nObj.toString();
						objectServiced.put("Object Number", objStr);
						objectServiced.put("Object Status", objectStatus);
					}

				}

			}
			else if ( from <= totalLength && currPos < totalLength ) {
				int readUpTo = (from+brs_MSS) - 1;
				int rem = (int)totalLength - currPos;
				int readAmount = (readUpTo <= totalLength) ? brs_MSS : rem;

				if (from < currPos) {
					System.out.println ("Error detected and a segment is being resent");
					rem = (int)totalLength - from;
					readAmount = (readUpTo <= totalLength) ? brs_MSS : rem;
				}

				payload_data = new byte[(int)readAmount];
				//System.arraycopy(src, srcPos, dest, destPos, length);
				System.arraycopy(inputBuffer, from, payload_data, 0, readAmount);
				System.out.println ("Server sending ByteSequenceNumber beginning: " + from);

				if ( !(from < currPos) )
					currPos += readAmount;
			}

			zData.setPayload( payload_data );
		}
		catch (Exception e) {
			e.printStackTrace();
			proceed_to_exit();
		}

		return payloadStatus;
	}

	void resetWindowSize () {
		window_size = 1;
	}

	int calculateWindowSize () {
		window_size = window_size * 2;
		return window_size;
	}

	int getWindowSize () {
		return window_size;
	}

	private void doSynSegmentProcessing ( My_zTCP_Data czData  ) {
		//Check for client type, #of objects requested and store them locally
		//Also negotiate for selective repeat behavior
		System.out.println ("ThreeWay handshake in progress...");
		String type = "";

		getNumOfObjects();
		if ( ( type = getBrowserType() ).equals("1.0") )
			brs_numObjRequest = 1;	//One request handled per connection

		//Initialize SYN segment specific variables
		czData.setSynBit( 1 ); 		//Handshake in progress
		czData.setSequenceNumber( 0 ); //Initial sequence number
		czData.setACKNumber( 0 );		//first data byte expected from client
	}


	String getpayLoadFromClient () {

		My_zTCP_Data cData = null;
		String payloadFromClient = 	null; //new String(EMPTY_STRING);
		if ( zTCP_data_fromclient.size() >= 1 ) {
			Object zObj = zTCP_data_fromclient.get(0);
			if ( zObj instanceof My_zTCP_Data ) {
				cData = (My_zTCP_Data) zObj;
				if ( cData != null ) {
					byte[] payload = cData.getPayload();
					int len = payload.length;
					payloadFromClient = new String (payload);	//(payload);
				}
			}
		}

		if (payloadFromClient == null)
			System.out.println ("Error in getting PayLoad from client");

		return payloadFromClient;
	}


	void manage_client_connection () {
		//Take care of connection tear down

		//if ( (brs_Type != null) && (brs_Type.equals("1.0") ) {

		//}
//		Closing connection
		try{
			if ( objectFromClient != null )
				objectFromClient.close();
			if ( objectToClient != null )
				objectToClient.close();
			if ( welcomeSocket != null )
				welcomeSocket.close();
			if ( inputReader != null )
				inputReader.close();
		}
		catch(IOException ioException){
			//Do nothing
			//ioException.printStackTrace();
		}
		System.out.println ("Server TCP is closed");
	}

	void enter_FIN_WAIT1 () {

		try {
			if ( connectionSocket != null ) {
				//	Get FIN Segment from client
				objectFromClient = new ObjectInputStream ( connectionSocket.getInputStream() );
				zTCP_data_fromclient = new ArrayList<Object> ();
				zTCP_data_fromclient = (ArrayList) objectFromClient.readObject();

				//Process data from client and prepare data to send back to client
				zTCP_data_toclient = process_FINSegment_fromclient ();

				//Send data to client
				objectToClient = new ObjectOutputStream ( connectionSocket.getOutputStream() );
				objectToClient.writeObject(zTCP_data_toclient);

				enter_FIN_WAIT2 ();
			}
		}
		catch ( IOException io ) {
			System.out.println ("Client connection has possibly closed");
			manage_client_connection ();
			System.exit(1);
		}
		catch (Exception e) {
			System.out.println ("Exception occurred");
			e.printStackTrace();
			manage_client_connection ();
			System.exit(1);
		}

		manage_client_connection ();
	}

	void enter_FIN_WAIT2 () {
		try {
			Thread.sleep(30);	//30milliseconds

			//	Get FIN Segment from client
			objectFromClient = new ObjectInputStream ( connectionSocket.getInputStream() );
			zTCP_data_fromclient = new ArrayList<Object> ();
			zTCP_data_fromclient = (ArrayList) objectFromClient.readObject();

			//Process data from client and prepare data to send back to client
			if ( isFinBitSet () ) {
				zTCP_data_toclient = process_FINSegment_fromclient ();

				//Send data to client
				if ( objectToClient != null ) {
					objectToClient = new ObjectOutputStream ( connectionSocket.getOutputStream() );
					objectToClient.writeObject(zTCP_data_toclient);
				}
			}
		}
		catch ( IOException io ) {
			System.out.println ("Client TCP Connection is closed");
			manage_client_connection ();
			System.exit(1);
		}
		catch (Exception e) {
			System.out.println ("Exception Occurred");
			e.printStackTrace();
			manage_client_connection ();
			System.exit(1);
		}

	}


	ArrayList process_FINSegment_fromclient () {
		int clientPort = getClientPort ();	//Source Port# of the incoming client request

		zTCP_data_toclient = new ArrayList<Object>();
		My_zTCP_Data zData = new My_zTCP_Data();

		zData.setSourcePort( portNumber );
		zData.setDestinationPort( clientPort );
		zData.setACKBit( 1 );

		//Send FIN Segment ACK to client
		if ( isFinBitSet() )
			zData.setFinBit( 1 );

		zData.setSynBit( 0 );
		zData.setSequenceNumber( 0 );
		zData.setCheckSum( "0000" );
		zData.setFinBit( 1 );
		String payload = new String( EMPTY_STRING );	//" ";
		byte[] pay_load = payload.getBytes();
		zData.setPayload( pay_load );

		zTCP_data_toclient.add( zData );
		return zTCP_data_toclient;
	}


	////////////////private Methods ///////////////////////


	boolean isSynSegment () {

		boolean isSyn = false;

		My_zTCP_Data cData = null;
		if ( zTCP_data_fromclient.size() >= 1 ) {
			Object zObj = zTCP_data_fromclient.get(0);
			if ( zObj instanceof My_zTCP_Data ) {
				cData = (My_zTCP_Data) zObj;
				if ( cData != null )
					isSyn = cData.isSynBitSet();
			}
		}
		return isSyn;

	}

	boolean isFinBitSet () {
		boolean isFinBit = false;

		My_zTCP_Data cData = null;
		if ( zTCP_data_fromclient.size() >= 1 ) {
			Object zObj = zTCP_data_fromclient.get(0);
			if ( zObj instanceof My_zTCP_Data ) {
				cData = (My_zTCP_Data) zObj;
				if ( cData != null )
					isFinBit = cData.isFinBitSet();
			}
		}
		return isFinBit;
	}


	//MSS negotiated with Web Browser
	int getMSS () {
		int mss = 1;

		My_zTCP_Data cData = null;
		if ( zTCP_data_fromclient.size() >= 1 ) {
			Object zObj = zTCP_data_fromclient.get(0);
			if ( zObj instanceof  My_zTCP_Data) {
				cData = (My_zTCP_Data) zObj;
				if ( cData != null )
					mss = cData.getMSS();
			}
		}
		return mss;
	}

	int getClientPort () {
		int clientPort = 0;

		//Source Port# of the incoming client request
		My_zTCP_Data cData = null;
		if ( zTCP_data_fromclient.size() >= 1 ) {
			Object zObj = zTCP_data_fromclient.get(0);
			if ( zObj instanceof My_zTCP_Data ) {
				cData = (My_zTCP_Data) zObj ;
				if ( cData != null )
					clientPort = cData.getSourcePort();
			}
		}
		return clientPort;
	}

	int getExpectedServerSeqNum () {

		int srvrSeqNum = 0;

		My_zTCP_Data cData = null;
		if ( zTCP_data_fromclient.size() >= 1 ) {
			Object zObj = zTCP_data_fromclient.get(0);
			if ( zObj instanceof My_zTCP_Data ) {
				cData = (My_zTCP_Data) zObj;
				if ( cData != null &&
					 cData.isACKBitSet() )
					srvrSeqNum = cData.getACKNumber();
			}
		}
		return srvrSeqNum;
	}



	int getErrRecoveryMode () {
		//Get Error recovery mode negotiated with the web browser
		//Changing this: Selective Repeat by default
		return brs_errorRecoveryMode;
	}

	int getErrSegmentNumber () {

		//Changing this: No, error segment by default
		return brs_errorSegmentNumber;
	}

	int getNumOfObjects () {

		//Changing this: No object requested by default
		return brs_numObjRequest;
	}


	String getBrowserType () {
		//	Web Browser Type
		return brs_Type;
	}

	static void  TestReadWrite () {
		String fToOpen = "Z:\\ThirdPartyApplns\\Eclipse\\Workspace\\MyWebSimulator\\CS542\\server\\cs.iit.edu.htm";
		String dFileName = "Z:\\ThirdPartyApplns\\Eclipse\\Workspace\\MyWebSimulator\\CS542\\client\\Client-a.cs.iit.edu.html";
		File dFile = new File ( dFileName );
		PrintStream outd = null;
		BufferedReader inputReader = null;
		FileInputStream fis = null;
		int brs_MSS = 1000;

		File ipFile = new File ( fToOpen );
		if ( ! ipFile.exists() || ! ipFile.isFile() ) {
			System.out.println ("Error: " + "Unable to locate " + fToOpen);
		}

		long totalLength = ipFile.length();

		try {
			inputReader = new BufferedReader ( new InputStreamReader
			 					( new FileInputStream ( ipFile ) ) );

			//fis = new FileInputStream ( ipFile );
			outd = new PrintStream ( new FileOutputStream (dFile) );
		}
		catch ( IOException io ) {
			System.out.println ("Exception occurred on trying to read " + fToOpen);
			io.printStackTrace();
		}

		char[] sBuffer = new char[brs_MSS];
		char[] mysBuffer = null;
		byte[] bBuffer = null;
		int numRead = 0;
		String eStr = new String ( EMPTY_STRING );
		byte[] payload_data = null;

		//Buffered Reader
		// cbuf - Destination buffer,
		// off - Offset at which to start storing characters,
		// len - Maximum number of characters to read
		//read (cbuf, off, len)
		int offset = 0;
		try {
			while (numRead != -1) {
				if ( ( numRead = inputReader.read(sBuffer, 0, brs_MSS) ) == brs_MSS ) {
					mysBuffer = new char[brs_MSS];
					System.arraycopy(sBuffer, 0, mysBuffer, 0, brs_MSS);
				}
				else if ( numRead < brs_MSS && numRead != -1 )  { //EOF not reached
					mysBuffer = new char[numRead];
					System.arraycopy(sBuffer, 0, mysBuffer, 0, numRead);
				}
				else if ( numRead == -1 ) {	//EOF reached
					System.out.println("EOF reached");
					//payload_data = eStr.getBytes();
					//close the local file
					inputReader.close();

					outd.flush();
					outd.close();

				}
				if (numRead != -1) {
				//	outd.print(mysBuffer);

					String myString = new String (mysBuffer);
					bBuffer = myString.getBytes();
					outd.write(bBuffer, 0, numRead);

				}
			} //while

		}
		catch (IOException io) {
			io.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.exit (1);

		//Testing
	}


}	// MySimWebServer