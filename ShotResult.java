package common;

public enum ShotResult {
    HIT("Hit"), 
    MISS("Miss"), 
    SUNK("Sunk"), 
    GAME_OVER("Game Over"), 
    ALREADY_HIT("Already Hit"), 
    INVALID("Invalid Shot");
    
    private final String description;
    
    ShotResult(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isHit() {
        return this == HIT || this == SUNK || this == GAME_OVER;
    }
}