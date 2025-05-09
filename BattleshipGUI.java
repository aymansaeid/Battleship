package client;

import common.Ship;
import common.ShotResult;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class BattleshipGUI extends JFrame {
    private final BattleshipClient client;
    private int playerId;
    private boolean myTurn;
    private boolean placingShips = true;
    
    private JPanel myBoardPanel;
    private JPanel opponentBoardPanel;
    private JLabel statusLabel;
    private JPanel controlPanel;
    private JButton readyButton;
    
    private enum ShipType {
        CARRIER(5), BATTLESHIP(4), CRUISER(3), SUBMARINE(2);
        final int size;
        ShipType(int size) { this.size = size; }
    }
    
    private final Map<ShipType, JButton> shipButtons = new EnumMap<>(ShipType.class);
    private final Map<ShipType, Boolean> shipsPlaced = new EnumMap<>(ShipType.class);
    private final List<Ship> placedShips = new ArrayList<>();
    private ShipType selectedShipType;
    private boolean isVertical;
    
    public BattleshipGUI(BattleshipClient client) {
        this.client = client;
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Battleship Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLayout(new BorderLayout());
        
        initializeShipTracking();
        createGamePanels();
        createControlPanel();
        
        setVisible(true);
    }
    
    private void initializeShipTracking() {
        for (ShipType type : ShipType.values()) {
            shipsPlaced.put(type, false);
        }
    }
    
    private void createGamePanels() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        
        myBoardPanel = createBoardPanel(false);
        myBoardPanel.setBorder(BorderFactory.createTitledBorder("Your Board"));
        
        opponentBoardPanel = createBoardPanel(true);
        opponentBoardPanel.setBorder(BorderFactory.createTitledBorder("Opponent Board"));
        
        mainPanel.add(myBoardPanel);
        mainPanel.add(opponentBoardPanel);
        
        add(mainPanel, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Connecting to server...", JLabel.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout());
        
        for (ShipType type : ShipType.values()) {
            JButton btn = new JButton(type.name() + " (" + type.size + ")");
            btn.addActionListener(e -> selectShip(type));
            controlPanel.add(btn);
            shipButtons.put(type, btn);
        }
        
        JButton rotateBtn = new JButton("Rotate (Vertical: OFF)");
        rotateBtn.addActionListener(e -> toggleRotation(rotateBtn));
        controlPanel.add(rotateBtn);
        
        readyButton = new JButton("Ready");
        readyButton.setVisible(false);
        readyButton.addActionListener(e -> handleReady());
        controlPanel.add(readyButton);
        
        add(controlPanel, BorderLayout.NORTH);
    }
    
    private JPanel createBoardPanel(boolean isOpponent) {
        JPanel panel = new JPanel(new GridLayout(10, 10));
        
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(30, 30));
                final int x = i, y = j;
                
                button.addActionListener(e -> handleBoardClick(x, y, isOpponent, button));
                panel.add(button);
            }
        }
        return panel;
    }
    
    private void handleBoardClick(int x, int y, boolean isOpponent, JButton button) {
        if (isOpponent) {
            if (!placingShips && myTurn) {
                client.sendShot(x, y);
            }
        } else {
            if (placingShips && selectedShipType != null) {
                placeShipOnBoard(x, y, selectedShipType, isVertical, button);
            }
        }
    }
    
    private void selectShip(ShipType type) {
        selectedShipType = type;
        statusLabel.setText("Selected: " + type.name() + " - Click on your board to place");
    }
    
    private void toggleRotation(JButton rotateBtn) {
        isVertical = !isVertical;
        rotateBtn.setText("Rotate (Vertical: " + (isVertical ? "ON" : "OFF"));
    }
    
    private void placeShipOnBoard(int x, int y, ShipType shipType, boolean vertical, JButton button) {
        Ship ship = new Ship(shipType.size);
        if (client.getMyBoard().placeShip(ship, x, y, vertical)) {
            placedShips.add(ship);
            shipsPlaced.put(shipType, true);
            highlightShipCells(ship, x, y, vertical);
            updateShipButtons();
            checkReadyCondition();
            selectedShipType = null;
        } else {
            JOptionPane.showMessageDialog(this, "Cannot place ship here!");
        }
    }
    
    private void highlightShipCells(Ship ship, int x, int y, boolean vertical) {
        Color color = getShipColor(ship.getSize());
        for (int i = 0; i < ship.getSize(); i++) {
            int cellX = vertical ? x + i : x;
            int cellY = vertical ? y : y + i;
            JButton cell = (JButton) myBoardPanel.getComponent(cellX * 10 + cellY);
            cell.setBackground(color);
            cell.setText("S");
            cell.setOpaque(true);
        }
    }
    
    private Color getShipColor(int size) {
        return switch (size) {
            case 5 -> new Color(100, 150, 200);
            case 4 -> new Color(150, 200, 100);
            case 3 -> new Color(200, 100, 150);
            case 2 -> new Color(200, 200, 100);
            default -> Color.GRAY;
        };
    }
    
    private void updateShipButtons() {
        shipButtons.forEach((type, btn) -> {
            boolean placed = shipsPlaced.get(type);
            btn.setEnabled(!placed);
            btn.setBackground(placed ? Color.LIGHT_GRAY : null);
        });
    }
    
    private void checkReadyCondition() {
        boolean allPlaced = shipsPlaced.values().stream().allMatch(b -> b);
        readyButton.setVisible(allPlaced);
        if (allPlaced) {
            client.sendShipPlacements(placedShips);
        }
    }
    
    private void handleReady() {
        if (placedShips.size() == ShipType.values().length) {
            client.sendReadySignal();
            placingShips = false;
            controlPanel.remove(readyButton);
            controlPanel.revalidate();
            controlPanel.repaint();
        }
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        setTitle("Battleship - Player " + playerId);
        statusLabel.setText("Place your ships!");
    }
    
    public void setTurn(boolean myTurn) {
        this.myTurn = myTurn;
        statusLabel.setText(myTurn ? "Your turn! Attack opponent's board" : "Opponent's turn...");
    }
    
    public void updateOpponentBoard(int x, int y, ShotResult result) {
        SwingUtilities.invokeLater(() -> updateBoard(opponentBoardPanel, x, y, result, true));
    }
    
    public void updateMyBoard(int x, int y, ShotResult result) {
        SwingUtilities.invokeLater(() -> updateBoard(myBoardPanel, x, y, result, false));
    }
    
    private void updateBoard(JPanel panel, int x, int y, ShotResult result, boolean disable) {
        try {
            JButton button = (JButton) panel.getComponent(x * 10 + y);
            button.setOpaque(true);
            button.setBorderPainted(false);
            
            if (result.isHit()) {
                button.setBackground(new Color(255, 100, 100));
                button.setForeground(Color.WHITE);
                button.setText("X");
                if (result == ShotResult.SUNK) {
                    button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                }
            } else {
                button.setBackground(new Color(200, 230, 255));
                button.setForeground(Color.DARK_GRAY);
                button.setText("•");
            }
            
            button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
            if (disable) button.setEnabled(false);
            button.repaint();
        } catch (Exception e) {
            System.err.println("Error updating board: " + e.getMessage());
        }
    }
    
    public void gameStarted() {
        statusLabel.setText("Game started! Waiting for turn...");
    }
    
    public void gameOver(boolean won) {
        statusLabel.setText(won ? "You won! Game over." : "You lost! Game over.");
        disableGame();
        
        JOptionPane.showMessageDialog(this,
            won ? "Congratulations! You won!" : "Game over! You lost.",
            "Game Finished",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void disableGame() {
        Arrays.stream(opponentBoardPanel.getComponents())
              .filter(c -> c instanceof JButton)
              .forEach(c -> c.setEnabled(false));
    }
    
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}