import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.text.*;

public class receiver {
	
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		// Input variable initialization
//		String[] args1 = new String[6];
//		args1[0] = "2.txt"; args1[1] = "8000"; args1[2] = "127.0.0.1"; args1[3] = "5000"; args1[4] = "receiver_logfile.txt";
		
		String filename = null; 
		int listening_port;
		InetAddress sender_IP = null;
		int sender_port;
		String log_filename = null;

		filename = args[0];
		listening_port = Integer.parseInt(args[1]);
		sender_IP = InetAddress.getByName(args[2]);
		sender_port = Integer.parseInt(args[3]);
		log_filename = args[4];
		// --------------------------------------------------------
		
		// Initialize UDP to receive data
		DatagramSocket receiverSocket = new DatagramSocket(listening_port); // Initialize a lisenting port 
        
		int seqNumberRecord = 0;
		
		// When receiving packet from sender, creating log file
	    PrintWriter logfile = new PrintWriter(log_filename);
		PrintWriter reconstructFile = new PrintWriter(filename);	
	    
		int count = 0;
		
		while(true){
		count++;
		// Receiving packet and saving into packet byte[]
        byte[] SegmentFromSender = new byte[596];
        DatagramPacket senderSegment = new DatagramPacket(SegmentFromSender, SegmentFromSender.length);
        receiverSocket.receive(senderSegment);
        byte[] packet = senderSegment.getData(); 
		
        // lastPacketDataLength will be used only when " packet[12]&1 == 1 "
        int lastPacketDataLength = senderSegment.getLength() - 20;
        
        // ------------------------- Write information into log file -------------------------
		// timestamp
	    long packetReceivedTime = System.currentTimeMillis();
		SimpleDateFormat simpleDateFormat_receive = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");    
	    Date packetSentDate = new Date(packetReceivedTime);
		String timestamp_receive = simpleDateFormat_receive.format(packetSentDate);
		// Destination
		InetAddress localInternetAddress = InetAddress.getLocalHost();
		// Sequence number (Convert byte[] to Integer)
		byte[] seqNumber = new byte[4];
		System.arraycopy(packet, 4, seqNumber, 0, 4);
		int seqNumberInt = ByteToInteger(seqNumber);
        String seqNumberString = String.valueOf(seqNumberInt);
        
		// Write receive-log into logfile
        if((packet[12]&1) == 1){
        	StringBuilder logfileReceivedContent = WriteLogWhenReceivePKT(timestamp_receive, sender_port, sender_IP, listening_port, localInternetAddress, seqNumberString, 1);
    		logfile.println(logfileReceivedContent);
        }else{
        	StringBuilder logfileReceivedContent = WriteLogWhenReceivePKT(timestamp_receive, sender_port, sender_IP, listening_port, localInternetAddress, seqNumberString, 0);
    		logfile.println(logfileReceivedContent);
        }
        
        		
		if(logfile.equals("stdout")){
			System.out.println(logfile);
		}
		// ------------------------- Finish write the log file when receive a packet -------------------------
		
		// ******************** Deal with the received packet ********************
		
		// -------------If checksum is correct, do the following things: Reconstruct file, Send ACK back,  
		
		int checksum2 = 0;
		int orgChecksumInt = 0;
		
		// Take out the original checksum, and recalculate the packet checksum
		if((packet[12]&1) == 1){
			
			byte[] reconstructLastData2 = new byte[lastPacketDataLength+20];
			System.arraycopy(packet, 0, reconstructLastData2, 0, lastPacketDataLength+20);
			
			byte[] originalChecksum2 = new byte[4];
			System.arraycopy(reconstructLastData2, 16, originalChecksum2, 2, 2);
			reconstructLastData2[16] = 0;
			reconstructLastData2[17] = 0;
			
			checksum2 = checksumCalculation(reconstructLastData2);
			orgChecksumInt = ByteToInteger(originalChecksum2);
//			System.out.println(orgChecksumInt);
//			System.out.println(checksum2);
		}else{		
			byte[] originalChecksum = new byte[4];
			System.arraycopy(packet, 16, originalChecksum, 2, 2);
			packet[16] = 0;
			packet[17] = 0;
			
			checksum2 = checksumCalculation(packet);
			orgChecksumInt = ByteToInteger(originalChecksum);
//			System.out.println(orgChecksumInt);
//			System.out.println(checksum2);
		}
		
		if(orgChecksumInt == checksum2){
			
			
		    if(seqNumberInt != seqNumberRecord){
		    seqNumberRecord = seqNumberInt;
			
			int ackNumberInt = seqNumberInt;
			String ackNumberString = String.valueOf(ackNumberInt);
			String ack = "ACK" + ackNumberString;
			
			
			// If FIN = 1
			if((packet[12]&1) == 1){
				byte[] reconstructLastData = new byte[lastPacketDataLength];
				System.arraycopy(packet, 20, reconstructLastData, 0, lastPacketDataLength);
				String reconstructLastDataStr = new String(reconstructLastData);
				reconstructFile.print(reconstructLastDataStr);
				ackNumberString = String.valueOf(count);
				ack = "ACKFIN";
			}
			else{
				byte[] reconstructDataByte = new byte[576];
				System.arraycopy(packet, 20, reconstructDataByte, 0, 576);
				String reconstructDataString = new String(reconstructDataByte);
				reconstructFile.print(reconstructDataString);
			}
		    
					
		Socket clientSocket = new Socket(args[2], 6789);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	    outToServer.writeUTF(ack);
        clientSocket.close();
        
        
        // write send-log into logfile
        // ack - timestamp
	    long ackSentTime = System.currentTimeMillis();
		SimpleDateFormat simpleDateFormat_ackSent = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");    
	    Date ackSentDate = new Date(ackSentTime);
		String timestamp_ackSent = simpleDateFormat_ackSent.format(ackSentDate);		
		// Write receive-log into logfile
        if((packet[12]&1) == 1){
        	StringBuilder logfileACKSentContent = WriteLogWhenSendACK(timestamp_ackSent, sender_port, sender_IP, listening_port, localInternetAddress, ackNumberString, 1);
    		logfile.println(logfileACKSentContent);
        }
        else{
        	StringBuilder logfileACKSentContent = WriteLogWhenSendACK(timestamp_ackSent, sender_port, sender_IP, listening_port, localInternetAddress, ackNumberString, 0);
    		logfile.println(logfileACKSentContent);
        }
		
		
		if(logfile.equals("stdout")){
			System.out.println(logfile);
		}
        
		}
        
		    
		// Very Last Part
		if((packet[12]&1) == 1){
			break;
		}
		
		}		    
		    
		}
		
