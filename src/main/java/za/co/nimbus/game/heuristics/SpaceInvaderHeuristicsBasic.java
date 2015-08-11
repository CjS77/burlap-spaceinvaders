package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import za.co.nimbus.game.constants.Attributes;
import za.co.nimbus.game.constants.Commands;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;
import za.co.nimbus.game.rules.Collision;
import za.co.nimbus.game.rules.SpaceInvaderMechanics;

import java.util.*;
import java.util.stream.Collectors;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.MetaData.MAP_HEIGHT;
import static za.co.nimbus.game.constants.MetaData.MAP_WIDTH;
import static za.co.nimbus.game.constants.MetaData.SHOT_COST;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * A 4th attempt at a set of heuristics - The weights are always 1; and the features select the best move
 */
public class SpaceInvaderHeuristicsBasic extends Heuristics {

    final int RADAR_RANGE = 2;
    private final Domain domain;
    private double[] priorities = new double[7];
    private static final int MOVE_LEFT  = 1;
    private static final int NOTHING    = 2;
    private static final int MOVE_RIGHT = 3;
    private static final int SHOOT      = 4;
    private static final int BUILD_MC   = 5;
    private static final int BUILD_AF   = 6;
    private static final int BUILD_SHIELDS = 0;

    public SpaceInvaderHeuristicsBasic(Domain d) {
        this(null, d);
    }

    public SpaceInvaderHeuristicsBasic(String filename, Domain d) {
        super(filename, 1.0);
        this.domain = d;
        for (int i=0; i<7; i++) {
            priorities[i] = 0.0;
        }
    }

    @Override
    public List<StateFeature> getStateFeatures(State s) {
        throw new IllegalStateException("This Heuristic set does not support state features");
    }

    @Override
    public List<ActionFeaturesQuery> getActionFeaturesSets(State s, List<GroundedAction> actions) {
        List<ActionFeaturesQuery> result = new ArrayList<>();
        calculatePriorities(s);
        for (GroundedAction action : actions) {
            List<StateFeature> actionFeatures = new ArrayList<>();
            ActionFeaturesQuery afq = new ActionFeaturesQuery(action, actionFeatures);
            result.add(afq);
            switch (action.actionName()) {
                case Nothing:
                    actionFeatures.add(new StateFeature(NOTHING, priorities[NOTHING]));
                    break;
                case MoveLeft:
                    actionFeatures.add(new StateFeature(MOVE_LEFT, priorities[MOVE_LEFT]));
                    break;
                case MoveRight:
                    actionFeatures.add(new StateFeature(MOVE_RIGHT, priorities[MOVE_RIGHT]));
                    break;
                case Shoot:
                    actionFeatures.add(new StateFeature(SHOOT, priorities[SHOOT]));
                    break;
                case BuildAlienFactory:
                    actionFeatures.add(new StateFeature(BUILD_AF, priorities[BUILD_AF]));
                    break;
                case BuildMissileController:
                    actionFeatures.add(new StateFeature(BUILD_MC, priorities[BUILD_MC]));
                    break;
                case BuildShield:
                    actionFeatures.add(new StateFeature(BUILD_SHIELDS, priorities[BUILD_SHIELDS]));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid action: " + action.actionName());
            }
        }
        return result;
    }

    @Override
    protected Map<Integer, Double> createInitialWeightMap() {
        return new HashMap<>();
    }

    final int HHIGH = 50;
    final int HIGH = 10;
    final int MED = 5;
    final int LOW = 1;
    private void calculatePriorities(State s) {

        // Init some utility variables
        ObjectInstance ship = s.getObject(SHIP_CLASS + "0");
        int[] radar = getProjectileRadar(ship, s);
        boolean canShoot = canPlayerFire(ship);
        //Protocol A - Dont get killed
        calcDirectFiringLinePriorities(canShoot, radar[RADAR_RANGE]);
        calcOffsetFiringLinePriorities(s, canShoot, radar, -1);
        //Protocol B - Kill aliens
        calcShootPriorities(s, canShoot);
        //Protocol C - Build Buildings
        calcBuildingPriorities(ship);
        //Protocol D - Protect Building
        calcProtectPriorities(ship);
    }

    private void calcProtectPriorities(ObjectInstance ship) {
        int mcX = ship.getIntValForAttribute(MISSILE_CONTROL);
        if (mcX >= 0) {
            int dx = ship.getIntValForAttribute(X) - mcX;
            if (dx < 0) priorities[MOVE_RIGHT] += LOW;
            if (dx == 0) priorities[NOTHING] += LOW;
            if (dx > 0) priorities[MOVE_LEFT] += LOW;
        }
    }

    private void calcBuildingPriorities(ObjectInstance ship) {
        priorities[BUILD_AF] -= HHIGH; //Never build an AF
        if (ship.getIntValForAttribute(MISSILE_CONTROL) >= 0) {
            priorities[BUILD_MC] -= HHIGH;
        } else {
            priorities[BUILD_MC] += MED + 1;
        }
    }


    private void calcShootPriorities(State s, boolean canShoot) {
        if (canShoot) {
            if (getKillRange(s, new String[] {Shoot}) > 0) priorities[SHOOT] += MED;
            else priorities[SHOOT] -= MED;
        } else {
            if (canShootAfterSequence(s, new String[] {Nothing})) {
                if (getKillRange(s, new String[] {Nothing, Shoot}) > 0) priorities[NOTHING] += MED;
                if (getKillRange(s, new String[] {MoveLeft, Shoot}) > 0) priorities[MOVE_LEFT] += MED;
                if (getKillRange(s, new String[] {MoveRight, Shoot}) > 0) priorities[MOVE_RIGHT] += MED;
            }
        }
    }

