package org.lsfn.nebula;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.lsfn.nebula.STS.STSdown;
import org.lsfn.nebula.STS.STSup;

public class LobbyManager {
    
    private Map<UUID, LobbyInfo> lobbyInfos;
    
    private boolean shipListChanged;
    private boolean allReady;
    private boolean allReadyChanged;
    
    public LobbyManager() {
        this.lobbyInfos = new HashMap<UUID, LobbyInfo>();
        this.shipListChanged = false;
        this.allReady = false;
        this.allReadyChanged = false;
    }
    
    public void handleNewStarships(List<UUID> newStarships) {
        for(UUID starshipID : newStarships) {
            this.lobbyInfos.put(starshipID, new LobbyInfo(starshipID));
        }
    }
    
    public void processInput(UUID starshipID, STSup.Lobby lobby) {
        LobbyInfo lobbyInfo = this.lobbyInfos.get(starshipID);
        if(lobby.hasShipName()) {
            lobbyInfo.setShipName(lobby.getShipName());
            this.shipListChanged = true;
        }
        if(lobby.hasReadyState()) {
            lobbyInfo.setReady(lobby.getReadyState());
            if(this.lobbyInfos.size() >= 1) {
                boolean b = true;
                for(UUID id2 : this.lobbyInfos.keySet()) {
                    // If "&=" confuses you, that's ok. It's rather obscure syntax.
                    b &= this.lobbyInfos.get(id2).isReady();
                }
                if(this.allReady != b) {
                    this.allReadyChanged = true;
                    this.allReady = b;
                }
            }
        }
    }
    
    public boolean isEveryoneReady() {
        return this.allReady;
    }
    
    public boolean generateOutput(STSdown.Builder stsDown, UUID starshipID) {
        LobbyInfo lobbyInfo = this.lobbyInfos.get(starshipID);
        if(this.shipListChanged || this.allReadyChanged || lobbyInfo.isNew() || lobbyInfo.isShipNameChanged() || lobbyInfo.isReadyChanged()) {
            STSdown.Lobby.Builder stsDownLobby = STSdown.Lobby.newBuilder();
            if(this.shipListChanged || lobbyInfo.isNew()) {
                for(UUID shipID : this.lobbyInfos.keySet()) {
                    if(!shipID.equals(starshipID)) {
                        stsDownLobby.addShipsInGame(this.lobbyInfos.get(shipID).getShipName());
                    }
                }
            }
                
            if(this.allReadyChanged || lobbyInfo.isNew()) {
                stsDownLobby.setGameStarted(this.allReady);
            }
            
            if(lobbyInfo.isReadyChanged() || lobbyInfo.isNew()) {
                stsDownLobby.setReadyState(lobbyInfo.isReady());
            }
            
            if(lobbyInfo.isShipNameChanged() || lobbyInfo.isNew()) {
                stsDownLobby.setShipName(lobbyInfo.getShipName());
            }
            
            lobbyInfo.noLongerNew();
            lobbyInfo.resetFlags();
            
            stsDown.setLobby(stsDownLobby);
            return true;
        }
        return false;
    }
    
    public void resetFlags() {
        this.allReadyChanged = false;
        this.shipListChanged = false;
    }
    
    public Set<UUID> getIDs() {
        return new HashSet<UUID>(this.lobbyInfos.keySet());
    }

}
