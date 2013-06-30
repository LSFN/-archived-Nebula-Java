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
    private boolean ready;
    private boolean isNew;

    public StarshipInfo(UUID id) {
        this.id = id;
        this.shipName = "Mungle Box";
        this.setReady(false);
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
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
    
    public boolean isNew() {
        return this.isNew;
    }
    
    public void noLongerNew() {
        this.isNew = false;
    }

}
