package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Server {
    private static final int PORT = 8080;
    private static int count = 0;
    private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    private final List<Game> games = new ArrayList<>();
    private final Queue<Client> queue = new LinkedList<Client>();
    String ipAddress = "144.120.168.111";
    public void start() {
        try  {
            ServerSocket serverSocket = new ServerSocket(PORT);
            //InetAddress bindAddr = InetAddress.getByName(ipAddress);
            //InetSocketAddress socketAddress = new InetSocketAddress(bindAddr, PORT);

            //serverSocket.bind(socketAddress);
            System.out.println("Server started on port " + PORT);

            while (true) {
                try {
                    //This accepts any connection coming to the port where ServerSocket is listening
                    //And puts that connection into a Socket
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("client joined: " + ++count);
                    //Creating a new client to handle communication with the browser and passing in the socket and server in the constructor
                    Client client = new Client(clientSocket, this,count);
                    //Starting the thread.
                    new Thread(client).start();
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    // main method to create the server and start it
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
    //Function to add player to queue when they join queue for a multiplayer game
    public void addClientToQueue(Client client){
        this.queue.add(client);
    }
    //Check if queue is empty or not
    public boolean isQueueEmpty(){
        return this.queue.isEmpty();
    }
    //Function to retrieve the client infront of the queue
    public Client getClientFromQueue(){
        return this.queue.poll();
    }
    //Function to check if there is a specific client in queue to avoid queueing the same client
    public boolean checkClientInQueue(Client client){
        return queue.contains(client);
    }
    //Function to start multiplayer game
    public void startGame(Client player1,Client player2){
        //Checks to see if there is a game already containing the 2 clients and removes it from list of games to avoid clashes/errors
        removeGameFromList(player1.getUsername(),player2.getUsername());
        //Sets players ingame to true for leaving game purposes.
        player1.inGame = true;
        player2.inGame = true;
        String gameId = UUID.randomUUID().toString();
        Game newGame = new Game(player1,player2,gameId,this);
        //Add game to list of games stored on the server
        games.add(newGame);
    }
    //Function to start single player game
    public void startGame(Client client) throws IOException {
        removeGameFromList(client.getUsername());
        client.inGame = true;
        Game newGame = new Game(client,this);
        games.add(newGame);
        System.out.println(newGame);
        System.out.println("Game started!");
    }
    //Function to end a game
    public void endGame(Game game){
        if(game != null && game.isMultiPlayer){
            //Retrieve players in the game
            List<Client> players = game.getPlayers();
            //Sets their ingame to false for error purposes
            players.get(0).inGame = false;
            players.get(1).inGame = false;
            games.remove(game);
        }
        else if(game != null && game.isSinglePlayer){
            game.singlePlayer.inGame = false;
            games.remove(game);
        }
    }
    //To check if a game exists and it is multiplayer
    public boolean multiplayerGameExists(String username){
        for(int i = 0; i < games.size(); i++){
            if(games.get(i).PlayerInMultGame(username)){
                return true;
            }
        }
        return false;
    }
    //Function to get a multiplayer game given username of a player inside
    public Game getGameMult(String username){
        for(int i = 0; i < games.size(); i++){
            if(games.get(i).PlayerInMultGame(username)){
                return games.get(i);
            }
        }
        return null;
    }
    //Function to get a single player game given a username inside the game
    public Game getGameSingle(String username){
        for(int i = 0; i < games.size(); i++){
            if(games.get(i).PlayerInSingleGame(username)){
                return games.get(i);
            }
        }
        return null;
    }
    //Function to insert a Client in client list
    public void insertClient(String username,Client client){
        clients.put(username,client);
    }
    //Function to get a Client from the client list
    public Client getClient(String username){
        return clients.get(username);
    }
    //Function to check if a client exists in the client list
    public boolean clientExists(String username){
        return clients.containsKey(username);
    }
    //Function to retrieve all usernames stored in the clients list except the username in the parameter
    public String getAllUsernames(String userName) {
        if (this.clients.isEmpty()){
            return "";
        }
        return this.clients.values().stream()  // Convert the collection of Client objects (values of the clients map) to a Stream for processing
                .map(Client::getUsername)          // Transform the Stream of Client objects into a Stream of their usernames by calling getUsername function from Client on each Client
                .filter(username -> !username.equals(userName)) // Filter the stream to exclude the username that matches the 'userName' parameter
                .collect(Collectors.joining(",")); // Join all the remaining usernames in the stream into a single String, separated by commas
    }
    //Remove a client from the list based on username
    public void removeClient(String username){
        clients.remove(username);
    }
    //Remove a multiplayer game based on 2 usernames (the 2 players)
    public void removeGameFromList(String client1,String client2){
        for(int i = 0; i < games.size(); i++){
            if(games.get(i).checkGameIsLive(client1,client2)){
                games.remove(i);
                System.out.println("Game removed!!");
                return;
            }
        }
        System.out.println("GAME WAS NOT REMOVED!");
    }
    //remove a a single player game from the lsit of games
    public void removeGameFromList(String client){
        for(int i = 0; i < games.size(); i++){
            if(games.get(i).checkGameIsLive(client)){
                games.remove(i);
                System.out.println("Game removed!!");
                return;
            }
        }
        System.out.println("GAME WAS NOT REMOVED!");
    }
}
