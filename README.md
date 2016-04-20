# Computer-Network-Project2---Socket-Programming_TCP-like-Protocol

—————————————————————————————————————————————————————————————————————————
(a) A brief description of TCP-like protocol

I implemented a TCP-like protocol using Java. The sender and receiver main files called “sender.java” and “receiver.java”.

Files included in submitted folder - “pz2210_java”:
sender.java  receiver.java  README.txt  makefile  sender_logfile.txt  recerver_logfile.txt
original_file.txt  reconstruct_file.txt

—————————————————————————————————————————————————————————————————————————
(b) The TCP segment structure I used

b.1> The header file (20 Bytes) of TCP includes: Source Port, Destination Port, Sequence Number, Acknowledgement Number, Flags, Checksum.

b.2> The data segment of TCP is 576 Bytes.

              —————————————————————————————————————————————————————
              |Source port# (2 Bytes) | Destination port# (2 Bytes)|
              —————————————————————————————————————————————————————
              |             Sequence Number (4 Byte)               |
              —————————————————————————————————————————————————————
              |           Acknowledgment Number (4 Byte)           |
              —————————————————————————————————————————————————————
              |             Flags (4 Byte) - FIN(1 bit)            |
              —————————————————————————————————————————————————————
              |     Checksum(2 Bytes)    |    Unused (2 Bytes)     |
              —————————————————————————————————————————————————————
              |                 Data Segment (576 Bytes)           |
              —————————————————————————————————————————————————————

—————————————————————————————————————————————————————————————————————————
(c) Instruction of running the program

*************** Test on Localhost ***************

I strongly suggest to test the program on localhost firstly, making sure: 

1. the program can compile successfully, 
2. sender & receiver can be invoked in given format, 
3. statistics on sender can be printed properly, 
4. “Log File” relevant function are correct.

Running code direction:

1. cd to “pz2210_java” directory.

2. Run the make file:
2.1> $ make clean
2.2> $ make

3. Run the receiver & receiver by using following command:

3.1 IPV4 Localhost:

$ java receiver reconstruct_file.txt 8000 127.0.0.1 5000 recerver_logfile.txt

$ java sender original_file.txt 127.0.0.1 8000 5000 sender_logfile.txt 1

3.2 IPV6 Localhost:

$ java receiver reconstruct_file.txt 8000 ::1 5000 recerver_logfile.txt

$ java sender original_file.txt ::1 8000 5000 sender_logfile.txt 1


* For the domain name, you can just change the “127.0.0.1” to “localhost”.


*************** Test with Newudpl Proxy ***************

1. cd to the Newudpl Proxy directory, starting the proxy.

2. Then, cd to “pz2210_java” directory.

3. Run the make file:
3.1> $ make clean
3.2> $ make

4. Run the receiver by using following command:

$ java receiver reconstruct_file.txt 8000 160.39.138.66 41193 recerver_logfile.txt

5. Run the sender by using following command:

$ java sender original_file.txt 160.39.138.66 41192 5000 sender_logfile.txt 1

—————————————————————————————————————————————————————————————————————————
(d) For the recovery relevant function:

My TCP-like protocol can recover from in-network packet loss, packet corruption, packet duplication, packet reordering and dynamic network delay.

—————————————————————————————————————————————————————————————————————————
(e) Unusual about implementation

My protocol is pretty generic, there is no strange or unusual bug in my program. 

* But for the log file, during one packet communication (send packet - receive ACK), to make the log records more accurate, I set both sender and receiver have two log records. 
For sender: Packet Sent Log & ACK Receive Log. 
For receiver: Packet Receive Log & ACK Sent Log

* Also, for the log record’s source and destination, I recorded both the IP and port number, so that user could be easier to check out the log.

* For the last ACK, I set it to a different value, so that the sender could know whether or not the ACK is the last one. Pretty tricky. 

