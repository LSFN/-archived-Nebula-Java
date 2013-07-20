package org.lsfn.nebula;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Vector2;

public class Asteroid {
    
    private Body asteroidBody;
    
    public Asteroid(World world, Vector2 startPos) {
        this.asteroidBody = new Body();
        Convex asteroidConvex = new Circle(1.0);
        BodyFixture fixture = new BodyFixture(asteroidConvex);
        fixture.setDensity(1.0);
        fixture.setFriction(0.0);
        fixture.setRestitution(0.0);
        this.asteroidBody.addFixture(fixture);
        this.asteroidBody.setMass();
        this.asteroidBody.translate(startPos);
        world.addBody(this.asteroidBody);
    }

    public Vector2 getPosition() {
        return this.asteroidBody.getWorldPoint(new Vector2(0,0));
    }
    
}
