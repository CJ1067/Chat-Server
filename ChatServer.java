import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatServer class
 *
 * Server with functionality to run a chat server for users to communicate with the group or directly with another user
 *
 * @author Christopher Lehman
 * @author David Sillman
 *
 * @version 11/8/18
 *
 */

final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private String badWordsFileName;
    private ChatFilter cf;


    private ChatServer(int port, String badWordsFileName) {
        this.port = port;
        this.badWordsFileName = badWordsFileName;
        cf = new ChatFilter(badWordsFileName);
    }

    /*
     * This is what starts the ChatServer.
     */
    private void start() {
        File file = new File(badWordsFileName);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Banned Words File: " + file.getName() + "\nBanned Words:");

            //add all words in file to list for future checking

            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }
                System.out.println(line);
            }
            System.out.println();

            while (true) {
                System.out.println("Server waiting for Clients on port " + port + ".");
                Socket socket = serverSocket.accept();
                ClientThread newClient = new ClientThread(socket, uniqueId++);
                boolean invalid = false;
                for (ClientThread ct : clients) {
                    if (ct.getUsername().equals(newClient.getUsername())) {
                        invalid = true;
                        break;
                    }
                }
                if (invalid) {
                    System.out.println("User rejected: \" " + newClient.getUsername() + "\" (duplicate name)");
                    newClient.writeMessage("DUPLICATE");
                    newClient.close();
                    socket.close();
                } else {
                    Runnable r = newClient;
                    Thread t = new Thread(r);
                    clients.add((ClientThread) r);
                    t.start();
                }
            }
        } catch (BindException be) {
            System.out.println("Server already in use");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void broadcast(String message) throws IOException {
        ChatFilter cf = new ChatFilter(badWordsFileName);
        message = cf.filter(message);
        for (ClientThread ct : clients) {
            ct.writeMessage(message);
        }
    }

    private synchronized void directMessage(String message, String sender, String username) throws IOException {
        message = cf.filter(message);
        for (ClientThread ct : clients) {
            if (ct.getUsername().equals(username)) {
                String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                ct.writeMessage(message);
                System.out.println(message);
            } else if (ct.getUsername().equals(sender)){
                ct.writeMessage(message);
            }
        }
    }

    private synchronized void remove(int id) {
        for (ClientThread ct : clients) {
            if (ct.id == id) {
                ct.close();
                clients.remove(ct);
                break;
            }
        }
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1500;
        String bwfn = (args.length > 1) ? args[1] : "badwords.txt";
        ChatServer server = new ChatServer(port, bwfn);
        server.start();
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private synchronized boolean writeMessage(String message) throws IOException {
            if(socket.isConnected()) {
                sOutput.writeObject(message);
                return true;
            }
            return false;
        }

        private String getUsername() {
            return username;
        }
        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            // Read the username sent to you by client

            System.out.println(username + " just connected.");
            // Send message back to the client
            try {
                broadcast(username + " just connected.\n");
                while (true) {
                    cm = (ChatMessage) sInput.readObject();
                    String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                    if (cm.toString() != null) {
                        System.out.print("(" + timeStamp + ") " + username + " : " + cf.filter(cm.toString()) + "\n");
                        broadcast("(" + timeStamp + ") " + username + " : " + cm.toString() + "\n");
                    } else if (cm.getType() == ChatMessage.ChatType.COMMAND) {
                        if (cm.getMessage().equals("/logout")) {
                            //System.out.println("DEBUG: read 'logout'");
                            System.out.println(username + " disconnected with a LOGOUT message.");
                            remove(id);
                            broadcast(username + " disconnected with a LOGOUT message.\n");
                            socket.close();
                            break;
                        } else if (cm.getMessage().length() > 3 &&
                                cm.getMessage().substring(0, 4).equals("/msg")) {
                            int lastSpace = cm.getMessage().lastIndexOf(" ");
                            if (lastSpace == -1) {
                                break;
                            }
                            String[] input = cm.getMessage().split(" ");
                            for (int s = 3; s < input.length; s++) {
                                input[2] += " " + input[s];
                            }
                            if (!username.equals(cm.getMessage().substring(5, lastSpace))) //recipient username
                                directMessage( "(" + timeStamp + ") " + username + " -> " + input[1] + " : " + input[2] + "\n",  //message
                                    username, input[1]);
                        } else if (cm.getMessage().length() > 4 &&
                                cm.getMessage().substring(0, 5).equals("/list")) {
                            for (ClientThread ct : clients) {
                                if (!ct.username.equals(username)) {
                                    sOutput.writeObject(ct.username + "\n");
                                }
                            }
                        }
                    }
                }
            } catch (StringIndexOutOfBoundsException sioobe) {
                return;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(username + " has force closed and disconnected.");
                remove(id);
            }
        }

        private synchronized void close() {
            try {
                sInput.close();
                sOutput.close();
            } catch(IOException ioe) {
                System.err.println("Encountered IOE when closing.");
            }
        }
    }
}