    private boolean canShootAfterSequence(State s, String[] moves) {
        State sNext = simulateTurns(s, moves);
        return canPlayerFire(sNext.getObject(SHIP_CLASS + "0"));
    }

    private void calcOffsetFiringLinePriorities(State s, boolean canShoot, int[] radar, int dir) {
        boolean canShootNextTurn = false;
        if (!canShoot) {
            String move = dir > 0 ? MoveRight : MoveLeft;
            State sNext = simulateTurns(s, new String[]{move});
            canShootNextTurn = canPlayerFire(sNext.getObject(SHIP_CLASS + "0"));
        }
        if (radar[RADAR_RANGE + dir] <= 2) {
            priorities[NOTHING - dir] += HHIGH; //Evade
        }
        if (radar[RADAR_RANGE + dir] == 3) {
            if (canShoot || canShootNextTurn) {
                priorities[NOTHING + dir] += HIGH; //Move & Shoot
                priorities[NOTHING - dir] += MED;  //Evade
                priorities[BUILD_SHIELDS] += LOW;  //Block with shields
            } else {
                priorities[NOTHING - dir] += HIGH;
                priorities[BUILD_SHIELDS] += LOW;  //Block with shields
            }
        }
    }

    private void calcDirectFiringLinePriorities(boolean canShoot, int range) {
        if (range <= 3) {
            if (canShoot) {
                priorities[SHOOT] += HHIGH;
            } else {
                priorities[MOVE_RIGHT] += HIGH;
                priorities[MOVE_RIGHT] += HIGH;
            }
        }
        if (range == 4 && canShoot) priorities[SHOOT] += MED;
        if (range > 5 && range < MAP_HEIGHT/2 && canShoot) priorities[SHOOT] += LOW;
        if (range >= 4 && range < MAP_HEIGHT/2 && !canShoot) {
            priorities[NOTHING] += LOW;
            priorities[BUILD_SHIELDS] += LOW;
            priorities[MOVE_RIGHT] += LOW;
            priorities[MOVE_LEFT] += LOW;
        }
    }


    private State simulateTurns(State state, String[] moveSequence) {
        State s= state.copy();
        for (String move : moveSequence) {
            SpaceInvaderMechanics.moveProjectiles(s, MISSILE_CLASS);
            s = SpaceInvaderMechanics.simulateAliensShipOnly(domain, s, move);
        }
        return s;
    }

    private int getKillRange(State state, String[] moveSequence) {
        State s= state.copy();
        //Remove my existing missiles, to avoid false positives
        List<ObjectInstance> missiles = s.getObjectsOfClass(MISSILE_CLASS);
        missiles.forEach(s::removeObject);
        missiles = s.getObjectsOfClass(BULLET_CLASS);
        missiles.forEach(s::removeObject);
        for (int i = 0; i < MAP_HEIGHT/2; i++){
            SpaceInvaderMechanics.moveProjectiles(s, MISSILE_CLASS);
            if (gotKill(s)) return i;
            String move = i < moveSequence.length? moveSequence[i] : Nothing;
            s = SpaceInvaderMechanics.simulateAliensShipOnly(domain, s, move);
            if (gotKill(s)) return i;
        }
        return 0; //Whiffed
    }

    private boolean gotKill(State s) {
        PropositionalFunction pf = domain.getPropFunction(MISSILE_CLASS + ALIEN_CLASS + Collision.NAME);
        boolean hit = pf.getAllGroundedPropsForState(s).stream().filter(
                gp -> s.getObject(gp.params[0]).getIntValForAttribute(PNUM) == 0
                        && s.getObject(gp.params[1]).getIntValForAttribute(PNUM) == 1
        ).anyMatch(
                gp -> gp.isTrue(s)
        );
        return hit;
    }

    @Override
    public int numberOfFeatures() {
        return 0;
    }

    /**
     * Returns a radar map of incoming projectiles (missiles and bullets).
     * @param ship my ship instance
     * @param s the current state
     * @return a vector of 2*RADAR_RANGE + 1 y values. radar[RADAR_RANGE] is in line with the ship.
     */
    private int[] getProjectileRadar(ObjectInstance ship, State s) {
        List<ObjectInstance> allProjectiles = s.getObjectsOfClass(MISSILE_CLASS);
        allProjectiles.addAll(s.getObjectsOfClass(BULLET_CLASS));
        List<ObjectInstance> projectiles = allProjectiles.stream().filter(
                projectile -> projectile.getIntValForAttribute(PNUM) != 0).collect(Collectors.toList());
        int[] value = new int[2* RADAR_RANGE +1];
        for (int i = 0; i < 2 * RADAR_RANGE + 1; i++) {
            value[i] = MAP_HEIGHT;
        }
        int shipX = ship.getIntValForAttribute(X);
        for (ObjectInstance projectile : projectiles) {
            Location pLoc = Location.getObjectLocation(projectile);
            //Don't worry if shields are covering
            int dX = Math.abs(pLoc.x - shipX);
            if (dX <= RADAR_RANGE) {
                //Adjust the danger level for projectiles off to the side to account for us having to move there over time
                //i.e. a missile 3 to the left at y=2 will never be able to hit us
                int index = pLoc.x - shipX + RADAR_RANGE;
                value[index] = Math.min(value[index],pLoc.y); //Get closest missile in column
            }
        }
        return value;
    }


    /**
     * @param ship the player's ship object
     * @return true if the player is able to fire a missile, false otherwise
     */
    private boolean canPlayerFire(ObjectInstance ship) {
        boolean hasMissileController = ship.getIntValForAttribute(MISSILE_CONTROL) >= 0;
        int missileCount = ship.getIntValForAttribute(MISSILE_COUNT);
        return missileCount == 0 || (missileCount == 1 && hasMissileController);
    }

}