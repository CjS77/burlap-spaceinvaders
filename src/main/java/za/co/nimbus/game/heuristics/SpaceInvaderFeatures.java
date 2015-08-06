package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.FeatureDatabase;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import za.co.nimbus.game.helpers.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.MetaData.MAP_HEIGHT;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * A handrolled feature database for the Space Invaders domain
 */
@Deprecated
public class SpaceInvaderFeatures implements FeatureDatabase {
    public static final String coeff_file = "SpaceInvaderFeaturesVFA";
    public static final int LEFT_SHIELD = 4;
    public static final int MIDDLE_SHIELD = 2;
    public static final int RIGHT_SHIELD = 1;
    public static final int NO_SHIELD_COVER = 0;
    public static final int FULL_SHIELD_COVER = LEFT_SHIELD + MIDDLE_SHIELD + RIGHT_SHIELD;
    private final List<StateFeature> features =  new ArrayList<>();

    @Override
    public List<StateFeature> getStateFeatures(State s) {
        features.clear();
        ObjectInstance ship = s.getObject(SHIP_CLASS + "0");
        int id=0;
        features.add(new StateFeature(id++, getFireMetric(s, ship)));
        features.add(new StateFeature(id++, canFireMetric(ship)));
        features.add(new StateFeature(id++, shootingWillHitShieldMetric(s, ship)));
        int[] lof = addLineOfFireFeatures(features, id, ship, s);
        id += lof.length;
        ObjectInstance ship1 = s.getObject(SHIP_CLASS + "1");
        features.add(new StateFeature(id++, ship.getIntValForAttribute(MISSILE_CONTROL) >= 0? 1.0 : 0.0));
        features.add(new StateFeature(id++, ship.getIntValForAttribute(ALIEN_FACTORY) >= 0? 1.0 : 0.0));
        features.add(new StateFeature(id++, 3 - ship.getIntValForAttribute(LIVES)));
        features.add(new StateFeature(id++, ship1.getIntValForAttribute(MISSILE_CONTROL) >= 0? 1.0 : 0.0));
        features.add(new StateFeature(id++, ship1.getIntValForAttribute(ALIEN_FACTORY) >= 0? 1.0 : 0.0));
        features.add(new StateFeature(id++, 3 - ship1.getIntValForAttribute(LIVES)));
        return features;
    }

    @Override
    public List<ActionFeaturesQuery> getActionFeaturesSets(State s, List<GroundedAction> actions) {
        List<ActionFeaturesQuery> result = new ArrayList<>();
        List<StateFeature> fl = getStateFeatures(s);
        int id = 0;
        for (GroundedAction action : actions) {
            ActionFeaturesQuery afq = new ActionFeaturesQuery(action);
            for (StateFeature feature : fl) {
                afq.addFeature(new StateFeature(id++, feature.value));
            }
            result.add(afq);
        }
        return result;
    }

    @Override
    public void freezeDatabaseState(boolean toggle) {}

    @Override
    public int numberOfFeatures() {
        return features.size();
    }

    /**
     * Creates and returns a linear VFA object over this RBF feature database.
     * @param defaultWeightValue the default feature weight value to use for all features
     * @return a linear VFA object over this RBF feature database.
     */
    public LinearVFA generateVFA(double defaultWeightValue, int pNum)
    {
        File f = new File(coeff_file + "p" + pNum + ".bin");
        long t0 = System.currentTimeMillis();

        LinearVFA vfa = new LinearVFA(this, defaultWeightValue);
        if (f.exists()) {
            VFAFile.loadFromFile(vfa, f.getAbsolutePath());
            long t1 = System.currentTimeMillis();
            System.out.println("Loading coeffs took " + (t1 - t0) + "ms\n");
        }
        return vfa;
    }

    private int[] addLineOfFireFeatures(List<StateFeature> features, int id, ObjectInstance ship, State s) {
        final int range = 3;
        List<ObjectInstance> allProjectiles = s.getObjectsOfClass(MISSILE_CLASS);
        allProjectiles.addAll(s.getObjectsOfClass(BULLET_CLASS));
        List<ObjectInstance> projectiles = allProjectiles.stream().filter(
                projectile -> projectile.getIntValForAttribute(PNUM) != 0).collect(Collectors.toList());
        int[] value = new int[2*range+1];
        for (int i = 0; i < 2 * range + 1; i++) {
            value[i] = MAP_HEIGHT;
        }
        int shipX = ship.getIntValForAttribute(X);
        for (ObjectInstance projectile : projectiles) {
            Location pLoc = Location.getObjectLocation(projectile);
            int dX = Math.abs(pLoc.x - shipX);
            if (dX <= range) {
                //Adjust the danger level for projectiles off to the side to account for us having to move there over time
                //i.e. a missile 3 to the left at y=2 will never be able to hit us
                int eventHorizon = dX > 2? dX-1: 1;
                int index = pLoc.x - shipX + range;
                int y = pLoc.y - eventHorizon;
                if (y <= 0) y = MAP_HEIGHT; //The missile is no longer a threat
                value[index] = Math.min(value[index], y); //Get closest missile in column
            }
        }
        for (int aValue : value) {
            features.add(new StateFeature(id++, 1.0 - (double) aValue / (double) MAP_HEIGHT));
        }
        return value;
    }

