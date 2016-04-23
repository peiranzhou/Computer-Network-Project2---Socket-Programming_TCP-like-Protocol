import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;


public class sender {

	// For the sender, it should be invoked by inputing following arguments:
	// args0 - filename; args1 - remote_IP; args2 - remote_port; args3 - ack_port_num; arg4 - log_filename; arg5 - window_size
	
	public static void main(String[] args) throws EOFException, IOException, NoSuchAlgorithmException {
		
//		String[] args1 = new String[6];
//		args1[0] = "1.txt"; args1[1] = "127.0.0.1"; args1[2] = "8000"; args1[3] = "5000"; args1[4] = "sender_logfile.txt"; args1[5] = "1";
		
		// Initializing the Sender invoked arguments
		String filename = null; 
		InetAddress remote_IP;
		int remote_port;
		int ack_port_num;
		String log_filename = null;
		int window_size;
	
		filename = args[0];
		remote_IP = InetAddress.getByName(args[1]); // String to InetAddress
		remote_port = Integer.parseInt(args[2]);
		ack_port_num = Integer.parseInt(args[3]);
		log_filename = args[4];
		window_size = Integer.parseInt(args[5]);
		//----------------------------------------------------------------------
		int numberOfSegments = 0;
		int segmentSize = 576; // Each packet size
		long timeout = 5000;
		long estimatedRTT = 500;
        int totalByte = 0;
        int retransmitted = 0;
        int segmentsSent = 0;
		//----------------------------------------------------------------------
		
		
		// Read file into string, transfer string into byte
		String tempString = readFileIntoString(filename);		
		byte[] byteFile= tempString.getBytes();
		
		
		// Calculating number of packets will be sent
		numberOfSegments = numberOfSegments(byteFile, segmentSize);
		
		
		// Separate byteFile (data segment) into each packet, and I use ArrayList to store each packet (just data segment)
		ArrayList<byte[]> arrayListHasEachPacket = new ArrayList<byte[]>();
		if(byteFile.length % segmentSize == 0){
		    for(int loop2 = 0; loop2 < numberOfSegments; loop2++){
			    byte[] tempByte = new byte[576];
			    System.arraycopy(byteFile, loop2*576, tempByte, 0, 576);
			    arrayListHasEachPacket.add(tempByte);
		    }
		}
		else{
			int loop2 = 0;
		    for(loop2 = 0; loop2 < numberOfSegments-1; loop2++){
			    byte[] tempByte = new byte[576];
				System.arraycopy(byteFile, loop2*576, tempByte, 0, 576);
				arrayListHasEachPacket.add(tempByte);
			}
		    // Saving the last packet into ArrayList
		    int lastPacketLength = byteFile.length % segmentSize;
			byte[] tempByte2 = new byte[lastPacketLength];
		    System.arraycopy(byteFile, 576*(numberOfSegments-1), tempByte2, 0, lastPacketLength);
		    arrayListHasEachPacket.add(tempByte2);
		}
		
		
		
		// Add header file to each data segment, forming the integrated Packet
		int SourcePort = ack_port_num;
		int DestPort = remote_port;
		byte[] SourcePortByte = IntegerTo2Bytes(SourcePort);
		byte[] DestPortByte = IntegerTo2Bytes(DestPort);
		    
		int packetPLZLength = 20+arrayListHasEachPacket.get(numberOfSegments-1).length;
		
		// Finally, using ArrayList to store each Packet (Header + Data)
	    ArrayList<byte[]> packet = new ArrayList<byte[]>();
		    
		for(int k = 0; k < numberOfSegments; k++){
			
		  	byte[] packetPZ = new byte[596]; // Ceate byte array
		    byte[] packetPZL = new byte[packetPLZLength]; // Create byte array for the last packet
		    byte[] tempPZ1 = SourcePortByte; 
		    byte[] tempPZ2 = DestPortByte;
		    int sequenceNumber = k+1;
		    byte[] tempPZ3 = IntegerTo4Bytes(sequenceNumber);
			
		    // if - Last packet
	    	if(arrayListHasEachPacket.get(k).length != 576){
	    		System.arraycopy(tempPZ1, 0, packetPZL, 0, 2);  // Add SourcePort into file: 0 - 1 byte
			    System.arraycopy(tempPZ2, 0, packetPZL, 2, 2);  // Add DestPort into file: 2 - 3 byte
			    System.arraycopy(tempPZ3, 0, packetPZL, 4, 4);  // Add sequenceNumber into file: 4 - 7 byte
			    System.arraycopy(arrayListHasEachPacket.get(k), 0, packetPZL, 20, arrayListHasEachPacket.get(k).length); // Add data segment into packet
			    packetPZL[12] = 0b0001_0001;
			    
			    // Calculate checksum
			    int checksumInt2 = checksumCalculation(packetPZL);  
			    byte[] checksumByte = IntegerTo2Bytes(checksumInt2); // Transfer checksum to byte[]
			    System.arraycopy(checksumByte, 0, packetPZL, 16, 2);
			    packet.add(packetPZL);  // Add each packet to ArrayList	
		    }    
	    	// else - Packet except the last one
		    else{
		       System.arraycopy(tempPZ1, 0, packetPZ, 0, 2);  // Add SourcePort into file: 0 - 1 byte
		       System.arraycopy(tempPZ2, 0, packetPZ, 2, 2);  // Add DestPort into file: 2 - 3 byte
		       System.arraycopy(tempPZ3, 0, packetPZ, 4, 4);  // Add sequenceNumber into file: 4 - 7 byte
    	       System.arraycopy(arrayListHasEachPacket.get(k), 0, packetPZ, 20, arrayListHasEachPacket.get(k).length); // Add data segment into packet    
    	       
    	       // Calculate checksum
    	       int checksumInt = checksumCalculation(packetPZ);
		       byte[] checksumByte = IntegerTo2Bytes(checksumInt);
			   System.arraycopy(checksumByte, 0, packetPZ, 16, 2);			    
			   packet.add(packetPZ);  // Add each packet to ArrayList
		    }
		}

		// Create a socket waiting for the coming ACK
	    ServerSocket welcomeSocket = new ServerSocket(6789);
	    // Initializing UDP, Send
	 	DatagramSocket senderSocket = new DatagramSocket();
	    // Create a log file
		PrintWriter logfile = new PrintWriter(log_filename);	
			
		
		// ********* Send out eahc packet to Receiver *********
		for(int loop = 0; loop < numberOfSegments; loop++){
			
			DatagramPacket PacketIsSent;
			PacketIsSent = new DatagramPacket(packet.get(loop), packet.get(loop).length, remote_IP, remote_port);     
			senderSocket.send(PacketIsSent);
			segmentsSent = segmentsSent + 1;
			totalByte = totalByte + packet.get(loop).length;
			
			

            
    		
            // -------------------Create Logfile for Packet is Sent------------------------
			// timestamp
			long packetSentTime = System.currentTimeMillis();
			SimpleDateFormat simpleDateFormat_sent = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");    
			Date packetSentDate = new Date(packetSentTime);
			String timestamp_sent = simpleDateFormat_sent.format(packetSentDate);
			// source
			InetAddress localInternetAddress = InetAddress.getLocalHost();			
			// Write log into logfile
			if(loop == numberOfSegments - 1){
				StringBuilder logfileSentContent = WriteLogWhenSent(timestamp_sent, ack_port_num, localInternetAddress, remote_port, remote_IP, loop, 1);
				logfile.println(logfileSentContent);
			}else{
				StringBuilder logfileSentContent = WriteLogWhenSent(timestamp_sent, ack_port_num, localInternetAddress, remote_port, remote_IP, loop, 0);
				logfile.println(logfileSentContent);
			}
			
			if(logfile.equals("stdout")){
				System.out.println(logfile);
			}
			
			
			// Waiting for the feedback ack
			
			// Set timeout for welcomeSocket
	        welcomeSocket.setSoTimeout((int)timeout);
	        
			String clientACK;
	        while(true){
	        	try{
	        	Socket connectionSocket = welcomeSocket.accept();
	        	BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	        	clientACK = inFromClient.readLine();
	        	break;
	        	}
	        	catch (SocketTimeoutException s) {
	        		PacketIsSent = new DatagramPacket(packet.get(loop), packet.get(loop).length, remote_IP, remote_port);     
	    			senderSocket.send(PacketIsSent);
	    			
	    			retransmitted = retransmitted + 1; // Recording retransmitted packets
	    			totalByte = totalByte + packet.get(loop).length; // Recording the more part of packets data segment
	    			segmentsSent = segmentsSent + 1; // Recording the more part of segments are sent    			
	            }
	        }
			
	        
			// -------------------Create Logfile for Packet is Received------------------------
			// timestamp
			long ackReceivedTime = System.currentTimeMillis();
			SimpleDateFormat simpleDateFormat_ackReceive = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");    
			Date ackReceivedDate = new Date(ackReceivedTime);
			String timestamp_ackReceive = simpleDateFormat_ackReceive.format(ackReceivedDate);
			// Calculate RRT
			long devRTT = 0;
			long sampleRTT = ackReceivedTime - packetSentTime;
			estimatedRTT = (long) (0.875 * estimatedRTT + 0.125 * sampleRTT);
			devRTT = (long) (0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimatedRTT));
			timeout = estimatedRTT + 4 * devRTT;
			// Write log into logfile
			char muha = clientACK.charAt(5);
			if(muha == 'F'){
				StringBuilder logfileReicevedContent = WriteLogWhenReceiveACK(timestamp_ackReceive, ack_port_num, remote_IP, remote_port, localInternetAddress, loop, clientACK, 1, estimatedRTT);
				logfile.println(logfileReicevedContent);
			}else{
				StringBuilder logfileReicevedContent = WriteLogWhenReceiveACK(timestamp_ackReceive, ack_port_num, remote_IP, remote_port, localInternetAddress, loop, clientACK, 0, estimatedRTT);
				logfile.println(logfileReicevedContent);
			}
			
			if(logfile.equals("stdout")){
				System.out.println(logfile);
			}						
		}
              
