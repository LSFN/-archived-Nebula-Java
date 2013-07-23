package org.lsfn.nebula;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.lsfn.nebula.STS.*;
import org.lsfn.nebula.StarshipListener.ListenerStatus;

/**
 * Creates a listener for each connection.
 * Each connection assigned unique ID.
 * ID can be used to receive messages from and to send to a specific console.
 * @author Lukeus_Maximus
 *
 */
public class StarshipServer extends Thread {
    
    private static final Integer defaultPort = 39461;
    private static final Integer pollWait = 50;
    
    private ServerSocket starshipServer;
    private Map<UUID, StarshipListener> listeners;
    private Map<UUID, List<STSup>> buffers;
    private List<UUID> connectedStarships;
    private List<UUID> disconnectedStarships;
    
    public enum ServerStatus {
        CLOSED,
        OPEN
    }
    private ServerStatus serverStatus;
    
    public StarshipServer() {
        clearServer();
        serverStatus = ServerStatus.CLOSED;
    }
    
    public void clearServer() {
        starshipServer = null;
        listeners = null;
        buffers = null;
        connectedStarships = null;
        disconnectedStarships = null;
    }
    
    public ServerStatus getListenStatus() {
        return serverStatus;
    }
    
    public ServerStatus listen() {
        return this.listen(defaultPort);
    }
        
    public ServerStatus listen(int port) {
        if(this.serverStatus == ServerStatus.CLOSED) {
            try {
                this.starshipServer = new ServerSocket(port);
                this.starshipServer.setSoTimeout(pollWait);
                this.listeners = new HashMap<UUID, StarshipListener>();
                this.buffers = new HashMap<UUID, List<STSup>>();
                this.connectedStarships = new ArrayList<UUID>();
                this.disconnectedStarships = new ArrayList<UUID>();
                this.serverStatus = ServerStatus.OPEN;
                System.out.println("Listening on port " + this.starshipServer.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
                clearServer();
                this.serverStatus = ServerStatus.CLOSED;
            }
        }
        return this.serverStatus;
    }
    
    /**
     * Returns a list of the consoles that have connected since last polled.
     * @return List of consoles that have connected.
     */
    public synchronized List<UUID> getConnectedStarships() {
        List<UUID> result = new ArrayList<UUID>(this.connectedStarships);
        this.connectedStarships.clear();
        return result;
    }
    
    private synchronized void addConnectedStarships(UUID id) {
        this.connectedStarships.add(id);
    }
    
    /**
     * Returns a list of the consoles that have disconnected since last polled.
     * @return List of consoles that have disconnected.
     */
    public synchronized List<UUID> getDisconnectedStarships() {
        List<UUID> result = new ArrayList<UUID>(this.disconnectedStarships);
        this.disconnectedStarships.clear();
        return result;
    }
    
    private synchronized void addDisconnectedStarship(UUID id) {
        this.disconnectedStarships.add(id);
    }
    
    public synchronized Map<UUID, List<STSup>> receiveMessagesFromConsoles() {
        Map<UUID, List<STSup>> result = this.buffers;
        
        this.buffers = new HashMap<UUID, List<STSup>>();
        for(UUID id : result.keySet()) {
            this.buffers.put(id, new ArrayList<STSup>());
        }
        
        return result;
    }
    
    private synchronized void addMessagesToBuffer(UUID id, List<STSup> upMessages) {
        this.buffers.get(id).addAll(upMessages);
    }
    
    public void disconnectStarship(UUID id) {
        if(this.listeners.containsKey(id)) {
            this.listeners.get(id).disconnect();
        }
    }
    
    public void sendMessageToStarship(UUID id, STSdown downMessage) {
        StarshipListener listener = getListener(id);
        if(listener != null) {
            listener.sendMessageToStarship(downMessage);
        }
    }
    
    public void sendMessageToAllStarships(STSdown downMessage) {
        Set<UUID> ids = getListenerIDs();
        for(UUID id: ids) {
            StarshipListener listener = getListener(id);
            if(listener != null) {
                listener.sendMessageToStarship(downMessage);
            }
        }
    }
    
    private synchronized void addListener(UUID id, StarshipListener listener) {
        this.listeners.put(id, listener);
        this.buffers.put(id, new ArrayList<STSup>());
    }
    
    private synchronized void removeListener(UUID id) {
        this.listeners.remove(id);
        this.buffers.remove(id);
    }
    
    private synchronized StarshipListener getListener(UUID id) {
        return this.listeners.get(id);
    }
    
    private synchronized Set<UUID> getListenerIDs() {
        return new HashSet<UUID>(this.listeners.keySet());
    }
    
    public ServerStatus shutDown() {
        this.serverStatus = ServerStatus.CLOSED;
        try {
            this.starshipServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.serverStatus;
    }
    
    private void internalShutDown() {
        if(!this.starshipServer.isClosed()) {
            try {
                this.starshipServer.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        for(UUID id : getListenerIDs()) {
            getListener(id).disconnect();
        }
        clearServer();
    }
    
    @Override
    public void run() {
        while(this.serverStatus == ServerStatus.OPEN) {
            for(UUID id : getListenerIDs()) {
                StarshipListener listener = getListener(id);
                List<STSup> upMessages = listener.receiveMessagesFromStarship();
                addMessagesToBuffer(id, upMessages);
                if(listener.getListenerStatus() == ListenerStatus.DISCONNECTED) {
                    System.out.println("Starship " + id.toString() + " disconnected.");
                    removeListener(id);
                    addDisconnectedStarship(id);
                }
            }
            try {
                UUID id = UUID.randomUUID();
                Socket starshipSocket = this.starshipServer.accept();
                addListener(id, new StarshipListener(starshipSocket));
                addConnectedStarships(id);
                System.out.println("New Starship " + id.toString() + " connected from " + starshipSocket.getInetAddress());
            } catch (SocketTimeoutException e) {
                // Timeouts are normal, do nothing
            } catch (SocketException e) {
                // If the server is closed, it closed because we asked it to.
                if(this.serverStatus == ServerStatus.OPEN) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                // Shutdown if anything else goes wrong
                this.serverStatus = ServerStatus.CLOSED;
            }
        }
        internalShutDown();
    }
}
