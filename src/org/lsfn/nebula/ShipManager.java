package org.lsfn.nebula;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Vector2;
import org.lsfn.nebula.FF.FFdown;
import org.lsfn.nebula.FF.FFup;

/**
 * Not to be confused with the StarshipManager, this class deals with the actual ship in the game and not the Starship program.
 * @author Lukeus_Maximus
 *
 */
public class ShipManager {
    
    private World world;
    private Map<UUID, Ship> ships;
    private double nextX;
    
    public ShipManager(World world) {
        this.world = world;
        this.ships = new HashMap<UUID, Ship>();
        this.nextX = 0;
    }
    
    public void addShip(UUID id) {
        Ship ship = new Ship(this.world, new Vector2(nextX, 0));
        nextX += 3.0;
        this.ships.put(id, ship);
    }
    
    public void processInput(UUID id, FFup.Piloting piloting) {
        this.ships.get(id).processInput(piloting);
    }
    
    public void tick() {
        for(UUID id : this.ships.keySet()) {
            this.ships.get(id).tick();
        }
    }
    
    public FFdown.VisualSensors generateOutput(UUID id) {
        Ship ship = this.ships.get(id);
        Map<UUID, Vector2> shipPositions = new HashMap<UUID, Vector2>();
        for(UUID id2 : this.ships.keySet()) {
            if(!id.equals(id2)) {
                shipPositions.put(id2, this.ships.get(id2).getPosition());
            }
        }
        return ship.generateOutput(shipPositions);
    }
}
