package org.lsfn.nebula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dyn4j.dynamics.World;

/**
 * Everything to do with the game itself is managed through this class.
 * @author Lukeus_Maximus
 *
 */
public class GameManager extends Thread {

    private static final Integer pollWait = 50;
    
    private StarshipServer starshipServer;
    private Map<UUID, StarshipInfo> starships;
    private World physicsWorld;
    
    private boolean running;
    private boolean gameInProgress;
    
    public GameManager(StarshipServer starshipServer) {
        this.starshipServer = starshipServer;
        this.starships = new HashMap<UUID, StarshipInfo>();
        this.physicsWorld = null;
        
        this.running = false;
        this.gameInProgress = false;
    }
    
    private void handleNewConnections() {
        List<UUID> newConnections = this.starshipServer.getConnectedStarships();
        for(UUID id : newConnections) {
            if(!this.gameInProgress && !this.starships.containsKey(id)) {
                this.starships.put(id, new StarshipInfo(id));
            }
        }
    }
    
    private void handleDisconnections() {
        List<UUID> newDisconnections = this.starshipServer.getDisconnectedStarships();
        for(UUID id : newDisconnections) {
            if(this.starships.containsKey(id)) {
                this.starships.remove(id);
            }
        }
    }

    private void processInput() {
    
    }
    
    private void gameTick() {
        
    }
    
    private void dispatchOutput() {
        
    }
    
    @Override
    public void run() {
        // This is the main game loop!
        // It runs even when a game is not in progress, i.e. in the lobby
        this.running = true;
        while(this.running) {
            // Handle new connections
            handleNewConnections();
            // Handle disconnections
            handleDisconnections();
            // Process input
            processInput();
            // Game update step
            gameTick();
            // Dispatch output
            dispatchOutput();
            
            // Wait for the next iteration
            try {
                // TODO use time between updates for more consistent looping
                Thread.sleep(pollWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}
