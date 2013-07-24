package org.lsfn.nebula;

import java.util.UUID;

public class LobbyInfo {

    private UUID id;
    private String shipName;
    private boolean shipNameChanged;
    private boolean ready;
    private boolean readyChanged;
    private boolean isNew;
    
    public LobbyInfo(UUID id) {
        this.id = id;
        this.shipName = "Mungle Box";
        this.shipNameChanged = false;
        this.ready = false;
        this.readyChanged = false;
        this.isNew = true;
    }
    
    public UUID getId() {
        return this.id;
    }

    public String getShipName() {
        return this.shipName;
    }

    public void setShipName(String shipName) {
        this.shipName = shipName;
        this.shipNameChanged = true;
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
        this.readyChanged = true;
    }
    
    public boolean isNew() {
        return this.isNew;
    }
    
    public void noLongerNew() {
        this.isNew = false;
    }
    
    public boolean isShipNameChanged() {
        return this.shipNameChanged;
    }

    public boolean isReadyChanged() {
        return this.readyChanged;
    }

    public void resetFlags() {
        this.readyChanged = false;
        this.shipNameChanged = false;
    }

}
