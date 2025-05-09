package server;

import common.Board;
import common.Ship;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerHandler implements Runnable {
    private final BattleshipServer server;
    private final Socket socket;
    private final int playerId;
    private final Board board;
    private final PrintWriter out;
    private final BufferedReader in;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    
    public PlayerHandler(BattleshipServer server, Socket socket, int playerId, int boardSize) throws IOException {
        this.server = server;
        this.socket = socket;
        this.playerId = playerId;
        this.board = new Board(boardSize);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    @Override
    public void run() {
        try {
            sendMessage("PLAYER_ID:" + playerId);
            
            // Ship placement phase
            handleShipPlacement();
            
            // Gameplay phase
            handleGameplay();
            
        } catch (IOException e) {
            System.err.println("Player " + playerId + " disconnected: " + e.getMessage());
        } finally {
            server.removePlayer(this);
            closeConnection();
        }
    }
    
    private void handleShipPlacement() throws IOException {
        String message;
        while (!ready.get() && (message = in.readLine()) != null) {
            if ("READY".equals(message)) {
                ready.set(true);
                System.out.println("Player " + playerId + " is ready");
            } else if (message.startsWith("SHIPS:")) {
                handleShipPlacement(message);
            }
        }
    }
    
    private void handleGameplay() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (message.startsWith("SHOT:")) {
                String[] parts = message.substring(5).split(",");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                server.processShot(playerId, x, y);
            }
        }
    }
    
    private void handleShipPlacement(String message) {
        try {
            String[] shipData = message.substring(6).split("\\|");
            for (String shipCoords : shipData) {
                if (shipCoords.isEmpty()) continue;
                
                String[] coords = shipCoords.split(";");
                List<int[]> coordinates = new java.util.ArrayList<>();
                
                for (String coord : coords) {
                    if (coord.isEmpty()) continue;
                    String[] xy = coord.split(",");
                    coordinates.add(new int[]{
                        Integer.parseInt(xy[0]),
                        Integer.parseInt(xy[1])
                    });
                }
                
                if (!coordinates.isEmpty()) {
                    Ship ship = new Ship(coordinates.size());
                    boolean isVertical = isVertical(coordinates);
                    
                    for (int[] coord : coordinates) {
                        if (!board.placeShip(ship, coord[0], coord[1], isVertical)) {
                            System.err.println("Failed to place ship at " + coord[0] + "," + coord[1]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing ship placement: " + e.getMessage());
        }
    }
    
    private boolean isVertical(List<int[]> coords) {
        if (coords.size() < 2) return false;
        return coords.get(0)[0] != coords.get(1)[0];
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    
    public boolean isReady() {
        return ready.get();
    }
    
    public Board getBoard() {
        return board;
    }
    
    public int getPlayerId() {
        return playerId;
    }
}