    /**
     * Returns a metric score for the player firing in the current state. A score of 1 means the player should
     * definitely fire, a score of 0 means the player cannot, or should not fire (no target is hit).
     *
     * The metric is calculated as 1 - 2n/H
     * where
     *   H is map height
     *   n is turns before kill is registered
     * @param state the current state
     * @return the shooting kill metric
     */
    private double getFireMetric(State state, ObjectInstance ship) {
        int hitRange = willHitAlienIfShoot(state, ship);
        return 1.0 - 2.0*hitRange/MAP_HEIGHT;
    }

    /**
     * Returns a metric indicating whether the player can fire or not. 0 indicated the player is unable to fire, 1
     * indicates that he can
     * @param ship the player's ship object
     * @return the "can fire" metric
     */
    private double canFireMetric(ObjectInstance ship) {
        return canPlayerFire(ship)? 1.0 : 0.0;
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

    /**
     * Determines whether firing a missile now (regardless of whether we can actually fire) would hit one of my shields
     */
    private boolean shootingWillHitShield(State s, ObjectInstance ship) {
        return (getShieldCover(s, ship) & MIDDLE_SHIELD) > 0;
    }

    private double shootingWillHitShieldMetric(State s, ObjectInstance ship) {
        return shootingWillHitShield(s, ship)? 1.0 : 0;
    }

    /**
     * Determine whether the player is behind shields or not
     * @param s the current state
     * @param ship the player's ship object
     * @return a 3-bit mask indicating level of shield protection.
     *   421 = left, middle and right ship element
     */
    private int getShieldCover(State s, ObjectInstance ship) {
        int result = NO_SHIELD_COVER;
        List<ObjectInstance> shields = s.getObjectsOfClass(SHIELD_CLASS);
        Location l = Location.getObjectLocation(ship);
        for (ObjectInstance shield : shields) {
            int sx = shield.getIntValForAttribute(X);
            if ((sx == l.x-1) && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) result |= LEFT_SHIELD;
            if ((sx == l.x)   && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) result |= MIDDLE_SHIELD;
            if ((sx == l.x+1) && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) result |= RIGHT_SHIELD;
        }
        return result;
    }

    /**
     * Figures out if a missile fired now will hit an alien
     * @param s the current state
     * @param myShip my ship object
     * @return -1 if no alien will be hit, or the number of turns before the strike
     */
    private int willHitAlienIfShoot(State s, ObjectInstance myShip) {
        Location ship = Location.getObjectLocation(myShip);
        List<ObjectInstance> aliens = s.getObjectsOfClass(ALIEN_CLASS);
        int deltaX = myShip.getIntValForAttribute(DELTA_X);
        int table = deltaX == 1? 0 : 1;
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            if (ap != 0) {
                Location l = Location.getObjectLocation(alien);
                int j = 10 - l.y;
                if (j > 8) j = 8;
                int sx = targetMap[table][j][l.x];
                if (sx == ship.x) return Math.abs(ship.y - l.y);
            }
        }
        return -1;
    }

    /**
     * The target map works as follows. The 2nd index is the alien's 10-y position. The 3rd index is the alien X position.
     * The result is the X position the ship must be at to score a hit on that alien.
     *
     * The first map is for deltaX = 1; the 2nd map is for deltaX = -1
     */
    private static final int[][][] targetMap = new int[][][]{
            //Map for DeltaX = 1
            {
                {8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16,	15,	14,	13,	12,	11,	10},     //for y=10
                {7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16,	15,	14,	13,	12,	11},     //for y=9
                {6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16,	15,	14,	13,	12},     //for y=8
                {5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16,	15,	14,	13},     //for y=7
                {4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16,	15,	14},     //for y=6
                {3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16,	15},     //for y=5
                {2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16,	16},     //for y=4
                {1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16,	16},     //for y=3
                {0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16}      //for y=2
            },
            //Map for DeltaX = -1
            {
                {6,	5,	4,	3,	2,	1,	0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8},      //for y=10
                {5,	4,	3,	2,	1,	0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	10},     //for y=9
                {4,	3,	2,	1,	0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10},     //for y=8
                {3,	2,	1,	0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11},     //for y=7
                {2,	1,	0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12},     //for y=6
                {1,	0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13},     //for y=5
                {0,	0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14},     //for y=4
                {0,	0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15},     //for y=3
                {0,	1,	2,	3,	4,	5,	6,	7,	8,	9,	10,	11,	12,	13,	14,	15,	16}      //for y=2
            }
    };
}
