package org.example;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Game {
    //List of different Clients for different purposes (multiplayer and singleplayer)
    Client player1, player2, currentPlayer, notCurrentPlayer, singlePlayer, computer = new Client();
    String player1Name, player2Name, singlePlayerName;
    public boolean player1Left = false, player2Left = false, isSinglePlayer = false, isMultiPlayer = false;
    //To use to track players leaving game
    int playerLeaveCount = 0;
    //Executor to run endGame after a specified amount of time
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //Server to be used to communicate with server
    Server server;

    private Queue<Client> rematchRequest = new LinkedList<Client>();
    String gameId;
    String computerPiece;
    private int move;
    private final int SIZE = 3;
    //board
    String board[][] = new String[SIZE][SIZE];
    //used to generate random booleans for choosing turns
    Random random = new Random();

    public Game(Client player1, Client player2, String gameId, Server server) {
        System.out.println("Client " + player1.count + " in game constructor: " + player1);
        System.out.println("Client " + player2.count + " in game constructor: " + player2);
        this.isMultiPlayer = true;
        this.server = server;
        initializeBoard();
        this.gameId = gameId;
        //This allows Game to communicate with players as they have access to the Players reference
        this.player1 = player1;
        this.player2 = player2;
        this.player1Name = player1.getUsername();
        this.player2Name = player2.getUsername();
        //This allows players to communicate with the Game as they have access to this Game through reference
        player1.setGame(this);
        System.out.println("Game set for player1");
        player2.setGame(this);
        System.out.println("Game set for player2");
        if (player1.equals(player2)) {
            System.out.println("THEY ARE EQUAL SOMEHOW");
        }
        //For choosing random turn
        //TO-DO LIST:
        //Code to choose whos turn, Right now its not working and Im just loading game to both
        //Figure out how to get the players(browsers) to know its their turn
        if (random.nextBoolean()) {
            player1.setPiece("X");
            player2.setPiece("O");
            currentPlayer = player1;
            notCurrentPlayer = player2;
        } else {
            player2.setPiece("X");
            player1.setPiece("O");
            currentPlayer = player2;
            notCurrentPlayer = player1;
        }
        try {
            //Loading the pages to each player(Uses javascript in frontend to redirect to localhost:8080/game check that out in menu.html
            currentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", "http://localhost:8080/game".getBytes(), true);
            System.out.println("Player1 done");
            notCurrentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", "http://localhost:8080/game".getBytes(), true);
            System.out.println("Player 2 done");
        } catch (IOException e) {
            System.out.println("Exeption Error: " + e.getMessage());
        }
    }
    //Constructor for Single player game
    public Game(Client player, Server server) throws IOException {
        this.isSinglePlayer = true;
        this.singlePlayer = player;
        this.singlePlayer.setGame(this);
        this.server = server;
        this.singlePlayerName = player.getUsername();
        initializeBoard();
        player.writeResponse("HTTP/1.1 200 OK", "text/plain", "http://localhost:8080/game".getBytes(), true);
    }
    //Initializing board
    public void initializeBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = null;
            }
        }
    }

    //    @Override
