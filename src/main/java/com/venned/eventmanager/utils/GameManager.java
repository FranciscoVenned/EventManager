package com.venned.eventmanager.utils;

public class GameManager {
    private GameState gameState;
    private int currentPhase;

    public GameManager() {
        this.gameState = GameState.WAITING;
        this.currentPhase = 1;
    }

    public void startGame() {
        this.gameState = GameState.RUNNING;
    }

    public void endGame() {
        this.gameState = GameState.ENDED;
    }

    public boolean isWaiting() {
        return this.gameState == GameState.WAITING;
    }

    public boolean isRunning() {
        return this.gameState == GameState.RUNNING;
    }

    public int getCurrentPhase() {
        return this.currentPhase;
    }

    public void setCurrentPhase(int phase) {
        this.currentPhase = phase;
    }

    public boolean isEnded() {
        return this.gameState == GameState.ENDED;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public enum GameState {
        WAITING,
        RUNNING,
        ENDED
    }
}
