package org.lsfn.nebula;

import java.util.Map;
import java.util.UUID;

import org.dyn4j.dynamics.*;
import org.dyn4j.geometry.*;
import org.lsfn.nebula.FF.FFdown;
import org.lsfn.nebula.FF.FFup;

public class Ship {

    private Body shipBody;
    private boolean[] controls = {false, false, false, false, false, false};
    
    public Ship(World world, Vector2 startPos) {
        // Turns the shipBody into a triangle
        this.shipBody = new Body();
        Vector2 v1 = new Vector2(0.0, 1.0);
        Vector2 v2 = new Vector2(0.0, -1.0);
        Vector2 v3 = new Vector2(2.0, 0.0);
        Convex shipConvex = new Triangle(v1, v2, v3);
        BodyFixture fixture = new BodyFixture(shipConvex);
        fixture.setDensity(1.0);
        fixture.setFriction(0.0);
        fixture.setRestitution(0.0);
        shipBody.addFixture(fixture);
        shipBody.setMass();
        shipBody.translate(startPos);
        world.addBody(shipBody);
    }
    
    public void processInput(FFup.Piloting piloting) {
     // The order of the booleans in controls matches that of the tag numbers in FF.proto
        if(piloting.hasTurnAnti()) {
            controls[0] = piloting.getTurnAnti();
        }
        if(piloting.hasTurnClock()) {
            controls[1] = piloting.getTurnClock();
        }
        if(piloting.hasThrustLeft()) {
            controls[2] = piloting.getThrustLeft();
        }
        if(piloting.hasThrustRight()) {
            controls[3] = piloting.getThrustRight();
        }
        if(piloting.hasThrustForward()) {
            controls[4] = piloting.getThrustForward();
        }
        if(piloting.hasThrustBackward()) {
            controls[5] = piloting.getThrustBackward();
        }
    }
    
    public void tick() {
        int turn = (controls[1] ? 1 : 0) - (controls[0] ? 1 : 0);
        int longditudinal = (controls[4] ? 1 : 0) - (controls[5] ? 1 : 0);
        int lateral = (controls[3] ? 1 : 0) - (controls[2] ? 1 : 0);
        this.shipBody.applyForce(new Vector2(0.0, longditudinal * 0.1));
        this.shipBody.applyForce(new Vector2(lateral * 0.1, 0.0));
        this.shipBody.applyTorque(turn * 0.1);
    }
    
    public Vector2 getPosition() {
        return this.shipBody.getWorldCenter();
    }
    
    public FFdown.VisualSensors generateOutput(Map<UUID, Vector2> shipPositions) {
        FFdown.VisualSensors.Builder builder = FFdown.VisualSensors.newBuilder();
        for(UUID id : shipPositions.keySet()) {
            Vector2 shipPos = shipPositions.get(id);
            Vector2 relativePos = this.shipBody.getLocalPoint(shipPos);
            FFdown.VisualSensors.SpaceObject.Point.Builder point = FFdown.VisualSensors.SpaceObject.Point.newBuilder();
            point.setX(relativePos.x).setY(relativePos.y);
            FFdown.VisualSensors.SpaceObject.Builder spaceObject = FFdown.VisualSensors.SpaceObject.newBuilder();
            spaceObject.setPosition(point.build());
            spaceObject.setOrientation(0.0);
            spaceObject.setType(0);
            builder.addSpaceObjects(spaceObject.build());
        }
        return builder.build();
    }
}
