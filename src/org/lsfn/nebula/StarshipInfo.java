package org.lsfn.nebula;

import java.util.UUID;

/**
 * POJO for storing information about each Starship.
 * @author Lukeus_Maximus
 *
 */
public class StarshipInfo {

    private UUID id;
    private String shipName;
    private boolean shipNameChanged;
    private boolean ready;
    private boolean readyChanged;
    private boolean isNew;

    public StarshipInfo(UUID id) {
        this.id = id;
        this.shipName = "Mungle Box";
        this.shipNameChanged = true;
        this.ready = false;
        this.readyChanged = true;
        this.isNew = true;
    }

    public UUID getId() {
        return id;
    }

    public String getShipName() {
        return shipName;
    }

    public void setShipName(String shipName) {
        this.shipName = shipName;
        this.shipNameChanged = true;
    }

    public boolean isReady() {
        return ready;
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
        return shipNameChanged;
    }

    public boolean isReadyChanged() {
        return readyChanged;
    }

    public void resetFlags() {
        this.readyChanged = false;
        this.shipNameChanged = false;
    }

}