		reconstructFile.close();
		logfile.close();
		
		System.out.println("Delivery completed successfully");
		receiverSocket.close();
		
	}
	
	
	
	// --------------------------------Method Section---------------------------------------------------
	
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
    
    public static int ByteToInteger(byte[] seqNumber) throws IOException{
		int seqNumberInt = 0;
	    ByteArrayInputStream jixin = new ByteArrayInputStream(seqNumber);
        DataInputStream djixin = new DataInputStream(jixin);
        seqNumberInt = djixin.readInt();	
        return seqNumberInt;
	}
	
    public static StringBuilder WriteLogWhenReceivePKT(String timestamp_receive, int sourcePort, InetAddress sender_IP, int destPort, InetAddress localInternetAddress, String seqNumberString, int FIN){
    	
		StringBuilder logfileReceivedContent = new StringBuilder();
		logfileReceivedContent.append("Timestamp - Received: ");
		logfileReceivedContent.append(timestamp_receive);
		logfileReceivedContent.append("; ");
		
		logfileReceivedContent.append("Source Port: ");
		logfileReceivedContent.append(sourcePort);
		logfileReceivedContent.append("; ");
		
		logfileReceivedContent.append("Source IP: ");
		logfileReceivedContent.append(sender_IP.toString());
		logfileReceivedContent.append("; ");
		
		logfileReceivedContent.append("Destination Port: ");
		logfileReceivedContent.append(destPort);
		logfileReceivedContent.append("; ");
		
		logfileReceivedContent.append("Destination IP: ");
		logfileReceivedContent.append(localInternetAddress.toString());
		logfileReceivedContent.append("; ");
		
		logfileReceivedContent.append("Sequence NO. ");
		logfileReceivedContent.append(seqNumberString);
		logfileReceivedContent.append("; ");	
		
		logfileReceivedContent.append("FIN Value: ");
		logfileReceivedContent.append(FIN);
		logfileReceivedContent.append("; ");
		
		return logfileReceivedContent;
		
        }

    public static StringBuilder WriteLogWhenSendACK(String timestamp_ackSent, int sourcePort, InetAddress sender_IP, int destPort, InetAddress localInternetAddress, String ackNumberString, int FIN){
		StringBuilder logfileACKSentContent = new StringBuilder();
		logfileACKSentContent.append("Timestamp - SentACK: ");
		logfileACKSentContent.append(timestamp_ackSent);
		logfileACKSentContent.append("; ");
		
		logfileACKSentContent.append("Source Port: ");
		logfileACKSentContent.append(sourcePort);
		logfileACKSentContent.append("; ");
		
		logfileACKSentContent.append("Source IP: ");
		logfileACKSentContent.append(sender_IP.toString());
		logfileACKSentContent.append("; ");
		
		logfileACKSentContent.append("Destination Port: ");
		logfileACKSentContent.append(destPort);
		logfileACKSentContent.append("; ");
		
		logfileACKSentContent.append("Destination IP: ");
		logfileACKSentContent.append(localInternetAddress.toString());
		logfileACKSentContent.append("; ");
		
		logfileACKSentContent.append("ACK NO. ");
		logfileACKSentContent.append(ackNumberString);
		logfileACKSentContent.append("; ");		
		
		logfileACKSentContent.append("FIN Value: ");
		logfileACKSentContent.append(FIN);
		logfileACKSentContent.append("; ");	
		
		return logfileACKSentContent;
		
		}
    
    
}
