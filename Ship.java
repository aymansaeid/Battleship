package common;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    private final int size;
    private int hits;
    private final List<int[]> coordinates;
    private boolean sunk;
    
    public Ship(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Ship size must be positive");
        }
        this.size = size;
        this.hits = 0;
        this.coordinates = new ArrayList<>();
        this.sunk = false;
    }
    
    public synchronized void hit() {
        if (!sunk && hits < size) {
            hits++;
            if (hits >= size) {
                sunk = true;
            }
        }
    }
    
    public boolean isSunk() { return sunk; }
    public int getSize() { return size; }
    public int getHits() { return hits; }
    
    public void addCoordinate(int x, int y) {
        if (coordinates.size() < size) {
            coordinates.add(new int[]{x, y});
        }
    }
    
    public List<int[]> getCoordinates() {
        return new ArrayList<>(coordinates);
    }
    
    @Override
    public String toString() {
        return String.format("Ship[size=%d, hits=%d, sunk=%b]", 
            size, hits, sunk);
    }
}