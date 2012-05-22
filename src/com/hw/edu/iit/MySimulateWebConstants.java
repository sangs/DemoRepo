/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.hw.edu.iit;

/**
 *
 * @author Sangeetha Ramadurai
 */
public interface MySimulateWebConstants {

	public final static String USAGE_MESSAGE = "MySimWebServer -P <port number - any number between 1024 to 65536>";

	public final static String INVALID_PORT_NUMBER = "Invalid port number. Please enter a number between 1024 to 65536";

	public final static int CLIENT_SOURCE_PORT = 4022;

	public final static String EMPTY_STRING = "EMPTY";
	public final static String CONFIG_FILE_LOCATION = "C:\\CS542\\Sangeetha\\batch\\";
	public final static String CONFIG_FILE_NAME = "MyWebSim.config.txt";
	public final static String CLIENTLOCATION = "CLIENT_LOCATION";
	public final static String SERVERLOCATION = "SERVER_LOCATION";
	public final static String BROWSERTYPE = "BROWSER_TYPE";
	public final static String MSSVALUE = "MSS_VALUE";
	public final static String NUMOBJREQUEST = "NUMBER_OF_OBJECTS_REQUESTED";
	public final static String ERRSEGMENTNUMBER = "ERROR_SEGMENT_NUMBER";

	public final static String CLIENT_A_DATAFILE = "Client-a.cs.iit.edu.html";
	public final static String CLIENT_B_DATAFILE = "Client-b.cs.iit.edu.html";
	public final static String CLIENT_A_ZTCP_FILE = "ca.zTCP.txt";
	public final static String CLIENT_B_ZTCP_FILE = "cb.zTCP.txt";
	public final static String BEGIN_RECEIVE_WINDOW_COMMENT = "******** --Begin Receive Window-- ********";
	public final static String END_RECEIVE_WINDOW_COMMENT = "******** --End Receive Window-- ********";
	public final static String BEGIN_OF_SEGMENT = "#### Begin zTCP segment ####";
	public final static String END_OF_SEGMENT = "#### End zTCP segment ####";

	public final static String OBJECT_BEGIN_SEPERATOR = " #### BEGIN OF OBJECT ";
	public final static String OBJECT_END_SEPERATOR = " #### END OF OBJECT ";
	public final static String OBJECT_TAIL = " #### ";
	public final static String REQUEST_FOR_OBJ1 = "OBJECT_ONE_NAME";
	public final static String REQUEST_FOR_OBJ2 = "OBJECT_TWO_NAME";
	public final static String REQUEST_FOR_OBJ3 = "OBJECT_THREE_NAME";

	//Printable zTCP Segment format
	public final static String SOURCEPORT_NAME = "Source Port";
	public final static String DESTINATIONPORT_NAME = "Destination Port";
	public final static String SEGMENTNUMBER_NAME = "Segment Number";
	public final static String ACKNUMBER_NAME = "ACK Number";
	public final static String ACKBIT_NAME = "ACK Bit"; //binary value
	public final static String SYNBIT_NAME = "SYN Bit"; //binary value
	public final static String FINBIT_NAME = "FIN Bit"; //binary value
	public final static String CHECKSUM_NAME = "Checksum";
	public final static String MSS_NAME = "Option";
	public final static String PAYLOAD_NAME = "Payload Data"; //(data file contents)


}