//    public void run() {
//
//    }
    //Function to get turn for multiplayer game
    public boolean getMyTurn(Client client) {
        System.out.println("Client " + client.count + " in get my turn: " + client);
        return client.equals(currentPlayer);
    }
    //Function to get turn for single player game
    public boolean getMyTurn() {
        if (random.nextBoolean()) {
            currentPlayer = singlePlayer;
            notCurrentPlayer = computer;
            singlePlayer.setPiece("X");
            computerPiece = "O";
            computer.setPiece("O");
            return true;
        } else {
            currentPlayer = computer;
            notCurrentPlayer = singlePlayer;
            singlePlayer.setPiece("O");
            computerPiece = "X";
            computer.setPiece("X");
            return false;
        }
    }
    //Function to swap turns after each move
    public void swapTurn() {
        if (isSinglePlayer) {
            if (currentPlayer.equals(computer)) {
                currentPlayer = singlePlayer;
                notCurrentPlayer = computer;
            } else {
                currentPlayer = computer;
                notCurrentPlayer = singlePlayer;
            }
        } else {
            if (currentPlayer.equals(player1)) {
                currentPlayer = player2;
                notCurrentPlayer = player1;
            } else {
                currentPlayer = player1;
                notCurrentPlayer = player2;
            }
        }

    }
    //Function to convert move(int) to string
    public String convertMove() {
        return (String.valueOf(this.move));
    }
    //Function to set the move to be later used to update board
    public void setMove(int move) {
        this.move = move;
    }

    public String makeMove() {
        //DO SOMETHING HERE?!?!
        return null;
    }
    //Function to update the board in the server
    public void updateServerBoard() {
        if (this.move <= 3) {
            this.board[0][move - 1] = currentPlayer.getPiece();
        } else if (this.move <= 6) {
            this.board[1][move - 4] = currentPlayer.getPiece();
        } else if (this.move <= 9) {
            this.board[2][move - 7] = currentPlayer.getPiece();
        }
    }
    //Function to update the board in the 2 clients by sending them the move made.
    public void updateClientBoards() throws IOException {
        //Print statements for logging purposes(finding and fixing errors)
        System.out.println(player1.getUsername() + " Writer: " + player1.getWriter());
        System.out.println(player2.getUsername() + " Writer: " + player2.getWriter());
        currentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("Dummy," + convertMove()).getBytes(), true);
        System.out.println("Player 1 updated!");
        notCurrentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("Dummy," + convertMove()).getBytes(), true);
        System.out.println("Player 2 updated!");
        System.out.println("Move: " + move);
    }
    //Function to check win
    public boolean checkWin() {
        printBoard();
        // Check all rows
        for (int i = 0; i < SIZE; i++) {
            if (board[i][0] != null && board[i][0].equals(board[i][1]) && board[i][1].equals(board[i][2])) {
                return true;
            }
        }

        // Check all columns
        for (int j = 0; j < SIZE; j++) {
            if (board[0][j] != null && board[0][j].equals(board[1][j]) && board[1][j].equals(board[2][j])) {
                return true;
            }
        }

        // Check diagonal (top-left to bottom-right)
        if (board[0][0] != null && board[0][0].equals(board[1][1]) && board[1][1].equals(board[2][2])) {
            return true;
        }

        // Check diagonal (top-right to bottom-left)
        if (board[0][2] != null && board[0][2].equals(board[1][1]) && board[1][1].equals(board[2][0])) {
            return true;
        }

        // No winner found
        return false;
    }
    //Function to check draw

    public boolean checkDraw() {

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                // If there's an empty space, then it's not a draw
                if (board[i][j] == null) {
                    return false;
                }
            }
        }
        // If no empty spaces, it's a draw
        return true;
    }
    //Function to end game which will let the players know games ended and call end game from server
    public void endGame(boolean win) throws Exception {
        //If someone won
        if (win) {
            if (isSinglePlayer) {
                if (currentPlayer.equals(singlePlayer)) {
                    singlePlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("WINNER," + convertMove()).getBytes(), true);
                } else {
                    System.out.println("Computer won");
                    singlePlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("LOSER," + convertMove()).getBytes(), true);
                }
            } else {
                if (player1.equals(currentPlayer)) {
                    currentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("WINNER," + convertMove()).getBytes(), true);
                    notCurrentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("LOSER," + convertMove()).getBytes(), true);
                } else {
                    currentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("WINNER," + convertMove()).getBytes(), true);
                    notCurrentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("LOSER," + convertMove()).getBytes(), true);
                }
            }

        }
        //If its a draw
        else {
            if (isSinglePlayer) {
                singlePlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("DRAW," + convertMove()).getBytes(), true);
            } else {
                currentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("DRAW," + convertMove()).getBytes(), true);
                notCurrentPlayer.writeResponse("HTTP/1.1 200 OK", "text/plain", ("DRAW," + convertMove()).getBytes(), true);
            }
        }
        // Schedule the next step to be executed after 5 seconds without blocking
        scheduler.schedule(() -> {
            try {
                server.endGame(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, TimeUnit.SECONDS);
    }
    //Function to printboard to terminal for logging purposes
    public void printBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.out.print((board[i][j] == null ? "-" : board[i][j]) + " ");
            }
            System.out.println();
        }
    }
    //Function to check if rematch queue is empty
    public boolean isRematchEmpty() {
        return this.rematchRequest.isEmpty();
    }
    //Function to get the player who's requested for rematch from queue
    public Client getRematch() {
        return this.rematchRequest.poll();
    }
    //Add player who has requested rematch to queue
    public void addRematchRequest(Client client) {
        this.rematchRequest.add(client);
    }
    //Function to get your opponents name provided your Client instance
    public String getOpponentName(Client client) {
        if (client.equals(player1)) {
            return player2.username;
        } else {
            return player1.username;
        }
    }
    //Function to get opponents client instance
    public Client getOpponent(Client client) {
        if (client.equals(player1)) {
            return player2;
        } else {
            return player1;
        }
    }
    //Function to check if the player is in multiplayer game
    public boolean PlayerInMultGame(String username) {
        return username.equals(player1Name) || username.equals(player2Name);
    }
    //Function to check if the player is in single player game
    public boolean PlayerInSingleGame(String username) {
        return username.equals(singlePlayerName);
    }
    //Function to make the player leave the game
    public void setLeave(Client client) {
        if (playerLeaveCount >= 1) {
            server.endGame(this);
            return;
        }
        if (client.equals(player1)) {
            player1Left = true;
        } else {
            player2Left = true;
        }
        playerLeaveCount++;
    }
    //Function to check if opponent left
    public boolean checkOpponentLeft(Client client) {
        if (player1.equals(client)) {
            return player2Left;
        } else {
            return player1Left;
        }
    }
    //Might need this!?!?
    public Client getPlayerSameUsername(String username) {
        if (username.equals(player1Name)) {
            return player1;
        } else {
            return player2;
        }
    }
    //Function to check if the game is Live meaning both players are in it and active.
    public boolean checkGameIsLive(String username, String username1) {
        return ((username.equals(player1Name)) && username1.equals(player2Name)) || ((username.equals(player2Name)) && username1.equals(player1Name));
    }
    //Same as the above but for single player game
    public boolean checkGameIsLive(String username) {
        return username.equals(singlePlayerName);
    }
    //Function to get all players in the game
    public List<Client> getPlayers() {
        List<Client> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);
        return players;
    }
    //Supplement function to use for A.I algorithm we got from geeks for geeks + https://www.youtube.com/watch?v=trKjYdBASyQ&t=705s
    public String checkWinner() {
        //printBoard();
        // Check all rows
        for (int i = 0; i < SIZE; i++) {
            if (board[i][0] != null && board[i][0].equals(board[i][1]) && board[i][1].equals(board[i][2])) {
                return board[i][0];
            }
        }

        // Check all columns
        for (int j = 0; j < SIZE; j++) {
            if (board[0][j] != null && board[0][j].equals(board[1][j]) && board[1][j].equals(board[2][j])) {
                return board[0][j];
            }
        }

        // Check diagonal (top-left to bottom-right)
        if (board[0][0] != null && board[0][0].equals(board[1][1]) && board[1][1].equals(board[2][2])) {
            return board[0][0];
        }

        // Check diagonal (top-right to bottom-left)
        if (board[0][2] != null && board[0][2].equals(board[1][1]) && board[1][1].equals(board[2][0])) {
            return board[0][2];
        }

        // No winner found
        return null;
    }
    //Minimax function to find best move for A.I
    private int minimax(String[][] board, int depth, boolean isMax) {
        String result = checkWinner();
        if (result != null) {
            if (result.equals(computerPiece)) {
                return 10 - depth;  // Prefer quicker wins
            } else if (result.equals(singlePlayer.getPiece())) {
                return depth - 10;  // Prefer slower losses
            }
        }

        if (!isMovesLeft(board)) {
            return 0;  // It's a draw
        }

        if (isMax) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (board[i][j] == null) {
                        board[i][j] = computerPiece;
                        int score = minimax(board, depth + 1, false);
                        board[i][j] = null;
                        bestScore = Math.max(score, bestScore);
                    }
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (board[i][j] == null) {
                        board[i][j] = singlePlayer.getPiece();
                        int score = minimax(board, depth + 1, true);
                        board[i][j] = null;
                        bestScore = Math.min(score, bestScore);
                    }
                }
            }
            return bestScore;
        }
    }
    //Supplement function for minimax()
    private boolean isMovesLeft(String[][] board) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == null) {
                    return true;
                }
            }
        }
        return false;
    }
    //Function to use to find best move which uses minimax.
    public int findBestMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] pos = new int[2];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == null) {
                    board[i][j] = computerPiece;
                    int score = minimax(board, 0, false);
                    board[i][j] = null;
                    if (score > bestScore) {
                        bestScore = score;
                        pos[0] = i;
                        pos[1] = j;
                    }
                }
            }
        }
        return convertToSingleIndex(pos[0], pos[1]);
    }
    //Function to use to convert the 2d position (board[i][j]) into a single digit to be used to update the clients board which is buttons from 1-9.
    private int convertToSingleIndex(int row, int col) {
        if(row == 0){
            return col+1;
        }
        else if(row ==1){
            return col+4;
        }
        else{
            return col+7;
        }
    }
}
