		logfile.close();
		System.out.println("Delivery completed sucessfully");
//		totalByte = totalByte + byteFile.length;
		System.out.print("Total bytes sent = ");
		System.out.println(totalByte);
//		segmentsSent = segmentsSent + numberOfSegments;
		System.out.print("Segments Sent ＝ ");
		System.out.println(segmentsSent);
		System.out.print("Segments retransmitted = ");
		// Calculating the retransmitted percentage
		float ab;
		float ac;
		ab = retransmitted;
		ac = numberOfSegments;
		ab = (ab*100 / (ab + ac));
		System.out.print(ab);
		System.out.println(" %");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	// --------------------------------Methods Section--------------------------------
	
	
	
	public static int numberOfSegments(byte[] byteFile, int segmentSize){
		int numberOfSegments;
		if(byteFile.length % segmentSize == 0){
			numberOfSegments = byteFile.length / segmentSize;
		}
		else{
			numberOfSegments = byteFile.length / segmentSize + 1;
		}
		return numberOfSegments;
	}
	
	static byte[] IntegerTo4Bytes(int inputInteger){
		
		  byte[] ouputByte = new byte[4];

		  ouputByte[0] = (byte) (inputInteger >> 24);
		  ouputByte[1] = (byte) (inputInteger >> 16);
		  ouputByte[2] = (byte) (inputInteger >> 8);
		  ouputByte[3] = (byte) (inputInteger /*>> 0*/);

		  return ouputByte;
		  
	}
	
