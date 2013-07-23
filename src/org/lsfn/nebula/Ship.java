package org.lsfn.nebula;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dyn4j.dynamics.*;
import org.dyn4j.geometry.*;
import org.lsfn.nebula.STS.*;

public class Ship {

    private static final double torqueMod = 0.1;
    private static final double forceMod = 0.5;
    
    private Body shipBody;
    private boolean[] controls = {false, false, false, false, false, false};
    
    public Ship(World world, Vector2 startPos) {
        // Turns the shipBody into a triangle
        this.shipBody = new Body();
        Vector2 v1 = new Vector2(1.0, 0.0);
        Vector2 v2 = new Vector2(0.0, 2.0);
        Vector2 v3 = new Vector2(-1.0, 0.0);
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
    
    public void processInput(STSup.Piloting piloting) {
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
        int turn = (controls[0] ? 1 : 0) - (controls[1] ? 1 : 0);
        int longditudinal = (controls[4] ? 1 : 0) - (controls[5] ? 1 : 0);
        int lateral = (controls[3] ? 1 : 0) - (controls[2] ? 1 : 0);
        // TRIG MATHS!
        double theAngle = this.shipBody.getTransform().getRotation();
        double theSin = Math.sin(theAngle);
        double theCos = Math.cos(theAngle);
        this.shipBody.applyForce(new Vector2(-theSin * longditudinal * forceMod, theCos * longditudinal * forceMod));
        this.shipBody.applyForce(new Vector2(theCos * lateral * forceMod, theSin * lateral * forceMod));
        this.shipBody.applyTorque(turn * torqueMod);
    }
    
    public Vector2 getPosition() {
        return this.shipBody.getWorldPoint(new Vector2(0, 0));
    }
    
    public double getRotation() {
        return this.shipBody.getTransform().getRotation();
    }
    
    public STSdown.VisualSensors generateOutput(List<Ship> ships, List<Asteroid> asteroids) {
        STSdown.VisualSensors.Builder builder = STSdown.VisualSensors.newBuilder();
        // It has been determined that "this.shipBody.getLocalPoint(shipPos)" takes into account the rotation of the body
        // So no manual trig maths needs to go here
        for(Ship ship : ships) {
            Vector2 relativePos = this.shipBody.getLocalPoint(ship.getPosition());
            STSdown.VisualSensors.SpaceObject.Point.Builder point = STSdown.VisualSensors.SpaceObject.Point.newBuilder();
            point.setX(relativePos.x).setY(relativePos.y);
            STSdown.VisualSensors.SpaceObject.Builder spaceObject = STSdown.VisualSensors.SpaceObject.newBuilder();
            spaceObject.setPosition(point.build());
            spaceObject.setOrientation(ship.getRotation() - this.getRotation());
            spaceObject.setType(STSdown.VisualSensors.SpaceObject.Type.SHIP);
            builder.addSpaceObjects(spaceObject.build());
        }
        for(Asteroid asteroid : asteroids) {
            Vector2 relativePos = this.shipBody.getLocalPoint(asteroid.getPosition());
            STSdown.VisualSensors.SpaceObject.Point.Builder point = STSdown.VisualSensors.SpaceObject.Point.newBuilder();
            point.setX(relativePos.x).setY(relativePos.y);
            STSdown.VisualSensors.SpaceObject.Builder spaceObject = STSdown.VisualSensors.SpaceObject.newBuilder();
            spaceObject.setPosition(point.build());
            spaceObject.setOrientation(0.0); // actual orientation is unnecessary on a circular asteroid
            spaceObject.setType(STSdown.VisualSensors.SpaceObject.Type.ASTEROID);
            builder.addSpaceObjects(spaceObject.build());
        }
        return builder.build();
    }
}