//    public int findBestMove(){
//        int bestScore = Integer.MIN_VALUE;
//        int[] pos = new int[2];
//        for(int i = 0;i < SIZE;i++){
//            for(int j = 0;j < SIZE;j++){
//                if(board[i][j] == null){
//                    board[i][j] = computerPiece;
//                    int score = minimax(board,0,false);
//                    board[i][j] = null;
//                    if(score > bestScore){
//                        pos[0] = i;
//                        pos[1] = j;
//                    }
//                }
//            }
//        }
//        int i = pos[0];
//        int j = pos[1];
//        //board[i][j] = computerPiece;
//        if(i == 0){
//            j++;
//            return j;
//        }
//        else if(i == 1){
//            j = j + 4;
//            return j;
//        }
//        else{
//            j = j + 7;
//            return j;
//        }
//    }
//    private int minimax(String[][] board,int depth,boolean isMax){
//        String result = checkWinnner();
//        int score = 0;
//        if(result != null){
//            if(result == computerPiece){
//                score++;
//                return score;
//            }
//            else{
//                score--;
//                return score;
//            }
//        }
//        if(isMax){
//            int bestScore = Integer.MIN_VALUE;
//            for(int i = 0;i < SIZE;i++){
//                for(int j = 0;j < SIZE;j++){
//                    if(board[i][j] == null){
//                        board[i][j] = computerPiece;
//                        int score1 = minimax(board,depth++,false);
//                        board[i][j] = null;
//                        if(score1 > bestScore){
//                            bestScore = score1;
//                        }
//                    }
//                }
//            }
//            return bestScore;
//        }
//        else{
//            int bestScore1 = Integer.MAX_VALUE;
//            for(int i = 0;i < SIZE;i++){
//                for(int j = 0;j < SIZE;j++){
//                    if(board[i][j] == null){
//                        board[i][j] = singlePlayer.getPiece();
//                        int score2 = minimax(board,depth++,true);
//                        board[i][j] = null;
//                        if(score2 < bestScore1){
//                            bestScore1 = score2;
//                        }
//                    }
//                }
//            }
//            return bestScore1;
//        }
//    }
//}