	static byte[] IntegerTo2Bytes(int inputInteger){
		
		  byte[] ouputByte = new byte[2];

		  ouputByte[0] = (byte) (inputInteger >> 8);
		  ouputByte[1] = (byte) (inputInteger /*>> 0*/);

		  return ouputByte;
		  
	}
	
	public static String readFileIntoString(String filename2) throws IOException{
	    String  thisLine = null;
		FileReader fileReader = new FileReader(filename2);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
				
		StringBuilder stringBuilder = new StringBuilder();
		        
		while((thisLine = bufferedReader.readLine()) !=null){
		     stringBuilder.append(thisLine);
//			 System.out.println(stringBuilder);
		}
		
		String tempString = stringBuilder.toString();
		return tempString;
	}
	
	//get checksum of each packet by MD5 encryption supported by java
    public static int checksumCalculation(byte[] DataNeedToCheck) throws IOException, NoSuchAlgorithmException {
        
    	byte[] newChecksumByte = new byte[4];
        MessageDigest variableMD = MessageDigest.getInstance("MD5");
        variableMD.update(DataNeedToCheck);
        byte[] zprMD = variableMD.digest();
        System.arraycopy(zprMD, 0, newChecksumByte, 2, 2);
	    ByteArrayInputStream a = new ByteArrayInputStream(newChecksumByte);
        DataInputStream b = new DataInputStream(a);
        int checksumValue = b.readInt();
        return checksumValue;
    }
	
