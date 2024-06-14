package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.logging.Logger;


public class Client implements Runnable {
    String username, id;
    //To know if its their first time or they've signed out and act accordingly
    boolean firstTime = true,signOut = false;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    //Socket to be used to communicate with browser/client
    private Socket clientSocket;
    //To be used to communicate with server
    private Server server;
    //To be used to communicate with game
    private Game game;
    //To reference this Client with the same username that is stored in server list, this is for making the game seem persistent because the connections arent persistent
    //And to reference that Client's opponent
    //Will discuss more on this down at where Cookies are handled below
    private Client me,opponent;
    //Count to be used for logging purposes/debugging
    public int count;
    //Piece to be used on the board in game
    private String piece;
    //BufferedReader to read requests coming from browser
    BufferedReader reader;
    //BufferedWriter to write back a response to the request
    BufferedWriter writer;
    //To see if the player is in a game or not
    public boolean inGame = false;
    //File paths to the html pages
    private static final String GAME_FILE_PATH = "src/main/java/org/example/game.html";
    private static final String AUTH_FILE_PATH = "src/main/java/org/example/auth.html";
    private static final String MENU_FILE_PATH = "src/main/java/org/example/menu.html";
    private static final String CHAT_FILE_PATH = "src/main/java/org/example/chat.html";
    //Constructor
    public Client(Socket clientSocket,Server server,int count) {
        this.count = count;
        this.clientSocket = clientSocket;
        this.server = server;
    }
    //Dummy constructor for Computer
    public Client(){}
    //run function which executes when the thread starts
    //which is the main function that handles all the parsing and responses
    @Override
    public void run() {

        try {
            //Just trying this to keep connection persistent (does not work)
            clientSocket.setKeepAlive(true);
            while (!clientSocket.isClosed()) {
                //Getting input stream from socket and setting it to reader
                 reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 //Getting output stream from socket and putting it in writer
                 writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                //Read the request headers
                String line;
                StringBuilder requestBuilder = new StringBuilder();
                //Reads the HEADER of the http and stops at the empty line separating the HEADER and BODY
                while ((line = reader.readLine()) != null && !line.isBlank()) {
                    //Adds each line in the header to requestBuilder
                    requestBuilder.append(line).append("\r\n");
                }


                //If line is null then there was nothing sent so we break out of the loop or close connection
                if (line == null) {
                    break;
                }

                //Storing the HEADER in String request
                String request = requestBuilder.toString();
                //Storing each lines in the header in requestLines
                String[] requestLines = request.split("\r\n");
                //Getting the first Line in the HEADER and split by space to get METHOD and PATH e.g: "GET /menu"
                String[] requestLine = requestLines[0].split(" ");
                String method = requestLine[0];
                String path = requestLine[1];
                //For debugging purposes
                System.out.println(method);
                System.out.println(path);
                //Extracting cookie from a request sent from chrome
                String chromeCookie = parseCookieForChrome(requestLines);
                //Extracting cookie from a request sent from edge
                //Both browsers may send cookies differently depending on device that's why
                String edgeCookie = parseCookieForEdge(requestLines);

                if(chromeCookie != null){
                    //If cookie is not null then that means they've already authenticated and it's not their first time here so firstTime set to false
                    firstTime = false;
                    //This is because when settin cookie (when they authenticate) we use their username to be the cookie
                    this.username = chromeCookie;
                    //This is to check if the client already exists because this could be a new connection with a new Cient class created to handle it
                    //Only their username will be same but their instances are different, the client in the server might already have a closed socket.
                    //So if client in the server is there then we set their writer and reader to ours(this current instance that has an open socket), so they can communicate with browser.
                    //This is because we use client in server to communicate with game so we cant have their sockets closed or reader/writer null
                    if(server.clientExists(this.username)){
                        //This is to check if the instance in the server client List is same as this current instance we're on.
                        //If they are not the same then update the instance in the client list's reader and writer(basically if they're reader and writer is null so we set it to ours).
                        if(!server.getClient(this.username).equals(this) && method.equals("POST") && !path.isEmpty()){
                            //Get the instance of that client in the server
                            me = server.getClient(this.username);
                            //Since this is a new instance, its game will be null, so we need to get the game stored in the game list in the server
                            this.game = server.getGameMult(this.username);
                            //If game is still null then it must be a single player game so we get for single player game
                            if(this.game == null){
                                this.game = server.getGameSingle(this.username);
                            }
                            //Setting the reader and writer
                            me.setReader(this.reader);
                            me.setWriter(this.writer);
                            //JUst checking to avoid null pointer errors
                            if(this.game != null){
                                if(game.isMultiPlayer){
                                    //To get opponent in the game only if they're still in the game
                                    if(me.inGame && me.getGame().getOpponent(me).inGame){
                                        opponent = this.game.getOpponent(me);
                                    }
                                }
                            }
                            System.out.println("Old Client updated");
                        }

                    }
                    else{
                        //If client dont exist then insert them
                        server.insertClient(this.username,this);
                        System.out.println("New client Added");
                    }
                    System.out.println("\nCHROME COOKIE: " + chromeCookie + "\n");
                }
                //Same as the above but for edge
                else if(edgeCookie != null){
                    firstTime = false;
                    this.username = edgeCookie;
                    if(server.clientExists(this.username)){
                        if(!server.getClient(this.username).equals(this) && method.equals("POST") && !path.isEmpty()){
                            me = server.getClient(this.username);
                            me.setReader(reader);
                            me.setWriter(writer);
                            this.game = server.getGameMult(this.username);
                            if(this.game == null){
                                this.game = server.getGameSingle(this.username);
                            }
                            if(this.game != null){
                                if(game.isMultiPlayer){
                                    if(me.inGame && me.getGame().getOpponent(me).inGame){
                                        opponent = this.game.getOpponent(me);
                                    }
                                }
                            }


                            System.out.println("Old Client updated");
                        }
                    }
                    else{
                        server.insertClient(this.username,this);
                        System.out.println("New client Added");
                    }
                    System.out.println("\nEDGE COOKIE: " + edgeCookie + "\n");
                }
                // This is for GET requests
                if ("GET".equals(method)) {
                    if("/auth".equals(path)){
                        //firstTime = false;
                        serveFile(AUTH_FILE_PATH);
                    }
                    //To redirect to auth if they havent authenticated
                    else if(path.startsWith("/") && firstTime && !"/auth".equals(path)){
                        writeRedirectResponse(writer,"auth");
                    }
                    //To redirect to menu if they dont enter any of the provided paths
                    else if(!"/menu".equals(path) && !"/game".equals(path) && !"/chat".equals(path) && !"/auth".equals(path)){
                        writeRedirectResponse(writer,"menu");
                    }
                    else if ("/game".equals(path)) {
                        serveFile(GAME_FILE_PATH);
                    }
                    else if("/menu".equals(path)){
                        serveFile(MENU_FILE_PATH);
                    }
                    else if("/chat".equals(path)){
                        serveFile(CHAT_FILE_PATH);
                    }

                     else {
                        // Send a 404 Not Found for any other GET requests
                        writeResponse( "HTTP/1.1 404 Not Found", "text/plain", "File Not Found".getBytes(), false);
                    }
                     //For POST requests
                } else if ("POST".equals(method)) {
                    //Post for when players authenticate
                    if("/auth".equals(path)){
                        String postData = readBody(requestLines);
                        this.username = postData.trim();
                        if(server.clientExists(this.username)){
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", "taken".getBytes(), true);
                        }
                        else{
                            //firstTime = false;
                            server.insertClient(this.username,this);
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", "/menu".getBytes(), true);
                        }
                    }
                    //This is the POST for when a player makes a move
                    else if("/makeMove".equals(path)){
                        String move = readBody(requestLines);
                        //Checking and setting to avoid null pointer errors
                        if(this.game == null){
                            this.game = server.getGameMult(this.username);
                        }
                        if(this.game.isSinglePlayer){
                            //Set the move in Game
                            game.setMove(Integer.parseInt(move));
                            game.updateServerBoard();
                            //update the board with new move that was set
                            if(game.checkWin()){
                                game.endGame(true);
                                continue;
                            }
                            else if(game.checkDraw()){
                                game.endGame(false);
                                continue;
                            }
                            //swap turn
                            game.swapTurn();
                            String bestMove = Integer.toString(game.findBestMove());
                            game.setMove(Integer.parseInt(bestMove));
                            game.updateServerBoard();
                            System.out.println("Best move: " + bestMove);
                            //To check again after computer has made its move
                            if(game.checkWin()){
                                System.out.println("Someone won");
                                game.endGame(true);
                                continue;
                            }
                            else if(game.checkDraw()){
                                game.endGame(false);
                                continue;
                            }//Swap turn again after A.I has made its move
                            game.swapTurn();
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", bestMove.getBytes(), true);
                        }
                        else{
                            if(this.game.checkOpponentLeft(me)){
                                System.out.println("\nOPPONENT LEFT!!\n");
                                writeResponse( "HTTP/1.1 200 OK", "text/plain", "playerLeft".getBytes(), true);
                                continue;
                            }
                            game.setMove(Integer.parseInt(move));
                            game.updateServerBoard();
                            if(game.checkWin()){
                                game.endGame(true);
                                continue;
                            }
                            else if(game.checkDraw()){
                                game.endGame(false);
                                continue;
                            }
                            game.swapTurn();
                            //Update the clients(browser) UI
                            game.updateClientBoards();
                        }

                    }
                    //Post for queueing up into a game or getting into a game
                    else if("/play".equals(path)){
                        //Clear the reader for next request
                        readBody(requestLines);
                        if(server.isQueueEmpty()){
                            server.addClientToQueue(me);
                        }
                        else{
                            //To avoid player pressing play twice and getting into a game
                            if(!server.checkClientInQueue(me)){
                                opponent = server.getClientFromQueue();
                                //opponent = server.getClient(opponentReference.getUsername());
                                //me = server.getClient(this.username);
                                server.startGame(me,opponent);
                                System.out.println("Game started");
                            }
                        }
                    }
                    //This is the POST that gets sent when the game.html is loaded for the first time
                    //It is sent to retrieve the player's turns
                    else if("/getTurn".equals(path)){
                        readBody(requestLines);
                        if(game.isMultiPlayer){
                            if(game.getMyTurn(me)){
                                System.out.println("Client:" + count + " ITS MY TURN!");
                                me.writeResponse( "HTTP/1.1 200 OK", "text/plain", ("1," + game.getOpponentName(me) + ",mult").getBytes(), true);
                            }
                            else{
                                System.out.println("Client:" + count + " ITS NOT MY TURN!");
                                me.writeResponse( "HTTP/1.1 200 OK", "text/plain", ("0," + game.getOpponentName(me) + ",mult").getBytes(), true);
                            }
                            System.out.println("sent turn for client: "+ count);
                        }
                        else if(game.isSinglePlayer){
                            if(game.getMyTurn()){
                                writeResponse( "HTTP/1.1 200 OK", "text/plain", ("1,Computer,single,dummy").getBytes(), true);
                            }
                            else{
                                int bestMove = game.findBestMove();
                                writeResponse( "HTTP/1.1 200 OK", "text/plain", ("0,Computer,single," + bestMove).getBytes(), true);
                                System.out.println("Computer made a move");
                                game.setMove(bestMove);
                                game.updateServerBoard();
                                game.swapTurn();
                            }
                        }
                    }
                    //Post to request for rematch
                    else if("/rematch".equals(path)){
                        String gameMode = readBody(requestLines);
                        //If its a single player requesting rematch then just start a new game
                        if(gameMode.equals("single")){
                            server.startGame(me);
                            continue;
                        }
                        //Check if game still exists or not
                        if(!server.multiplayerGameExists(this.username)){
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", ("timeout").getBytes(), true);
                            continue;
                        }
                        //Check if opponent left
                        if(game.checkOpponentLeft(me)){
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", ("playerLeft").getBytes(), true);
                            continue;
                        }
                        if(game.isRematchEmpty()){
                            game.addRematchRequest(me);
                        }
                        else{
                            Client myOpp = game.getRematch();
                            server.startGame(me,myOpp);
                        }
                    }
                    //Dummy Post to retrieve the update for game (polling)
                    else if("/getUpdate".equals(path)){
                        System.out.println(this.username + " writer in getUpdate" + writer);
                        readBody(requestLines);
                        if(this.game == null){
                            Client temp = server.getClient(this.username);
                            temp.setWriter(this.writer);
                            temp.setReader(this.reader);
                        }
                    }
                    //Post to get Username(IDK if this is still being used, but keeping it here just in case)
                    else if("/getUsername".equals(path)){
                        readBody(requestLines);
                        if (this.username == null || this.username.isEmpty()) {
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", "null".getBytes(), true);
                        }
                        else{
                            writeResponse( "HTTP/1.1 200 OK", "text/plain", this.username.getBytes(), true);
                        }
                    }
                    //Post to get all users for when chat.html loads up
                    else if("/getAllUsers".equals(path)){
                        String username1 = readBody(requestLines);
                        this.username = username1;
//                        if(!server.clientExists(username1)){
//                            server.insertClient(username1,this);
//                        }
                        String users = server.getAllUsernames(username1);
                        writeResponse( "HTTP/1.1 200 OK", "text/plain", users.getBytes(), true);
                    }
                    //Post to send message from chat.html to the receivers browser
                    else if("/sendMessage".equals(path)){
                        String bodyContent = readBody(requestLines);
                        String []bodyContentSplit = bodyContent.split("\\|");
                        String senderUsername = bodyContentSplit[0];
                        this.username = senderUsername;
                        String receiverUsername = bodyContentSplit[1];
                        String message = bodyContentSplit[2];
                        Client receiver = server.getClient(receiverUsername);
                        System.out.println("USERNAME: " + this.username);
                        receiver.writeResponse("HTTP/1.1 200 OK", "text/plain", (senderUsername + "|" + message).getBytes(), true);
                        writeResponse("HTTP/1.1 200 OK", "text/plain", ("Message Sent!").getBytes(), true);
                    }
                    //Dummy post to listen for any messages coming from chat.html
                    else if("/getMessage".equals(path)){
                        String username1 = readBody(requestLines);
                        this.username = username1;
                        if(!server.clientExists(username1)){
                            server.insertClient(username1,this);
                            System.out.println("\nCLIENT UPDATED!!!\n");
                        }
                        firstTime = false;
                    }
                    //Post to go back to menu.html
                    //Does a bit of checking before redirecting to menu
                    //Such as making the player leave game or checking if opponent left game
                    else if("/getMenu".equals(path)){
                        String body = readBody(requestLines);
                        if(game == null || game.isSinglePlayer){
                            writeResponse("HTTP/1.1 200 OK", "text/plain", ("/menu").getBytes(), true);
                            continue;
                        }
                        if(game.isMultiPlayer){
                            if(this.game.getMyTurn(me) && body.equals("playerLeft") && !game.checkOpponentLeft(me)){
                                opponent = game.getOpponent(me);
                                opponent.writeResponse("HTTP/1.1 200 OK", "text/plain", ("playerLeft").getBytes(), true);
                                writeResponse("HTTP/1.1 200 OK", "text/plain", ("/menu").getBytes(), true);
                                this.game.setLeave(me);
                            }
                            else{
                                this.game.setLeave(me);
                                writeResponse("HTTP/1.1 200 OK", "text/plain", ("/menu").getBytes(), true);
                            }
                        }
                    }
                    //Post to sign out and clear cookie
                    else if("/signOut".equals(path)){
                        server.removeClient(this.username);
                        readBody(requestLines);
                        firstTime = true;
                        signOut = true;
                        writeResponse( "HTTP/1.1 200 OK", "text/plain", "signOut".getBytes(), true);
                    }
                    //Post for when player choose to player single player
                    else if("/playSingle".equals(path)){
                        System.out.println("Single play called");
                        server.startGame(me);
                    }
                }
                else{
                    writeResponse( "HTTP/1.1 200 OK", "text/plain", "UNKNOWN REQUEST".getBytes(), true);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client " + id + ": " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("Connection closed for Client ID: " + count); //+ java.time.LocalDateTime.now());
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing connection for client " + id + ": " + e.getMessage());
            }
        }
    }
    //Function to read the body in the http request
    public String readBody(String []httpHeaders) throws IOException {
        int contentLength = getContentLength(httpHeaders);
        char[] body = new char[contentLength];
        reader.read(body, 0, contentLength);
        return new String(body);
    }
    //Function to serve html files to client
    public void serveFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            writeResponse( "HTTP/1.1 200 OK", "text/html", fileContent, true);
        } else {
            writeResponse( "HTTP/1.1 404 Not Found", "text/plain", "File not found".getBytes(), false);
        }
    }
    //Function to write a response back to the client
    public void writeResponse(String statusLine, String contentType, byte[] content, boolean keepAlive) throws IOException {
        writer.write(statusLine + "\r\n");
        writer.write("Content-Type: " + contentType + "\r\n");
        writer.write("Content-Length: " + content.length + "\r\n");
        if (keepAlive) {
            writer.write("Connection: keep-alive\r\n");
            writer.write("Keep-Alive: timeout=600, max=100\r\n");
        } else {
            writer.write("Connection: close\r\n");
        }
        if(signOut){
            writer.write("Set-Cookie: new=; Path=/; Max-Age=0; HttpOnly\r\n");

        }
        else if(this.username != null && !this.username.isEmpty()){
            writer.write("Set-Cookie: new=" + this.username + "; Path=/\r\n");
        }

        writer.write("\r\n");
        writer.write(new String(content));
        writer.flush();
    }
    //Function to be used to redirect client to another page if needed
    private void writeRedirectResponse(BufferedWriter writer, String newLocation) throws IOException {
        writer.write("HTTP/1.1 302 Found\r\n");
        writer.write("Location: " + newLocation + "\r\n");
        writer.write("Connection: keep-alive\r\n");
        writer.write("Keep-Alive: timeout=6000, max=100000\r\n");
       // writer.write("Set-Cookie: new=" + "notFirstTime" + "; Path=/; HttpOnly\r\n");
        writer.write("\r\n");
        writer.flush();
    }
    //Function to get content length in the HEADER
    private int getContentLength(String[] requestHeaders) {
        for (String header : requestHeaders) {
            if (header.toLowerCase().startsWith("content-length:")) {
                return Integer.parseInt(header.substring("content-length:".length()).trim());
            }
        }
        return 0;
    }
    //Function to parse cookie from chrome
    private String parseCookieForChrome(String[] requestHeaders) {
        for (String header : requestHeaders) {
            if (header.toLowerCase().startsWith("cookie:")) {
                String[] cookies = header.substring(7).split("; ");
                for (String cookie : cookies) {
                    String[] parts = cookie.split("=", 2);
                    if ("new".equalsIgnoreCase(parts[0]) && parts.length > 1) {
                        return parts[1];
                    }
                }
            }
        }
        return null;
    }
    //Function for parsing cookie from Edge
    private String parseCookieForEdge(String[] requestHeaders) {
        for (String header : requestHeaders) {
            if (header.toLowerCase().startsWith("cookie:")) {
                // Properly handle the case by trimming potential spaces
                String cookiesString = header.substring("cookie:".length()).trim();
                String[] cookies = cookiesString.split(";\\s*"); // Split cookies more reliably
                for (String cookie : cookies) {
                    String[] parts = cookie.split("=", 2);
                    if (parts.length == 2 && "new".equalsIgnoreCase(parts[0].trim())) {
                        return parts[1].trim();  // Return the value, trimmed of any extraneous spaces
                    }
                }
            }
        }
        return null;
    }
    //GETTERS and SETTERS below
    public void setGame(Game game){
        this.game = game;
    }
    public Game getGame(){
        return this.game;
    }
    public String getPiece(){
        return this.piece;
    }
    public void setPiece(String piece){
        this.piece = piece;
    }
    public Socket getClientSocket(){
        return this.clientSocket;
    }
    public void setClientSocket(Socket socket){
        this.clientSocket = socket;
    }
    public void setWriter(BufferedWriter writer){
        this.writer = writer;
    }
    public void setReader(BufferedReader reader){
        this.reader = reader;
    }
    public String getUsername(){
        return this.username;
    }
    public void setMeAndOpponent(String username){

    }
    public BufferedWriter getWriter(){
        return this.writer;
    }

}
