package org.lsfn.nebula;

import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Triangle;
import org.dyn4j.geometry.Vector2;
import org.lsfn.nebula.STS.STSdown;
import org.lsfn.nebula.STS.STSdown.PowerDistribution.SystemDescription;
import org.lsfn.nebula.STS.STSdown.PowerDistribution.SystemState;
import org.lsfn.nebula.STS.STSup;

public class Ship {

    private static final double enginePowerRequired = 100;
    private static final double thrusterPowerRequired = 10;
    private static final double engineThrustMod = 1.0;
    private static final double thrusterThrustMod = 0.8;
    private static final Vector2 engineLPoint = new Vector2(-0.5, 0);
    private static final Vector2 engineRPoint = new Vector2(0.5, 0);
    private static final Vector2 thrustersFPoint = new Vector2(0, 1.5);
    private static final Vector2 thrustersRPoint = new Vector2(0, 0.5);
    private static final double visualSensorsDistanceLimit = 100;
    
    private Body shipBody;
    private double reactant, coolant, throttleL, throttleR, thrustFL, thrustFR, thrustRL, thrustRR;
    private boolean engineLActive, engineRActive, thrusterFLActive, thrusterFRActive, thrusterRLActive, thrusterRRActive;
    private boolean systemsDescriptionChange;
    private double reactorOutput, reactorHeatLevel;
    
    public Ship(World world, Vector2 startPos) {
        // Turns the shipBody into a triangle
        shipBody = new Body();
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
        
        systemsDescriptionChange = true;
        reactant = coolant = throttleL = throttleR = thrustFL = thrustFR = thrustRL = thrustRR = 0;
        engineLActive = engineRActive = thrusterFLActive = thrusterFRActive = thrusterRLActive = thrusterRRActive = true;
        reactorOutput = reactorHeatLevel = 0;
    }
    
    public void processInput(STSup message) {
    	if(message.hasReactor()) {
	    	coolant = message.getReactor().getCoolantIntroduction();
	    	reactant = message.getReactor().getReactantIntroduction();
    	}
    	if(message.hasPowerDistribution()) {
    		for(STSup.PowerDistribution.SystemState systemState : message.getPowerDistribution().getSystemStatesList()) {
        		switch(systemState.getSystemID()) {
        		case 0:
        			engineLActive = systemState.getSystemState();
        			break;
        		case 1:
        			engineRActive = systemState.getSystemState();
        			break;
        		case 2:
        			thrusterFLActive = systemState.getSystemState();
        			break;
        		case 3:
        			thrusterFRActive = systemState.getSystemState();
        			break;
        		case 4:
        			thrusterRLActive = systemState.getSystemState();
        			break;
        		case 5:
        			thrusterRRActive = systemState.getSystemState();
        			break;
    			default:
    				break;
        		}
        	}
    	}
    	if(message.hasEngines()) {
    		throttleL = message.getEngines().getLeftEngineThrottle();
    		throttleR = message.getEngines().getRightEngineThrottle();
    	}
    	if(message.hasThrusters()) {
    		thrustFL = message.getThrusters().getForwardLeft();
    		thrustFR = message.getThrusters().getForwardRight();
    		thrustRL = message.getThrusters().getRearLeft();
    		thrustRR = message.getThrusters().getRearRight();
    	}
    }
    
    public void tick() {
    	// Some random equations to determine reactor performance
    	reactorOutput = reactorOutput * 1.1 + reactant - coolant;
    	if(reactorOutput < 0) reactorOutput = 0;
    	reactorHeatLevel = reactorHeatLevel + 0.5 * reactorOutput - coolant;
    	// Temporary way of saying "The reactor exploded"
    	if(reactorHeatLevel > 1000) {
    		reactorOutput = reactorHeatLevel = 0;
    		engineLActive = engineRActive = thrusterFLActive = thrusterFRActive = thrusterRLActive = thrusterRRActive = false;
    	}
    	
    	double distributedPowerLevel = reactorOutput - (enginePowerRequired * 2 + thrusterPowerRequired * 4);
    	if(distributedPowerLevel > 1) distributedPowerLevel = 1;
    	
		double engineLThrust = distributedPowerLevel * throttleL * engineThrustMod;
		double engineRThrust = distributedPowerLevel * throttleR * engineThrustMod;
    	double FLThrust	= distributedPowerLevel * thrustFL * thrusterThrustMod;
    	double FRThrust	= distributedPowerLevel * thrustFR * thrusterThrustMod;
    	double RLThrust	= distributedPowerLevel * thrustRL * thrusterThrustMod;
    	double RRThrust	= distributedPowerLevel * thrustRR * thrusterThrustMod;
		
    	// TRIG MATHS!
        double theAngle = this.shipBody.getTransform().getRotation();
        double theSin = Math.sin(theAngle);
        double theCos = Math.cos(theAngle);
    	this.shipBody.applyForce(new Vector2(-theSin * engineLThrust, theCos * engineLThrust), engineLPoint);
    	this.shipBody.applyForce(new Vector2(-theSin * engineRThrust, theCos * engineRThrust), engineRPoint);
    	this.shipBody.applyForce(new Vector2(theCos * (FRThrust - FLThrust), theSin * (FRThrust - FLThrust)), thrustersFPoint);
    	this.shipBody.applyForce(new Vector2(theCos * (RRThrust - RLThrust), theSin * (RRThrust - RLThrust)), thrustersRPoint);
    }
    