    public static StringBuilder WriteLogWhenSent(String timestamp_sent, int sourcePort, InetAddress localInternetAddress, int destPort, InetAddress remote_IP, int loop, int FIN){
		StringBuilder logfileSentContent = new StringBuilder();
		logfileSentContent.append("Timestamp - Sent: ");
		logfileSentContent.append(timestamp_sent);
		logfileSentContent.append("; ");
		
		logfileSentContent.append("Source Port: ");
		logfileSentContent.append(sourcePort);
		logfileSentContent.append("; ");
		
		logfileSentContent.append("Source: ");
		logfileSentContent.append(localInternetAddress.toString());
		logfileSentContent.append("; ");
		
		logfileSentContent.append("Destination Port: ");
		logfileSentContent.append(destPort);
		logfileSentContent.append("; ");
		
		logfileSentContent.append("Destination: ");
		logfileSentContent.append(remote_IP.toString());
		logfileSentContent.append("; ");
		
		logfileSentContent.append("Sequence NO. ");
		logfileSentContent.append(loop+1);
		logfileSentContent.append("; ");
		
		logfileSentContent.append("FIN Value: ");
		logfileSentContent.append(FIN);
		logfileSentContent.append("; ");
		
		return logfileSentContent;
		
		}
	
    public static StringBuilder WriteLogWhenReceiveACK(String timestamp_ackReceive, int sourcePort, InetAddress remote_IP, int destPort, InetAddress localInternetAddress, int loop, String clientACK, int FIN, long estimatedRTT){
		StringBuilder logfileReicevedContent = new StringBuilder();
		logfileReicevedContent.append("Timestamp - ACK - Reiceve: ");
		logfileReicevedContent.append(timestamp_ackReceive);
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("Source Port: ");
		logfileReicevedContent.append(sourcePort);
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("Source IP: ");
		logfileReicevedContent.append(remote_IP);
		logfileReicevedContent.append("; ");

		logfileReicevedContent.append("Destination Port: ");
		logfileReicevedContent.append(destPort);
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("Destination: ");
		logfileReicevedContent.append(localInternetAddress.toString());
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("Sequence NO.: ");
		logfileReicevedContent.append(loop+1);
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("ACK NO. ");
		logfileReicevedContent.append(clientACK.charAt(5));
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("FIN Value: ");
		logfileReicevedContent.append(FIN);
		logfileReicevedContent.append("; ");
		
		logfileReicevedContent.append("Estimated RTT: ");
		logfileReicevedContent.append(Long.toString(estimatedRTT));
		logfileReicevedContent.append("; ");
		
		return logfileReicevedContent;
		
		}

}
