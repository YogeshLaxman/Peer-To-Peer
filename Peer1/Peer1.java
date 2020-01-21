import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Peer1 {

    int myPort;
    // Defining global variables
    public static int TotalNumberOfFiles = 0;
    public static boolean isFileMerged = false;

    public static boolean ArgumentsNotValid(String[] arg) {
        return false;
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
        if(args.length != 3) {
    		System.out.println("Invalid input");
    	}
        else{
            if (!ArgumentsNotValid(args)) {
            int FOPort = Integer.valueOf(args[0]);
            int listPort = Integer.valueOf(args[1]);
            int downPort = Integer.valueOf(args[2]);
            Peer1 peer = new Peer1();
            peer.run(FOPort, listPort, downPort);
            }
        }
    }

    private void run(int FOPort, int upPort, int downPort) throws InterruptedException {
        int port;
        Path path = Paths.get("temp");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                // err creating dir
                e.printStackTrace();
            }
        }
        this.myPort = upPort;

        ///************ Initializing different threads for uploads and downloads *********

        new DownloadFromPort(FOPort).start();
        new DownloadFromPeerPort(downPort, upPort).start();
        new UploadToPort(upPort).start();
        Thread.sleep(500);
        System.out.println("Total files: " + TotalNumberOfFiles);
        
        String currentDir = System.getProperty("user.dir");
            File myServerDirectory = new File(currentDir + "\\temp");
            File[] myDocumentList = myServerDirectory.listFiles();
            if(myDocumentList.length > 1)
            {
                new MergeRestFilesWhenComplete(TotalNumberOfFiles).start();
            }
        


    }

    private class MergeRestFilesWhenComplete extends Thread {
        int TotalNumberOfChunks;

        public MergeRestFilesWhenComplete(int totalFile) {
            TotalNumberOfChunks = totalFile;
        }

        public void run() {
            //Check if temp folder has all the files
            boolean allFilesPresent = false;
            while (!allFilesPresent) {
                String filesInTemp = documentsInMyFolder();
                String[] listOfFilesInTemp = filesInTemp.split(" ");
                //System.out.println("Total files present in temp" + listOfFilesInTemp.length);
                if (listOfFilesInTemp.length >= TotalNumberOfChunks) {
                    //Merge function
                    System.out.println("################################### Starting merge function ##############################");
                    String currentDir = System.getProperty("user.dir");
                    String fileName = listOfFilesInTemp[0].substring(0, listOfFilesInTemp[0].length() - 4);
                    System.out.println("Making final file with fileName: " + fileName);
                    try {
                        mergeFiles(new File(currentDir + "\\temp\\" + listOfFilesInTemp[0]), new File(currentDir + "\\" + fileName));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    allFilesPresent = true;
                    //Setting global variable for file merged to true for killing download processes
                    isFileMerged = true;
                }

            }
        }


        public void mergeFiles(List<File> files, File into)
                throws IOException {
            try {
                boolean allGreaterThanZero = false;
                //
                while (!allGreaterThanZero) {
                    boolean looped = false;
                    for (File file : files) {
                        if (file.length() == 0) looped = true;
                    }

                    allGreaterThanZero = !looped;
                }
                //FileOutputStream fos = new FileOutputStream(into);
                BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into));
                for (File f : files) {
                    Files.copy(f.toPath(), mergingStream);
                }
                mergingStream.flush();
                mergingStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public List<File> listOfFilesToMerge(File oneOfFiles) {
            String tmpName = oneOfFiles.getName();//{name}.{number}
            String destFileName = tmpName.substring(0, tmpName.lastIndexOf('.'));//remove .{number}
            File[] files = oneOfFiles.getParentFile().listFiles(
                    (File dir, String name) -> name.matches(destFileName + "[.]\\d+"));
            Arrays.sort(files);//ensuring order 001, 002, ..., 010, ...
            return Arrays.asList(files);
        }

        public void mergeFiles(File oneOfFiles, File into)
                throws IOException {
            mergeFiles(listOfFilesToMerge(oneOfFiles), into);
        }

        public List<File> listOfFilesToMerge(String oneOfFiles) {
            return listOfFilesToMerge(new File(oneOfFiles));
        }

        public void mergeFiles(String oneOfFiles, String into) throws IOException {
            mergeFiles(new File(oneOfFiles), new File(into));
        }

        private String documentsInMyFolder() {
            String currentDir = System.getProperty("user.dir");
            // System.out.println("Current dir using System:" +currentDir);
            File myServerDirectory = new File(currentDir + "\\temp");
            File[] myDocumentList = myServerDirectory.listFiles();
            String output = "";
            for (int i = 0; i < myDocumentList.length; i++) {
                if (myDocumentList[i].isFile()) {
                    output = output.concat(myDocumentList[i].getName()) + " ";
                }
            }
            return output;
        }

    }


    private class DownloadFromPort extends Thread {

        Socket mySocketFO;
        ObjectOutputStream myOutputStreamFO;
        ObjectInputStream myInputStreamFO;
        int downPort;
        int state = 0;

        public DownloadFromPort(int fOPort) {
            downPort = fOPort;
        }

        public void run() {
            try {
                System.out.println("Trying to connect to File Owner at port [" + downPort + "]");
                Boolean isConnected = false;
                while (!isConnected) {
                    try {
                        mySocketFO = new Socket("localhost", downPort);
                        isConnected = true;
                    } catch (Exception e) {
                        System.out.println("File Owner at port [" + downPort + "] is not available");
                    }

                }

                System.out.println("\nConnection established with File Owner at port [" + downPort + "]");
                myOutputStreamFO = new ObjectOutputStream(mySocketFO.getOutputStream());
                myOutputStreamFO.flush();
                myInputStreamFO = new ObjectInputStream(mySocketFO.getInputStream());
                System.out.println("\n Connected to File Owner at port [" + downPort + "]\n");
                if (state == 0) {
                    //System.out.println("Inside state 0 sending to server");
                    String myStringPort = String.valueOf(myPort);
                    String commandToSend = myStringPort + " " + "start";
                    System.out.println("\nSENDING REQUEST [" + commandToSend + "] to File Owner at port [" + downPort + "]");
                    if (isFileMerged == false) {
                        writeObjectToOutputStreamFO(commandToSend);
                        try {
                            String temp = (String) myInputStreamFO.readObject();
                            System.out.println("\nReceived [Chunk list] from File Owner at port [" + downPort + "]");


                            // Getting a random first file to send to the peer
                            String[] listOfFiles = temp.split(" ");
                            //Set global value of TotalNumberOfFiles
                            TotalNumberOfFiles = listOfFiles.length;
                            System.out.println("Total number of files to receive: " + TotalNumberOfFiles);

                            Random randomGenerator = new Random();
                            int randomInt = randomGenerator.nextInt(listOfFiles.length);
                            String temp2 = listOfFiles[randomInt];
                            System.out.println("Requesting chunk [" + temp2 + "] from File Owner at port [" + downPort + "]");

                            if (!temp2.isEmpty()) {
                                if (isFileMerged == false) {
                                    writeObjectToOutputStreamFO(temp2 + " get");
                                    makeNewFileAndWriteData(temp2);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        state++;
                    }

                }
                // keep asking FileOwner with files which are not present inside your temp folder
                while (state == 1) {
                    try {
                        Thread.sleep(200);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isFileMerged == false) {
                        writeObjectToOutputStreamFO("command getList");

                        System.out.println("\nRequesting chunk list [command getList] from File Owner at port [" + downPort + "]");
                        try {
                            String filesInServer = (String) myInputStreamFO.readObject();
                            System.out.println("Received [Chunk list] from File Owner at port [" + downPort + "]");
                            // List of files present within temp folder
                            String filesInTemp = documentsInServer();
                            String[] listOfFilesInTemp = filesInTemp.split(" ");
                            String newTemp = "";
                            // Remove files from string filesInServer which are present in filesInTemp
                            for (int i = 0; i < listOfFilesInTemp.length; i++) {
                                //System.out.println("Text to remove: " + listOfFilesInTemp[i]);
                                newTemp = filesInServer.replace(listOfFilesInTemp[i] + " ", "");
                                filesInServer = newTemp;
                            }

                            //System.out.println("Files present in temp list:"+ filesInTemp);
                            //System.out.println("Files present in new Server list:"+ newTemp);
                            //Ask for random file from new list
                            String[] listOfFiles = newTemp.split(" ");
                            Random randomGenerator = new Random();
                            int randomInt = randomGenerator.nextInt(listOfFiles.length);
                            String temp2 = listOfFiles[randomInt];

                            if (!temp2.isEmpty()) {
                                if (isFileMerged == false) {
                                    System.out.println("\nRequesting chunk [" + temp2 + "] from File Owner at port [" + downPort + "]");
                                    writeObjectToOutputStreamFO(temp2 + " get");
                                    makeNewFileAndWriteData(temp2);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            //e.printStackTrace();
                        }
                    }


                }

            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        private void writeObjectToOutputStreamFO(String clientOutputSend) {
            if (!isFileMerged) {
                try {

                    myOutputStreamFO.writeObject(clientOutputSend);
                    myOutputStreamFO.flush();
                } catch (IOException e) {
                    System.out.println("Error while writing to output stream");
                    e.printStackTrace();
                }
            }


        }

        private String documentsInServer() {
            String currentDir = System.getProperty("user.dir");
            // System.out.println("Current dir using System:" +currentDir);
            File myServerDirectory = new File(currentDir + "\\temp");

            File[] myDocumentList = myServerDirectory.listFiles();
            String output = "";

            for (int i = 0; i < myDocumentList.length; i++) {
                if (myDocumentList[i].isFile()) {
                    output = output.concat(myDocumentList[i].getName()) + " ";
                }
            }
            return output;
        }

        private void makeNewFileAndWriteData(String document) throws IOException, ClassNotFoundException {

            System.out.println("Received chunk [" + document + "] from File Owner at port [" + downPort + "]");
            String currentDir = System.getProperty("user.dir");
            String fileName = currentDir + "\\temp\\" + document;
            if (!Files.exists(Path.of(fileName))) {
                BufferedOutputStream bufferedOS = new BufferedOutputStream(new FileOutputStream(fileName));

                bufferedOS.write((byte[]) myInputStreamFO.readObject());
                bufferedOS.close();
            }


        }
    }

    private class DownloadFromPeerPort extends Thread {

        Socket mySocketPE;
        ObjectOutputStream myOutputStreamPE;
        ObjectInputStream myInputStreamPE;
        int downPort;
        int state = 0;
        int selfPort;

        public DownloadFromPeerPort(int fOPort, int myPort) {
            this.downPort = fOPort;                            ////// earlier no this
            this.selfPort = myPort;
        }

        public void run() {
            try {
                System.out.println("\n Trying to connect to Peer at port [" + downPort + "]");
                Boolean isConnected = false;
                while (!isConnected) {
                    try {
                        mySocketPE = new Socket("localhost", downPort);
                        isConnected = true;
                    } catch (Exception e) {
                        System.out.println("Peer at port [" + downPort + "] is not available. Trying to establish connection...");
                    }

                }

                myOutputStreamPE = new ObjectOutputStream(mySocketPE.getOutputStream());
                myOutputStreamPE.flush();
                myInputStreamPE = new ObjectInputStream(mySocketPE.getInputStream());
                System.out.println("\nConnection established with Peer at port [" + downPort + "]");
                // Send initiation request to Server Peer

                writeObjectToOutputStreamPE(selfPort + " start");
                try {
                    String replyFromPeer = (String) myInputStreamPE.readObject();
                    System.out.println("Received [" + replyFromPeer + "] from Peer at port [" + downPort + "]");
                } catch (ClassNotFoundException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }

                //Waiting for Peer to take some files from the FileOwner
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                //Ask list of files from Peer
                while (true) {
                    try {
                        AskListOfFilesFromServerPeer();
                    } catch (ClassNotFoundException | InterruptedException e) {
                        System.out.println("Inside Exception of AskListOfFilesFromServerPeer");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void AskListOfFilesFromServerPeer() throws ClassNotFoundException, IOException, InterruptedException {
            //Wait few seconds
            if (!isFileMerged) {
                Thread.sleep(200);
                String myStringPort = String.valueOf(myPort);
                String commandToSend = myStringPort + " " + "getListFromPeer";

                //System.out.println("\nSENDING REQUEST ["+ commandToSend+"] to Peer at port ["+downPort+"]");
                if (isFileMerged == false) {
                    System.out.println("\nRequesting [Chunk list] from Download Peer at port [" + downPort + "]");
                    writeObjectToOutputStreamPE(commandToSend);
                    System.out.println("Received [Chunk list] from Peer at port [" + downPort + "]");
                    //List of files in Peer is received
                    String filesInServerPeer = (String) myInputStreamPE.readObject();
                    //Check files in my folder
                    String filesInTemp = documentsInMyFolder();
                    String[] listOfFilesInTemp = filesInTemp.split(" ");
                    String listOfFilesToTransfer = null;

                    for (int i = 0; i < listOfFilesInTemp.length; i++) {
                        //System.out.println("Text to remove: " + listOfFilesInTemp[i]);
                        listOfFilesToTransfer = filesInServerPeer.replace(listOfFilesInTemp[i] + " ", "");
                        filesInServerPeer = listOfFilesToTransfer;
                    }

                    if (listOfFilesToTransfer.length() > 4) {
                        //Transfer file
                        String[] listOfFiles = listOfFilesToTransfer.split(" ");
                        Random randomGenerator = new Random();
                        int randomInt = randomGenerator.nextInt(listOfFiles.length);
                        String temp2 = listOfFiles[randomInt];
                        System.out.println("\nRequesting chunk [" + temp2 + "] from Peer at port [" + downPort + "]");
                        //System.out.println("Sending command to get files from Peer:" + temp2);
                        writeObjectToOutputStreamPE(temp2 + " get");
                        makeNewFileAndWriteData(temp2);

                    } else {
                        AskListOfFilesFromServerPeer();
                    }
                }

            }

        }

        private String documentsInMyFolder() {
            String currentDir = System.getProperty("user.dir");
            File myServerDirectory = new File(currentDir + "\\temp");
            File[] myDocumentList = myServerDirectory.listFiles();
            String output = "";
            for (int i = 0; i < myDocumentList.length; i++) {
                if (myDocumentList[i].isFile()) {
                    output = output.concat(myDocumentList[i].getName()) + " ";
                }
            }
            return output;
        }

        private void writeObjectToOutputStreamPE(String clientOutputSend) {
            if (!isFileMerged) {
                try {
                    System.out.println("Sending command to Peer " + clientOutputSend);
                    myOutputStreamPE.writeObject(clientOutputSend);
                    myOutputStreamPE.flush();
                } catch (IOException e) {
                    System.out.println("Error while writing to output stream");
                    e.printStackTrace();
                }
            }
        }

        private void makeNewFileAndWriteData(String document) throws IOException, ClassNotFoundException {
            System.out.println("Received chunk [" + document + "] from Peer at port [" + downPort + "]");
            String currentDir = System.getProperty("user.dir");
            String fileName = currentDir + "\\temp\\" + document;
            if (!Files.exists(Path.of(fileName))) {
                BufferedOutputStream bufferedOS = new BufferedOutputStream(new FileOutputStream(fileName));
                bufferedOS.write((byte[]) myInputStreamPE.readObject());
                bufferedOS.close();
            }

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    private class UploadToPort extends Thread {

        int sPort;

        public UploadToPort(int port) {
            sPort = port;
        }

        public void run() {

            try {
                System.out.println("Upload thread started..\nWaiting for client connection...\n");
                ServerSocket listener = new ServerSocket(sPort);
                //mySocketMY = listener.accept();

                // Handler for handling multiple peers to upload chunks
                try {
                    while (true) {

                        new UploadHandler(listener.accept()).start();

                    }
                } catch (Exception e) {
                    System.out.println("Enter valid IP and Port");
                } finally {
                    listener.close();
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private class UploadHandler extends Thread {

            Socket mySocketMY;
            ObjectOutputStream myOutputStreamMY;
            ObjectInputStream myInputStreamMY;
            public int peerPortNumber;
            private String message;


            public UploadHandler(Socket connection) {
                this.mySocketMY = connection;
            }

            public void run() {
                try {
                    myOutputStreamMY = new ObjectOutputStream(mySocketMY.getOutputStream());
                    myOutputStreamMY.flush();
                    myInputStreamMY = new ObjectInputStream(mySocketMY.getInputStream());


                    // Get the peerPort number for displaying
                    try {
                        message = (String) myInputStreamMY.readObject();
                        processClientRequest(message);
                    } catch (ClassNotFoundException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    while (true) {
                        //System.out.println("\n Listening continuously for message from Client Peer...");
                        try {
                            message = (String) myInputStreamMY.readObject();
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        //show the message to the user
                        System.out.println("\nReceived request: [" + message + "] from Peer on Port [" + peerPortNumber + "]");
                        //System.out.println("---------------------------Message received from peer: " + message );

                        try {
                            processClientRequest(message);
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }


                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            private void processClientRequest(String input) throws IOException, ClassNotFoundException {
                String[] commandPart = input.split(" ");
                switch (commandPart[1]) {
                    case "start":
                        this.peerPortNumber = Integer.valueOf(commandPart[0]);
                        sendOutputToClient("Download of chunks available from Peer " + sPort);
                        break;
                    case "getListFromPeer":
                        sendOutputToClient(documentsInPeerDir());
                        break;
                    case "get":
                        writeFileToOutputStram(commandPart[0]);
                        break;
                    default:
                        System.out.println("Operation not supported");
                        break;
                }
            }

            private void writeFileToOutputStram(String document) {
                System.out.println("Sending [" + document + "] to Peer at port [" + peerPortNumber + "]");
                try {
                    String currentDir = System.getProperty("user.dir");
                    File myDocument = new File(currentDir + "\\temp\\" + document);
                    InputStream is = new FileInputStream(myDocument);
                    sendOutputToClient(is.readAllBytes());
                    is.close();
                } catch (Exception e) {
                    sendOutputToClient("Error while getting the file");
                }
            }

            void sendOutputToClient(Object clientOutputSend) {
                try {
                    myOutputStreamMY.writeObject(clientOutputSend);
                    myOutputStreamMY.flush();
                } catch (IOException e) {
                    System.out.println("Error while writing to output stream");
                    e.printStackTrace();
                }
            }

            private String documentsInPeerDir() throws IOException {
                System.out.println("Sending [Chunk list] to Peer at port [" + peerPortNumber + "]");
                String currentDir = System.getProperty("user.dir");
                File myServerDirectory = new File(currentDir + "\\temp");
                File[] myDocumentList = myServerDirectory.listFiles();
                String output = "";

                for (int i = 0; i < myDocumentList.length; i++) {
                    if (myDocumentList[i].isFile() && myDocumentList[i].length() > 0) {
                        output = output.concat(myDocumentList[i].getName()) + " ";
                    }
                }
                return output;
            }
        }
    }
}