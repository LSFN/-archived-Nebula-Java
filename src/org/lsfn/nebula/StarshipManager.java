package org.lsfn.nebula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.lsfn.nebula.STS.STSdown;
import org.lsfn.nebula.STS.STSup;
import org.lsfn.nebula.STS.STSup.Join.JoinType;
import org.lsfn.nebula.StarshipServer.ServerStatus;

public class StarshipManager {
    
    private static final long joinTimeout = 5000;
    
    private StarshipServer starshipServer;
    private Map<UUID, Long> timesConnected;
    private Map<UUID, UUID> connectionToStarshipIDMap;
    private List<UUID> newStarships;
    private boolean allowJoins;
    
    public StarshipManager() {
        this.starshipServer = new StarshipServer();
        this.timesConnected = new HashMap<UUID, Long>();
        this.connectionToStarshipIDMap = new HashMap<UUID, UUID>();
        this.newStarships = new ArrayList<UUID>();
        this.allowJoins = true;
    }
    
    public void attemptStarshipServerStart() {
        if(this.starshipServer.listen() == ServerStatus.OPEN) {
            this.starshipServer.start();
            System.out.println("Starship server started.");
        } else {
            System.out.println("Failed to start Starship server.");
        }
    }
    
    public void handleConnections() {
        for(UUID conn : this.starshipServer.getConnectedStarships()) {
            this.timesConnected.put(conn, System.currentTimeMillis());
        }
    }
    
    public void handleDisconnections() {
        for(UUID conn : this.starshipServer.getDisconnectedStarships()) {
            this.timesConnected.remove(conn);
        }
    }
    
    public List<UUID> getNewStarships() {
        List<UUID> result = this.newStarships;
        this.newStarships = new ArrayList<UUID>();
        return result;
    }
    
    private void acceptJoin(UUID conn) {
        UUID newID = UUID.randomUUID();
        STSdown.Builder stsDown = STSdown.newBuilder();
        STSdown.Join.Builder stsDownJoin = STSdown.Join.newBuilder();
        stsDownJoin.setResponse(STSdown.Join.Response.JOIN_ACCEPTED);
        stsDownJoin.setRejoinToken(newID.toString());
        stsDown.setJoin(stsDownJoin);
        this.starshipServer.sendMessageToStarship(conn, stsDown.build());
        this.timesConnected.remove(conn);
        this.connectionToStarshipIDMap.put(conn, newID);
        this.newStarships.add(newID);
    }
    
    private void rejectJoin(UUID conn) {
        STSdown.Builder stsDown = STSdown.newBuilder();
        STSdown.Join.Builder stsDownJoin = STSdown.Join.newBuilder();
        stsDownJoin.setResponse(STSdown.Join.Response.JOIN_REJECTED);
        stsDown.setJoin(stsDownJoin);
        this.starshipServer.sendMessageToStarship(conn, stsDown.build());
        this.starshipServer.disconnectStarship(conn);
        this.timesConnected.remove(conn);
    }

    private void acceptRejoin(UUID conn, UUID rejoinToken) {
        STSdown.Builder stsDown = STSdown.newBuilder();
        STSdown.Join.Builder stsDownJoin = STSdown.Join.newBuilder();
        stsDownJoin.setResponse(STSdown.Join.Response.REJOIN_ACCEPTED);
        stsDown.setJoin(stsDownJoin);
        this.starshipServer.sendMessageToStarship(conn, stsDown.build());
        Set<UUID> connections = new HashSet<UUID>(this.connectionToStarshipIDMap.keySet());
        for(UUID connection : connections) {
            if(this.connectionToStarshipIDMap.get(connection).equals(rejoinToken)) {
                this.connectionToStarshipIDMap.remove(connection);
                this.starshipServer.disconnectStarship(connection);
            }
        }
        this.connectionToStarshipIDMap.put(conn, rejoinToken);
        this.timesConnected.remove(conn);
    }
    
    private void processJoin(UUID conn, STSup.Join join) {
        if(join.getType() == JoinType.JOIN) {
            if(!this.connectionToStarshipIDMap.containsKey(conn)) {
                if(this.allowJoins) {
                    // Setup a new StarshipInfo
                    acceptJoin(conn);
                } else {
                    // Refuse the joining Starship
                    rejectJoin(conn);
                }
            }
        } else if(join.getType() == JoinType.REJOIN && join.hasRejoinToken()) {
            UUID rejoinToken = UUID.fromString(join.getRejoinToken());
            if(this.connectionToStarshipIDMap.containsValue(rejoinToken)) {
                // Rejoin the Starship overriding previous mapping
                acceptRejoin(conn, rejoinToken);
            }
        }
    }
    
    private void checkJoinTimeouts() {
        Set<UUID> connections = new HashSet<UUID>(this.timesConnected.keySet());
        for(UUID conn : connections) {
            if(System.currentTimeMillis() >= this.timesConnected.get(conn) + joinTimeout) {
                this.timesConnected.remove(conn);
                this.starshipServer.disconnectStarship(conn);
            }
        }        
    }
    
    public Map<UUID, List<STSup>> processAndRemapMessages() {
        Map<UUID, List<STSup>> allMessages = this.starshipServer.receiveMessagesFromConsoles();
        Map<UUID, List<STSup>> remappedMessages = new HashMap<UUID, List<STSup>>();
        for(UUID conn : allMessages.keySet()) {
            List<STSup> messages = allMessages.get(conn);
            for(STSup message : messages) {
                if(message.hasJoin()) {
                    processJoin(conn, message.getJoin());
                }
            }
            UUID starshipID = this.connectionToStarshipIDMap.get(conn);
            if(starshipID != null) {
                remappedMessages.put(starshipID, messages);
            }
        }
        return remappedMessages;
    }
    
    public Map<UUID, List<STSup>> getInput() {
        handleConnections();
        handleDisconnections();
        checkJoinTimeouts();
        return processAndRemapMessages();
    }

    public void sendMessageToStarship(UUID conn, STSdown downMessage) {
        this.starshipServer.sendMessageToStarship(conn, downMessage);
    }
}
