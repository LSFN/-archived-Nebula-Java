package org.lsfn.nebula;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Vector2;
import org.lsfn.nebula.FF.*;

/**
 * Everything to do with the game itself is managed through this class.
 * @author Lukeus_Maximus
 *
 */
public class GameManager extends Thread {

    private static final Integer pollWait = 50;
    
    private StarshipServer starshipServer;
    private StarshipManager starshipManager;
    private ShipManager shipManager;
    private AsteroidManager asteroidManager;
    private World physicsWorld;
    
    private boolean running;
    private boolean gameInProgress;
    
    public GameManager(StarshipServer starshipServer) {
        // Setup the Starship management classes
        this.starshipServer = starshipServer;
        this.starshipManager = new StarshipManager();
        
        // Setup the physical world
        // courtesy of dyn4j.
        // TODO Set world bounds
        this.physicsWorld = new World();
        // Because we're in space...
        this.physicsWorld.setGravity(World.ZERO_GRAVITY);
        Settings settings = new Settings();
        settings.setStepFrequency(0.05);
        this.physicsWorld.setSettings(settings);
        
        // Setup our game classes
        this.asteroidManager = new AsteroidManager(this.physicsWorld);
        this.shipManager = new ShipManager(this.physicsWorld, this.asteroidManager);
        
        // The class Thread isn't running
        this.running = false;
        // Nor is the game in progress
        this.gameInProgress = false;
    }
    
    private void handleNewConnections() {
        List<UUID> newConnections = this.starshipServer.getConnectedStarships();
        for(UUID id : newConnections) {
            if(!this.gameInProgress) {
                this.starshipManager.addStarship(id);
            }
        }
    }
    
    private void handleDisconnections() {
        List<UUID> newDisconnections = this.starshipServer.getDisconnectedStarships();
        for(UUID id : newDisconnections) {
            this.starshipManager.removeStarship(id);
        }
    }

    private void processInput() {
        Map<UUID, List<FFup>> messages = this.starshipServer.receiveMessagesFromConsoles();
        for(UUID id : messages.keySet()) {
            int i = 0;
            for(FFup upMessage : messages.get(id)) {
                if(upMessage.hasRcon()) {
                    // TODO handle RCon
                }
                if(upMessage.hasLobby()) {
                    if(!this.gameInProgress) {
                        this.starshipManager.processInput(id, upMessage.getLobby());
                    }
                }
                if(upMessage.hasPiloting()) {
                    if(this.gameInProgress) {
                        this.shipManager.processInput(id, upMessage.getPiloting());
                    }
                }
            }
        }
    }
    
    private void gameTick() {
        if(gameInProgress) {
            this.shipManager.tick();
            this.physicsWorld.update(0.05);
        } else {
            if(this.starshipManager.isEveryoneReady()) {
                // Start the game
                // Let the starships know that the game has started
                for(UUID id : this.starshipManager.getIDs()) {
                    FFdown.Lobby lobby = this.starshipManager.generateOutput(id);
                    if(lobby != null) {
                        FFdown.Builder builder = FFdown.newBuilder();
                        builder.setLobby(lobby);
                        this.starshipServer.sendMessageToStarship(id, builder.build());
                    }
                }
                // Setup the game
                for(UUID id : this.starshipManager.getIDs()) {
                    this.shipManager.addShip(id);
                }
                this.asteroidManager.addAsteroid(new Vector2(0.0, 5.0));
                this.asteroidManager.addAsteroid(new Vector2(0.0, 10.0));
                this.asteroidManager.addAsteroid(new Vector2(0.0, 15.0));
                this.asteroidManager.addAsteroid(new Vector2(0.0, 20.0));
                // Remind this class that the game has actually started
                this.gameInProgress = true;
                System.out.println("The game has begun.");
            }
        }
    }
    
    private void dispatchOutput() {
        if(gameInProgress) {
            for(UUID id : this.starshipManager.getIDs()) {
                FFdown.Builder builder = FFdown.newBuilder();
                builder.setVisualSensors(this.shipManager.generateOutput(id));
                this.starshipServer.sendMessageToStarship(id, builder.build());
            }
        } else {
            // Lobby
            for(UUID id : this.starshipManager.getIDs()) {
                FFdown.Lobby lobby = this.starshipManager.generateOutput(id);
                // This if statement will get additional clauses so it won't be redundant.
                if(lobby != null) {
                    FFdown.Builder builder = FFdown.newBuilder();
                    if(lobby != null) {
                        builder.setLobby(lobby);
                    }
                    this.starshipServer.sendMessageToStarship(id, builder.build());
                }
            }
        }
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
