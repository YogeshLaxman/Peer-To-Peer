import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
public class FileHandler {
    final static String FILE_NAME = "text.pdf";
	public static boolean validateArguments(String[] args)
	{	
		return true;	
	}
    public static void main(String[] args) throws Exception {

		if(args.length != 1) {
    		System.out.println("Invalid input");
    	}
		else
		{	
			int sPort = Integer.valueOf(args[0]);
			System.out.println("File Owner started.. \nListening to port number:");
			System.out.println(sPort);
    	    String currentDir = System.getProperty("user.dir");
			int numberOfFiles =splitFile(new File(currentDir + "\\"+ FILE_NAME ));
			System.out.println("Number of chunks formed from file ["+FILE_NAME+"] :"+numberOfFiles);
			System.out.println("Server started..\nWaiting for client connection...\n"); 
				// Listening on socket sPort
					ServerSocket listener = new ServerSocket(sPort);
					int clientNum = 1;// Change back to one
					try {
							while(true) {
									
									new Handler(listener.accept(),clientNum).start();
									System.out.println("Peer "  + clientNum + " is connected!");
									clientNum++;
								}
					}catch(Exception e)
					{
						System.out.println("Enter valid IP and Port");
					}
					finally {
							listener.close();
					} 
		}
    }

		public static int splitFile(File f) throws IOException {
			int chunks = 0;
			System.out.println("Splitting file..");
	        int chunkno = 1;
	        //int sizeOfFiles = 1024 * 100;// 1MB
	        byte[] buffer = new byte[1024 * 100];
	        // Create new directory for files split
	        Path path = Paths.get("Server_file_split");
	        
	        if (!Files.exists(path)) {
	            try {
	                Files.createDirectories(path);
	            } catch (IOException e) {
	                // err creating dir
	                e.printStackTrace();
	            }
	        }

	        String fileName = f.getName();

	        //try-with-resources to ensure closing stream
	        try (FileInputStream fis = new FileInputStream(f);
	             BufferedInputStream bis = new BufferedInputStream(fis)) {

	            int bytesAmount = 0;
	            while ((bytesAmount = bis.read(buffer)) > 0) {
	                //write each chunk of data into separate file with different number in name
	                String filePartName = String.format("%s.%03d", fileName, chunkno++);
	               // File newFile = new File(f.getParent(), filePartName);
	                // File newFile = new File(path.toString(), filePartName);
	                try (FileOutputStream out = new FileOutputStream(new File(path.toString(), filePartName))) {
	                    out.write(buffer, 0, bytesAmount);
	                }
	                chunks++;
	            }
	        }
			return chunks;
		
	}
		private static class Handler extends Thread {
    		private Socket connection;
            private ObjectInputStream myInputStream;
            private ObjectOutputStream myOutputStream;
            private int no;
            private String message;
            public int peerPortNumber;
            
            public Handler(Socket connection, int no) {
            	this.connection = connection;
                this.no = no;
                
            }

            public void run() {
                try{
                  	myOutputStream = new ObjectOutputStream(connection.getOutputStream());
        			myOutputStream.flush();
        			myInputStream = new ObjectInputStream(connection.getInputStream());
        			
        			try{
        				while(true)
        				{
        					System.out.println("Listening to input from Peer(s)...");
        					message = (String)myInputStream.readObject();
        					//show the message to the user
        					//System.out.println("\nReceived request: [" + message + "] from Peer " + no+ "on Port ["+ peerPortNumber+"]");
        					processClientRequest(message);
        					
        				}
        			}
        			catch(ClassNotFoundException classnot){
        					System.err.println("Data received in unknown format");
        				}
                }
        		catch(IOException ioException){
        			System.out.println("Disconnect with Client " + no);
        		}
            }
            private void processClientRequest(String input) throws IOException, ClassNotFoundException {
       		 String[] commandPart = input.split(" ");
       		 switch(commandPart[1]) {
       		 case "start":
       			 this.peerPortNumber = Integer.valueOf(commandPart[0]);
       			 System.out.println("\nReceived request: [" + input + "] from Peer " + no+ " on Port ["+ peerPortNumber+"]");
       		     sendOutputToClient(documentsInServer());
       			 break;
       		 case "get":
       			 System.out.println("\nReceived request: [" + input + "] from Peer " + no+ " on Port ["+ peerPortNumber+"]");
       			 writeFileToOutputStram(commandPart[0]);
       			 System.out.println("Chunk ["+commandPart[0]+"] sent to Peer: ["+ peerPortNumber+"]");
       			 break;
       		 case "getList":
       			 System.out.println("\nReceived request: [" + input + "] from Peer " + no+ " on Port ["+ peerPortNumber+"]");
       			sendOutputToClient(documentsInServer());
       			 break;
       		 default:
       			System.out.println("Operation not supported");
       			break;
       		 }	
       	}

            private void writeFileToOutputStram(String document) {
        		try {
        			String currentDir = System.getProperty("user.dir");
     				File myDocument = new File(currentDir + "\\Server_file_split\\" + document);
        			InputStream is = new FileInputStream(myDocument); 	
        	        sendOutputToClient(is.readAllBytes());
        	        is.close();
        		} catch(Exception e) {
        			sendOutputToClient("Error while getting the file");
        		}
            }

			private String documentsInServer() throws IOException{
				 String currentDir = System.getProperty("user.dir");
		    File myServerDirectory = new File(currentDir + "\\Server_file_split");
		       File[] myDocumentList = myServerDirectory.listFiles();
		       String output = "";
		       
		       for(int i=0; i<myDocumentList.length ; i++)
		       {
		       	if(myDocumentList[i].isFile())
		       	{
		       		output = output.concat(myDocumentList[i].getName()) + " ";
		       	}
		       }
		       System.out.println("[Sending chunk list] to Peer: ["+ peerPortNumber +"]");
		       return output;
			}
     
			void sendOutputToClient(Object clientOutputSend) {
				try {
					myOutputStream.writeObject(clientOutputSend);
					myOutputStream.flush();
				} catch (IOException e) {
					System.out.println("Error while writing to output stream");
					e.printStackTrace();
				}
			}
        }
}