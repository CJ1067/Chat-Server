import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
/**
 * ChatClient class
 *
 * Models a client who can connect to the server and utilize the chatbot
 *
 * @author Christopher Lehman
 * @author David Sillman
 *
 * @version 11/8/18
 *
 */
final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
        } catch (UnknownHostException uhe) {
            System.err.println("Could not find host: " + server);
            return false;
        } catch (ConnectException ce) {
            System.err.println("Bad port: " + port);
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        System.out.println("Connection accepted " + socket.getInetAddress() + ":" + port);
        try {
            sOutput.writeObject(username);
            Scanner keyboard = new Scanner(System.in);
            keyboard.useDelimiter("\n");
            while (true) {
                String written = keyboard.next();
                ChatMessage.ChatType type = ChatMessage.ChatType.GLOBAL;
                if (written.length() > 0) {
                    if (written.charAt(0) == '/') {
                        type = ChatMessage.ChatType.COMMAND;
                    }
                    sOutput.writeObject(new ChatMessage(type, written));
                    if (written.equals("/logout") || socket.isClosed()) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Part 2 : Get proper arguments and override defaults
        String address = "localhost";
        int port = 1500;
        String username = "Anonymous"; //defaults unless specified in program args
        try {
            username = args[0];
            port = Integer.parseInt(args[1]);
            address = args[2];
        } catch(ArrayIndexOutOfBoundsException aioobe) { }
        // Create your client and start it
        ChatClient client = new ChatClient(address, port, username);
        if (client.start()) {
            // Send an empty message to the server
            client.sendMessage(new ChatMessage());
        }
    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            try {
                while (!socket.isClosed()) {
                    try {
                        String msg = (String) sInput.readObject();
                        if (msg.equals("DUPLICATE")) {
                            System.out.println("Your username is already in use.");
                            System.exit(0);
                        }
                        System.out.print(msg);
                    } catch (EOFException eofe) {
                        sInput.close();
                        sOutput.close();
                        socket.close();
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {

            }
            if (socket.isClosed()) {
                System.out.println("Server has closed the connection");
            }
        }
    }
}
