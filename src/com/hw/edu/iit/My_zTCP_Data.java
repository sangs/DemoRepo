/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.hw.edu.iit;

/**
 *
 * @author Sangeetha Ramadurai
 */
import java.io.Serializable;
import java.io.*;
import static com.hw.edu.iit.MySimulateWebConstants.*;

public class My_zTCP_Data implements Serializable {

	private int SYNBit;
	private int ACKNumber;
	private int SourcePort;
	private int DestinationPort;
	private int SequenceNumber;
	private int ACKBit;
	private int FINBit;
	private int Option;				 //MSS stored in Option
	private byte[] Payload;
	//private char[] Payload;

	private String check_sum;

	public My_zTCP_Data () {
		super ();
		String inpstring = new String( EMPTY_STRING );
		setSynBit(-1);
		setACKNumber(-1);
		setSourcePort(0);
		setDestinationPort(0);
		setSequenceNumber(-1);
		setACKBit(-1);
		setFinBit(-1);
		setMSS(1);
		setPayload (inpstring.getBytes());

		setCheckSum("");
	}



	///////////////// Getters /////////////////////
	final boolean isSynBitSet () {
		return ( SYNBit == 1) ? true : false;
	}

	final boolean isFinBitSet () {
		return ( FINBit == 1 ) ? true : false;
	}

	final boolean isACKBitSet () {
		return ( ACKBit == 1) ? true : false;
	}

	final int getMSS () {
		return Option;
	}

	final int getSourcePort () {
		return SourcePort;
	}

	final int getDestinationPort () {
		return DestinationPort;
	}

	final int getSequenceNumber () {
		return SequenceNumber;
	}

	final int getACKNumber () {
		return ACKNumber;
	}

	final byte[] getPayload () {
		return Payload;
	}

/*	final String getPayload () {
		return Payload;
	}
	*/
	final String getCheckSum () {
		return check_sum;
	}

	//////////////// Setters //////////////
	final void setSynBit (int isSYN) {
		SYNBit = isSYN;
	}

	final void setFinBit (int isFIN) {
		FINBit = isFIN;
	}

	final void setACKBit (int isACK) {
		ACKBit = isACK;
	}

	final void setMSS (int mss) {
		if (mss <= 0)
			throw new IllegalArgumentException ("Invalid Maximum Segment Size");

		Option = mss;
	}

	final void setSourcePort (int sPort) {
		SourcePort = sPort;
	}

	final void setDestinationPort (int dPort) {
		DestinationPort = dPort;
	}

	final void setSequenceNumber (int seqNum) {
		SequenceNumber = seqNum;
	}

	final void setACKNumber (int ackNum) {
		ACKNumber = ackNum;
	}

	final void setPayload (byte[] payloadData) {
		if (payloadData == null)
			throw new IllegalArgumentException("PayLoad has invalid size");
		int sz = payloadData.length;
		Payload = new byte[ sz ];
		System.arraycopy(payloadData, 0, Payload, 0, sz);
	}

/*	final void setPayload (char[] payloadData) {
		if (payloadData == null)
			throw new IllegalArgumentException ("Invalid Payload");
		PayloadStr = payloadData;
	}
	*/


	//TODO: This is temp, remove this to HashMap later
	final void setCheckSum (String checkSum) {
		check_sum = checkSum;
	}


	static final long serialVersionUID = -1028098204615850500L;


	//Implement Serializable
	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		//always perform the default de-serialization first
		aInputStream.defaultReadObject();

//		Manually De-serialize My_zTCP_Data object contents
		//SYNBit
		Integer synObject = (Integer) aInputStream.readObject();
		int syn = synObject.intValue();
		setSynBit ( syn );

		//FINBit
		Integer finObject = (Integer) aInputStream.readObject();
		int fin = finObject.intValue();
		setFinBit ( fin );

		//ACKBit
		Integer ackBitObject = (Integer) aInputStream.readObject();
		int ack = ackBitObject.intValue();
		setACKBit ( ack );

		//MSS (Option)
		Integer mssObject = (Integer) aInputStream.readObject();
		int mss = mssObject.intValue();
		setMSS ( mss );

		//SourcePort
		Integer sPortObject = (Integer) aInputStream.readObject();
		int sPort = sPortObject.intValue();
		setSourcePort ( sPort );

		//DestinationPort
		Integer dPortObject = (Integer) aInputStream.readObject();
		int dPort = dPortObject.intValue();
		setDestinationPort ( dPort );

		//SequenceNumber
		Integer sqNumObject = (Integer) aInputStream.readObject();
		int sqNum = sqNumObject.intValue();
		setSequenceNumber ( sqNum );

		//ACKNumber
		Integer akNumObject = (Integer) aInputStream.readObject();
		int akNum = akNumObject.intValue();
		setACKNumber ( akNum );

		//Payload
		//DataInputStream ploadStream = new DataInputStream ( aInputStream );

		/*String ploadObject = (String) aInputStream.readObject();
		int sz = ploadObject.length();
		byte[] pLoad = new byte[sz];		//new byte [mss];
		pLoad = ploadObject.getBytes();
		setPayload ( pLoad );
		*/

		int sz = aInputStream.readInt();
		byte[] pLoad = new byte[sz];
		aInputStream.readFully( pLoad, 0, sz );
		setPayload ( pLoad );

		//ensure that object state has not been corrupted or tampered with maliciously
		//validateState();
	}
	/**
	* This is the default implementation of writeObject.
	* Customise if necessary.
	*/
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		//perform the default serialization for all non-transient, non-static fields
		aOutputStream.defaultWriteObject();

		//Manually serialize My_zTCP_Data object contents
		//SYNBit
		int si = ( isSynBitSet() ) ? 1 : 0;
		Integer synObject = new Integer ( si );
		aOutputStream.writeObject( synObject );

		//FINBit
		int fi = ( isFinBitSet() ) ? 1 : 0;
		Integer finObject = new Integer ( fi );
		aOutputStream.writeObject( finObject );

		//ACKBit
		int ack = ( isACKBitSet() ) ? 1 : 0;
		Integer ackBitObject = new Integer ( ack );
		aOutputStream.writeObject( ackBitObject );

		//MSS (Option)
		int mss = getMSS ();
		Integer mssObject = new Integer ( mss );
		aOutputStream.writeObject( mssObject );

		//SourcePort
		int sPort = getSourcePort ();
		Integer sPortObject = new Integer ( sPort );
		aOutputStream.writeObject( sPortObject );

		//DestinationPort
		int dPort = getDestinationPort ();
		Integer dPortObject = new Integer ( dPort );
		aOutputStream.writeObject( dPortObject );

		//SequenceNumber
		int sqNum = getSequenceNumber ();
		Integer sqNumObject = new Integer (sqNum);
		aOutputStream.writeObject( sqNumObject );

		//ACKNumber
		int akNum = getACKNumber ();
		Integer akNumObject = new Integer (akNum);
		aOutputStream.writeObject( akNumObject );

		//Payload
		byte[] pLoad = getPayload ();
//		ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		int sz = pLoad.length;
		if (sz <= 0) {
			System.out.println ("Invalid Payload. PayLoad data should be atleast one character long");
		}
	//	baos.write(pLoad, 0, sz-1);
		aOutputStream.writeInt( sz );
		aOutputStream.write( pLoad );

	}


}	//My_zTCP_Data
