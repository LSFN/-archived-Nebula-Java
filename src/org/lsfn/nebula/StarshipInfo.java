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

    public StarshipInfo(UUID id) {
        this.id = id;
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

}