    public Vector2 getPosition() {
        return this.shipBody.getWorldPoint(new Vector2(0, 0));
    }
    
    public double getRotation() {
        return this.shipBody.getTransform().getRotation();
    }
    
    public STSdown generateOutput(List<Ship> ships, List<Asteroid> asteroids) {
    	STSdown.Builder stsDown = STSdown.newBuilder();
    	
    	// Visual Sensors
    	STSdown.VisualSensors.Builder stsDownVS = STSdown.VisualSensors.newBuilder();
        // It has been determined that "this.shipBody.getLocalPoint(shipPos)" takes into account the rotation of the body
        // So no manual trig maths needs to go here
        for(Ship ship : ships) {
            Vector2 relativePos = this.shipBody.getLocalPoint(ship.getPosition());
            if(relativePos.getMagnitude() < visualSensorsDistanceLimit) {
	            STSdown.VisualSensors.SpaceObject.Point.Builder point = STSdown.VisualSensors.SpaceObject.Point.newBuilder();
	            point.setX(relativePos.x).setY(relativePos.y);
	            STSdown.VisualSensors.SpaceObject.Builder spaceObject = STSdown.VisualSensors.SpaceObject.newBuilder();
	            spaceObject.setPosition(point.build());
	            spaceObject.setOrientation(ship.getRotation() - this.getRotation());
	            spaceObject.setType(STSdown.VisualSensors.SpaceObject.Type.SHIP);
	            stsDownVS.addSpaceObjects(spaceObject.build());
            }
        }
        for(Asteroid asteroid : asteroids) {
            Vector2 relativePos = this.shipBody.getLocalPoint(asteroid.getPosition());
            STSdown.VisualSensors.SpaceObject.Point.Builder point = STSdown.VisualSensors.SpaceObject.Point.newBuilder();
            point.setX(relativePos.x).setY(relativePos.y);
            STSdown.VisualSensors.SpaceObject.Builder spaceObject = STSdown.VisualSensors.SpaceObject.newBuilder();
            spaceObject.setPosition(point.build());
            spaceObject.setOrientation(0.0); // actual orientation is unnecessary on a circular asteroid
            spaceObject.setType(STSdown.VisualSensors.SpaceObject.Type.ASTEROID);
            stsDownVS.addSpaceObjects(spaceObject.build());
        }
        stsDown.setVisualSensors(stsDownVS);
        
        // Reactor
        STSdown.Reactor.Builder stsDownReactor = STSdown.Reactor.newBuilder();
        stsDownReactor.setHeatLevel(reactorHeatLevel);
        stsDownReactor.setPowerOutput(reactorOutput);
        stsDown.setReactor(stsDownReactor);
        
        // Power Distribution
        STSdown.PowerDistribution.Builder stsDownPD = STSdown.PowerDistribution.newBuilder();
        if(systemsDescriptionChange) {
        	stsDownPD.addSystemDescriptions(SystemDescription.newBuilder().setSystemID(0).setSystemName("Main Engine (Left)"));
        	stsDownPD.addSystemDescriptions(SystemDescription.newBuilder().setSystemID(1).setSystemName("Main Engine (Right)"));
        	stsDownPD.addSystemDescriptions(SystemDescription.newBuilder().setSystemID(2).setSystemName("Thruster (Front Left)"));
        	stsDownPD.addSystemDescriptions(SystemDescription.newBuilder().setSystemID(3).setSystemName("Thruster (Front Right)"));
        	stsDownPD.addSystemDescriptions(SystemDescription.newBuilder().setSystemID(4).setSystemName("Thruster (Rear Left)"));
        	stsDownPD.addSystemDescriptions(SystemDescription.newBuilder().setSystemID(5).setSystemName("Thruster (Rear Right)"));
        	systemsDescriptionChange = false;
        }
        stsDownPD.addSystemStates(SystemState.newBuilder().setSystemID(0).setSystemState(engineLActive));
        stsDownPD.addSystemStates(SystemState.newBuilder().setSystemID(1).setSystemState(engineRActive));
        stsDownPD.addSystemStates(SystemState.newBuilder().setSystemID(2).setSystemState(thrusterFLActive));
        stsDownPD.addSystemStates(SystemState.newBuilder().setSystemID(3).setSystemState(thrusterFRActive));
        stsDownPD.addSystemStates(SystemState.newBuilder().setSystemID(4).setSystemState(thrusterRLActive));
        stsDownPD.addSystemStates(SystemState.newBuilder().setSystemID(5).setSystemState(thrusterRRActive));
        stsDown.setPowerDistribution(stsDownPD);
        
        return stsDown.build();
    }
    
}
