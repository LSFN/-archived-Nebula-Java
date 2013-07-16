package org.lsfn.nebula;

import java.util.ArrayList;
import java.util.List;

import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Vector2;

public class AsteroidManager {

    private World world;
    private List<Asteroid> asteroids;
    
    public AsteroidManager(World world) {
        this.world = world;
        this.asteroids = new ArrayList<Asteroid>();
    }

    public void addAsteroid(Vector2 pos) {
        asteroids.add(new Asteroid(this.world, pos));
    }

    public List<Asteroid> getAsteroids() {
        return asteroids;
    }
    
}
