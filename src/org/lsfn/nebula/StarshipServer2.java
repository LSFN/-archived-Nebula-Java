package org.lsfn.nebula;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

    private static final Integer tickInterval = 50;
    
    private ServerSocket starshipServer;
    private List<StarshipListener2> unassociatedListeners;
    private List<UUID> unassociatedIDs;
    private Map<UUID, StarshipListener2> clients;
    private Map<UUID, List<STSup>> buffers;
    private boolean open;
    private boolean allowingNewClients;
    
    public StarshipServer2() throws IOException {
        this.starshipServer = null;
        this.unassociatedListeners = new ArrayList<StarshipListener2>();
        this.unassociatedIDs = new ArrayList<UUID>();
        this.clients = new HashMap<UUID, StarshipListener2>();
        this.buffers = new HashMap<UUID, List<STSup>>();
        this.open = false;
        this.allowingNewClients = true;
    }
    
    public boolean listen(Integer port) {
        try {
            this.starshipServer = new ServerSocket(port);
            this.starshipServer.setSoTimeout(tickInterval);
            this.open = true;
        } catch (IOException e) {
            e.printStackTrace();
            this.open = false;
        }
        return this.open;
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
    
    public synchronized Map<UUID, List<STSup>> receiveMessagesFromStarships() {
        Map<UUID, List<STSup>> result = buffers;
        buffers = new HashMap<UUID, List<STSup>>();
        return result;
    }
    
    public synchronized void addMessageToBuffers(UUID clientID, STSup message) {
        if(!buffers.containsKey(clientID)) {
            buffers.put(clientID, new ArrayList<STSup>());
        }
        buffers.get(clientID).add(message);
    }
    
    /**
     * Processes joining info from Starships to see if they join the game.
     * Puts the listener with ID into clients map if they do join.
     * Disconnects the listener if they fail to do so.
     * @param join The join message
     * @return true if the listener joined as a client successfully
     */
    private UUID processJoin(StarshipListener2 listener, STSup.Join join) {
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
                return newID;
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
                    return rejoinToken;
                } else {
                    stsDownJoin.setResponse(STSdown.Join.Response.JOIN_REJECTED);
                    stsDown.setJoin(stsDownJoin);
                    listener.sendMessageToStarship(stsDown.build());
                    listener.disconnect();
                }
            }
        }
        return null;
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
                    UUID clientID = null;
                    for(STSup message : messages) {
                        // TODO failure to join timeouts
                        if(clientID != null) {
                            this.addMessageToBuffers(clientID, message);
                        } else if(message.hasJoin()) {
                            listenerIterator.remove();
                            // If join succeeds, submit this message to the corresponding buffer
                            clientID = this.processJoin(listener, message.getJoin());
                            if(clientID != null) {
                                this.addMessageToBuffers(clientID, message);
                            }
                        }
                    }
                } else {
                    listenerIterator.remove();
                }
            }
            // Process messages from listeners associated to IDs
            Iterator<UUID> clientIterator = this.clients.keySet().iterator();
            while(clientIterator.hasNext()) {
                UUID clientID = clientIterator.next();
                StarshipListener2 listener = this.clients.get(clientID);
                if(listener.isConnected()) {
                    List<STSup> messages = listener.receiveMessagesFromStarship();
                    for(STSup message : messages) {
                        this.addMessageToBuffers(clientID, message);
                    }
                } else {
                    // The client has disconnected
                    clientIterator.remove();
                    this.unassociatedIDs.add(clientID);
                }
            }
            // Accept new connections
            Socket starshipSocket = null;
            try {
                starshipSocket = this.starshipServer.accept();
            } catch (SocketTimeoutException e) {
                // Timeouts are normal, do nothing
            } catch (SocketException e) {
                // If the server is closed, it closed because we asked it to.
                if(this.open) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                // Shutdown if anything else goes wrong
                this.open = false;
            }
            
            if(starshipSocket != null) {
                try {
                    StarshipListener2 listener = new StarshipListener2(starshipSocket);
                    this.unassociatedListeners.add(listener);
                } catch (IOException e) {
                    // Listener creation failed
                    // Nothing to do here
                }
            }
        }
    }
}
