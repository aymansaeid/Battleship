package client;

import common.Board;
import common.Ship;
import common.ShotResult;
import java.io.*;
import java.net.Socket;
import java.util.List;
import javax.swing.SwingUtilities;

public class BattleshipClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private int playerId;
    private Board myBoard;
    private boolean myTurn;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BattleshipGUI gui;
    
    public BattleshipClient() {
        this.myBoard = new Board(10);
        this.gui = new BattleshipGUI(this);
        connectToServer();
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::listenForServerMessages).start();
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                gui.showError("Connection error: " + e.getMessage());
                gui.dispose();
            });
        }
    }
    
    private void listenForServerMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                gui.showError("Disconnected from server");
                gui.dispose();
            });
        }
    }
    
    private void processServerMessage(String message) {
        if (message.startsWith("PLAYER_ID:")) {
            playerId = Integer.parseInt(message.substring(10));
            SwingUtilities.invokeLater(() -> gui.setPlayerId(playerId));
        } 
        else if (message.startsWith("TURN:")) {
            int turnPlayer = Integer.parseInt(message.substring(5));
            myTurn = (turnPlayer == playerId);
            SwingUtilities.invokeLater(() -> gui.setTurn(myTurn));
        } 
        else if (message.startsWith("SHOT_RESULT:")) {
            String[] parts = message.substring(12).split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            ShotResult result = ShotResult.valueOf(parts[2]);
            SwingUtilities.invokeLater(() -> gui.updateOpponentBoard(x, y, result));
        } 
        else if (message.startsWith("OPPONENT_SHOT:")) {
            String[] parts = message.substring(14).split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            ShotResult result = ShotResult.valueOf(parts[2]);
            SwingUtilities.invokeLater(() -> {
                gui.updateMyBoard(x, y, result);
                if (result == ShotResult.GAME_OVER) {
                    gui.gameOver(false);
                }
            });
        } 
        else if (message.equals("GAME_START")) {
            SwingUtilities.invokeLater(() -> gui.gameStarted());
        } 
        else if (message.startsWith("GAME_OVER:")) {
            int winner = Integer.parseInt(message.substring(10));
            SwingUtilities.invokeLater(() -> {
                gui.gameOver(winner == playerId);
                closeConnection();
            });
        }
    }
    
    public void sendShot(int x, int y) {
        if (myTurn) {
            out.println("SHOT:" + x + "," + y);
            myTurn = false; // Prevent multiple shots
        }
    }
    
    public void sendReadySignal() {
        out.println("READY");
    }
    
    public void sendShipPlacements(List<Ship> ships) {
        StringBuilder shipData = new StringBuilder("SHIPS:");
        for (Ship ship : ships) {
            for (int[] coord : ship.getCoordinates()) {
                shipData.append(coord[0]).append(",").append(coord[1]).append(";");
            }
            shipData.append("|");
        }
        out.println(shipData.toString());
    }
    
    public Board getMyBoard() {
        return myBoard;
    }
    
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BattleshipClient());
    }
}