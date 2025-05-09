package common;

public class Cell {
    private final int x;
    private final int y;
    private boolean isHit;
    private Ship ship;
    
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.isHit = false;
        this.ship = null;
    }
    
    // Getters
    public boolean hasShip() { return ship != null; }
    public Ship getShip() { return ship; }
    public boolean isHit() { return isHit; }
    public int getX() { return x; }
    public int getY() { return y; }
    
    // Setters
    public void setShip(Ship ship) { 
        if (this.ship == null) {
            this.ship = ship; 
        }
    }
    
    public void setHit(boolean hit) { 
        if (!isHit) {
            this.isHit = hit; 
        }
    }
    
    // Debug info
    @Override
    public String toString() {
        return String.format("Cell[%d,%d] Hit: %b, Ship: %b",
            x, y, isHit, hasShip());
    }
}