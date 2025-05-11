package server;

import common.Board;
import common.ShotResult;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleshipServer extends Thread {
    private static final int PORT = 12345;
    private static final int BOARD_SIZE = 10;
    
    private final List<PlayerHandler> players = new ArrayList<>();
    private int currentPlayerIndex;
    private final AtomicBoolean gameStarted = new AtomicBoolean(false);
    private final AtomicBoolean gameOver = new AtomicBoolean(false);
    
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            
            // Wait for 2 players
            while (players.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                PlayerHandler player = new PlayerHandler(this, clientSocket, players.size() + 1, BOARD_SIZE);
                players.add(player);
                new Thread(player).start();
                System.out.println("Player " + players.size() + " connected");
            }
            
            // Start game when both ready
            waitForPlayersReady();
            
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private void waitForPlayersReady() {
        while (!allPlayersReady()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        startGame();
    }
    
    private boolean allPlayersReady() {
        synchronized(players) {
            return players.stream().allMatch(PlayerHandler::isReady);
        }
    }
    
    private void startGame() {
        gameStarted.set(true);
        broadcast("GAME_START");
        notifyCurrentPlayer();
        System.out.println("Game started!");
    }
    
    public synchronized void processShot(int playerId, int x, int y) {
        if (!gameStarted.get() || gameOver.get() || playerId != currentPlayerIndex + 1) {
            return;
        }
        
        PlayerHandler opponent = players.get(1 - currentPlayerIndex);
        ShotResult result = opponent.getBoard().receiveShot(x, y);
        
        // Debug logging
        System.out.printf("Player %d shot at (%d,%d) - Result: %s%n", 
            playerId, x, y, result);
        
        // Send results
        sendToPlayer(currentPlayerIndex, "SHOT_RESULT:" + x + "," + y + "," + result);
        sendToPlayer(1 - currentPlayerIndex, "OPPONENT_SHOT:" + x + "," + y + "," + result);
        
        // Check game over
        if (result == ShotResult.GAME_OVER || opponent.getBoard().allShipsSunk()) {
            endGame(playerId);
            return;
        }
        
        // Switch turn on miss
        if (result == ShotResult.MISS) {
            switchTurn();
        }else {
        // Immediately notify current player they can shoot again
        sendToPlayer(currentPlayerIndex, "TURN:" + (currentPlayerIndex + 1));
    }
    }
    
    private void sendToPlayer(int index, String message) {
        if (index >= 0 && index < players.size()) {
            players.get(index).sendMessage(message);
        }
    }
    
    private void endGame(int winnerId) {
        gameOver.set(true);
        broadcast("GAME_OVER:" + winnerId);
        System.out.println("Game over! Winner: Player " + winnerId);
        
        // Close connections
        players.forEach(player -> {
            player.closeConnection();
        });
    }
    
    private void switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex;
        notifyCurrentPlayer();
    }
    
    private void notifyCurrentPlayer() {
        broadcast("TURN:" + (currentPlayerIndex + 1));
    }
    
    private void broadcast(String message) {
        synchronized(players) {
            players.forEach(player -> player.sendMessage(message));
        }
    }
    
    public synchronized void removePlayer(PlayerHandler player) {
        players.remove(player);
        if (gameStarted.get() && !gameOver.get()) {
            int winnerId = players.isEmpty() ? 0 : players.get(0).getPlayerId();
            endGame(winnerId);
        }
    }
    
    public static void main(String[] args) {
        new BattleshipServer().startServer();
    }
}
