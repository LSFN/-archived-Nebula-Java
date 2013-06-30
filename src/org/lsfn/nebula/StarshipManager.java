package org.lsfn.nebula;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.lsfn.nebula.FF.FFdown;
import org.lsfn.nebula.FF.FFup;


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
        }
    }
    
    public void removeStarship(UUID id) {
        starships.remove(id);
    }
    
    public Set<UUID> getIDs() {
        return new HashSet<UUID>(this.starships.keySet());
    }
    
    public void processInput(UUID id, FFup.Lobby lobby) {
        if(lobby.hasShipName()) {
            starships.get(id).setShipName(lobby.getShipName());
        }
        if(lobby.hasReadyState()) {
            starships.get(id).setReady(lobby.getReadyState());
        }
    }
    
    public boolean isEveryoneReady() {
        boolean b = true;
        for(UUID id : this.starships.keySet()) {
            // If "&=" confuses you, that's ok. It's rather obscure syntax.
            b &= this.starships.get(id).isReady();
        }
        if(this.allReady != b) {
            this.allReadyChanged = true;
            this.allReady = b;
        }
        return this.allReady;
    }
    
    /**
     * Generates the lobby message for a given Starship
     * @param id
     * @return
     */
    public FFdown.Lobby generateOutput(UUID id) {
        FFdown.Lobby.Builder builder = FFdown.Lobby.newBuilder();
        StarshipInfo starshipInfo = starships.get(id);
        if(this.shipListChanged || this.allReadyChanged || starshipInfo.isNew()) {
            if(this.shipListChanged || starshipInfo.isNew()) {
                for(UUID shipID : this.starships.keySet()) {
                    builder.addShipNames(this.starships.get(shipID).getShipName());
                }
            }
            if(this.allReadyChanged || starshipInfo.isNew()) {
                builder.setGameStarted(this.allReady);
            }
            return builder.build();
        }
        return null;
    }
}
