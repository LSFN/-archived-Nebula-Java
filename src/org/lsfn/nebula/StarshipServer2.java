package org.lsfn.nebula;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.lsfn.nebula.STS.STSup.Join.JoinType;
import org.lsfn.nebula.STS.*;

/**
 * This class acts as a server for the Starship clients to the Nebula.
 * It runs as a separate thread that listens for new connections
 * and processes received messages. It presents different clients
 * through its methods and *not* different connections. Rejoining clients
 * will show up under the same ID.
 * @author Lukeus_Maximus
 *
 */
public class StarshipServer2 extends Thread {

    private static final Integer defaultPort = 39461;
    private static final Integer tickInterval = 50;
    
    private ServerSocket starshipServer;
    private List<StarshipListener2> unassociatedListeners;
    private List<UUID> unassociatedIDs;
    private Map<UUID, StarshipListener2> clients;
    private boolean open;
    private boolean allowingNewClients;
    
    public StarshipServer2() throws IOException {
        this.starshipServer = new ServerSocket(defaultPort);
        this.unassociatedListeners = new ArrayList<StarshipListener2>();
        this.unassociatedIDs = new ArrayList<UUID>();
        this.clients = new HashMap<UUID, StarshipListener2>();
        this.open = true;
        this.allowingNewClients = true;
    }
    
    public boolean isAllowingNewClients() {
        return allowingNewClients;
    }

    public void setAllowingNewClients(boolean allowingNewClients) {
        this.allowingNewClients = allowingNewClients;
    }

    public boolean isOpen() {
        return this.open;
    }
    
    /**
     * Processes joining info from Starships to see if they join the game.
     * Puts the listener with ID into clients map if they do join.
     * Disconnects the listener if they fail to do so.
     * @param join The join message
     */
    private void processJoin(StarshipListener2 listener, STSup.Join join) {
        STSdown.Builder stsDown = STSdown.newBuilder();
        STSdown.Join.Builder stsDownJoin = STSdown.Join.newBuilder();
        
        if(join.getType() == JoinType.JOIN) {
            if(this.allowingNewClients) {
                stsDownJoin.setResponse(STSdown.Join.Response.JOIN_ACCEPTED);
                UUID newID = UUID.randomUUID();
                this.clients.put(newID, listener);
                stsDownJoin.setRejoinToken(newID.toString());          
                stsDown.setJoin(stsDownJoin);
                listener.sendMessageToStarship(stsDown.build());
            } else {
                stsDownJoin.setResponse(STSdown.Join.Response.JOIN_REJECTED);
                stsDown.setJoin(stsDownJoin);
                listener.sendMessageToStarship(stsDown.build());
                listener.disconnect();
            }
        } else if(join.getType() == JoinType.REJOIN) {
            if(join.hasRejoinToken()) {
                UUID rejoinToken = UUID.fromString(join.getRejoinToken());
                if(this.unassociatedIDs.contains(rejoinToken)) {
                    this.unassociatedIDs.remove(rejoinToken);
                    this.clients.put(rejoinToken, listener);
                    stsDownJoin.setResponse(STSdown.Join.Response.REJOIN_ACCEPTED);
                    stsDown.setJoin(stsDownJoin);
                    listener.sendMessageToStarship(stsDown.build());
                } else {
                    stsDownJoin.setResponse(STSdown.Join.Response.JOIN_REJECTED);
                    stsDown.setJoin(stsDownJoin);
                    listener.sendMessageToStarship(stsDown.build());
                    listener.disconnect();
                }
            }
        }
        
    }
    
    @Override
    public void run() {
        while(this.open) {
            // Process messages from unassociated listeners.
            // Listeners remain in this list until they join the game.
            // Any traffic not relating to joining the game is ignored.
            Iterator<StarshipListener2> listenerIterator = unassociatedListeners.iterator();
            while(listenerIterator.hasNext()) {
                StarshipListener2 listener = listenerIterator.next();
                if(listener.isConnected()) {
                    List<STSup> messages = listener.receiveMessagesFromStarship();
                    for(STSup message : messages) {
                        // TODO failure to join timeouts
                        if(message.hasJoin()) {
                            this.processJoin(listener, message.getJoin());
                            listenerIterator.remove();
                        }
                    }
                } else {
                    listenerIterator.remove();
                }
            }
            // Process messages from listeners associated to IDs
            
            // Accept new connections
        }
    }
}
