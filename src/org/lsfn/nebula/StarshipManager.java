package org.lsfn.nebula;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.lsfn.nebula.STS.*;


public class StarshipManager {
    
    private Map<UUID, StarshipInfo> starships;
    private boolean shipListChanged;
    private boolean allReady;
    private boolean allReadyChanged;
    
    public StarshipManager() {
        this.starships = new HashMap<UUID, StarshipInfo>();
        this.shipListChanged = false;
        this.allReady = false;
        this.allReadyChanged = false;
    }
    
    public void addStarship(UUID id) {
        if(!starships.containsKey(id)) {
            starships.put(id, new StarshipInfo(id));
            this.shipListChanged = true;
        }
    }
    
    public void removeStarship(UUID id) {
        if(starships.remove(id) != null) {
            this.shipListChanged = true;
        }
    }
    
    public Set<UUID> getIDs() {
        return new HashSet<UUID>(this.starships.keySet());
    }
    
    public void processInput(UUID id, STSup.Lobby lobby) {
        if(lobby.hasShipName()) {
            starships.get(id).setShipName(lobby.getShipName());
            this.shipListChanged = true;
        }
        if(lobby.hasReadyState()) {
            starships.get(id).setReady(lobby.getReadyState());
            if(this.starships.size() >= 1) {
                boolean b = true;
                for(UUID id2 : this.starships.keySet()) {
                    // If "&=" confuses you, that's ok. It's rather obscure syntax.
                    b &= this.starships.get(id2).isReady();
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
    
    /**
     * Generates the lobby message for a given Starship
     * @param id
     * @return
     */
    public STSdown.Lobby generateOutput(UUID id) {
        StarshipInfo starshipInfo = starships.get(id);
        if(this.shipListChanged || this.allReadyChanged || starshipInfo.isNew() || starshipInfo.isShipNameChanged() || starshipInfo.isReadyChanged()) {
            STSdown.Lobby.Builder builder = STSdown.Lobby.newBuilder();
            if(this.shipListChanged || starshipInfo.isNew()) {
                for(UUID shipID : this.starships.keySet()) {
                    if(!shipID.equals(id)) {
                        builder.addShipsInGame(this.starships.get(shipID).getShipName());
                    }
                }
            }
                
            if(this.allReadyChanged || starshipInfo.isNew()) {
                builder.setGameStarted(this.allReady);
            }
            
            if(starshipInfo.isReadyChanged() || starshipInfo.isNew()) {
                builder.setReadyState(starshipInfo.isReady());
            }
            
            if(starshipInfo.isShipNameChanged() || starshipInfo.isNew()) {
                builder.setShipName(starshipInfo.getShipName());
            }
            
            starshipInfo.noLongerNew();
            starshipInfo.resetFlags();
            
            return builder.build();
        }
        return null;
    }
    
    public void resetFlags() {
        this.shipListChanged = false;
        this.allReadyChanged = false;
    }
}
