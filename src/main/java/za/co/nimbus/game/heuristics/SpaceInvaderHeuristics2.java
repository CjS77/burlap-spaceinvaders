package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;
import za.co.nimbus.game.rules.SpaceInvaderMechanics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.MetaData.MAP_HEIGHT;
import static za.co.nimbus.game.constants.MetaData.MAP_WIDTH;
import static za.co.nimbus.game.constants.ObjectClasses.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.saDomain.SASpaceInvaderRewardFunction.*;

/**
 * A second attempt at a set of heuristics
 */
public class SpaceInvaderHeuristics2 extends Heuristics {
    private static final int WILL_DIE_IF_STATIONARY = 1;
    private static final int WILL_DIE_IF_MOVELEFT = 2;
    private static final int WILL_DIE_IF_MOVERIGHT = 3;
    private static final int GOT_MIDDLE_SHIELD = 4;
    private static final int CAN_SHOOT = 5;
    private static final int ALIEN_RANGE = 6;
    private static final int ALIEN_RANGE_LEFT = 7;
    private static final int ALIEN_RANGE_RIGHT = 8;
    private static final int WILL_HIT_ENEMY_SHIELD_IF_FIRE = 9;
    private static final int WILL_HIT_ENEMY_BUILDING_IF_FIRE = 10;
    private static final int WILL_HIT_PROJECTILE_IF_FIRE = 11;
    private static final int LIVES_LEFT = 12;
    private static final int BUILDING_COVER = 13;
    private static final int SHIELDS_WILL_BLOCK_PROJECTILE = 14;
    private static final int SHIELDS_WILL_EXPLODE_ALIEN = 15;
    private static final int SHIELDS_WILL_DEFEND_BUILDING = 16;
    private static final int ALIEN_RANGE_WAIT = 17;
    private static final int ENEMY_OFFSET_LEFT = 18;
    private static final int ENEMY_OFFSET_RIGHT = 19;
    private static final int ENEMY_MC_OFFSET_RIGHT = 20;
    private static final int ENEMY_MC_OFFSET_LEFT = 21;
    private static final int ENEMY_AF_OFFSET_LEFT = 22;
    private static final int ENEMY_AF_OFFSET_RIGHT = 23;

    private static final int CONSTANT = 64;
    //Action flags
    private static final int NOTHING    = 1000000;
    private static final int MOVE_LEFT  = 2000000;
    private static final int MOVE_RIGHT = 3000000;
    private static final int SHOOT      = 4000000;
    private static final int BUILD_MC   = 5000000;
    private static final int BUILD_AF   = 6000000;
    private static final int BUILD_SHIELDS = 7000000;

    final int RADAR_RANGE = 3;
    private static final double DMAP_HEIGHT = (double) MAP_HEIGHT;


    public SpaceInvaderHeuristics2() {
        this(null, 0.0);
    }

    public SpaceInvaderHeuristics2(String filename, double defaultWeightValue) {
        super(filename, defaultWeightValue);
    }

    @Override
    public List<StateFeature> getStateFeatures(State s) {
        stateFeatures.clear();
        stateFeatureMap.clear();
        ObjectInstance ship = s.getObject(SHIP_CLASS + "0");
        int shipX = ship.getIntValForAttribute(X);
        int[] radar = getProjectileRadar(ship, s);
        //Constant - always returns a value of 1
        stateFeatures.add(new StateFeature(CONSTANT, 1.0));
        // Will we die if we move / don't move?
        stateFeatures.add(new StateFeature(WILL_DIE_IF_STATIONARY, willDieIfDontMove(radar)));
        stateFeatures.add(new StateFeature(WILL_DIE_IF_MOVELEFT, willDieIfMoveLeft(radar)));
        stateFeatures.add(new StateFeature(WILL_DIE_IF_MOVERIGHT, willDieIfMoveRight(radar)));
        // Middle Shield?
        int shields = getShieldCover(s, shipX);
        stateFeatures.add(new StateFeature(GOT_MIDDLE_SHIELD, (shields & MIDDLE_SHIELD)*1.0));
        // Shooting features
        stateFeatures.add(new StateFeature(CAN_SHOOT, canFireMetric(ship)));
        stateFeatures.add(new StateFeature(ALIEN_RANGE, getFireMetric(s, ship)));
        stateFeatures.add(new StateFeature(ALIEN_RANGE_LEFT, getFireMetricForMove(s, ship, -1)));
        stateFeatures.add(new StateFeature(ALIEN_RANGE_RIGHT, getFireMetricForMove(s, ship, 1)));
        stateFeatures.add(new StateFeature(ALIEN_RANGE_WAIT, getFireMetricForMove(s, ship, 0)));
        stateFeatures.add(new StateFeature(WILL_HIT_PROJECTILE_IF_FIRE, willHitProjectileIfFire(shields, radar)));
        stateFeatures.add(new StateFeature(WILL_HIT_ENEMY_SHIELD_IF_FIRE, willHitEnemyShieldIfFire(s, ship)));
        stateFeatures.add(new StateFeature(WILL_HIT_ENEMY_BUILDING_IF_FIRE, willHitEnemyBuildingIfFire(s, ship)));
        // Shield area features
        stateFeatures.add(new StateFeature(SHIELDS_WILL_BLOCK_PROJECTILE, shieldsWillBlockProjectile(radar)));
        stateFeatures.add(new StateFeature(SHIELDS_WILL_EXPLODE_ALIEN, shieldsWillExplodeAlien(shipX, s)));
        stateFeatures.add(new StateFeature(SHIELDS_WILL_DEFEND_BUILDING, shieldsWillDefendBuilding(shipX, ship)));
        // Building features
        stateFeatures.add(new StateFeature(LIVES_LEFT, ship.getIntValForAttribute(LIVES)));
        stateFeatures.add(new StateFeature(BUILDING_COVER, getBuildingCover(shipX, s)));
        // Geography
        int enemyDX = getEnemyDX(s, shipX);
        int mcDX = getEnemyMCDX(s, shipX, MISSILE_CONTROL);
        int afDX = getEnemyMCDX(s, shipX, ALIEN_FACTORY);
        stateFeatures.add(new StateFeature(ENEMY_OFFSET_LEFT, getOffsetLeftMetric(enemyDX)));
        stateFeatures.add(new StateFeature(ENEMY_OFFSET_RIGHT, getOffsetRightMetric(enemyDX)));
        stateFeatures.add(new StateFeature(ENEMY_MC_OFFSET_LEFT, getOffsetLeftMetric(mcDX)));
        stateFeatures.add(new StateFeature(ENEMY_MC_OFFSET_RIGHT, getOffsetRightMetric(mcDX)));
        stateFeatures.add(new StateFeature(ENEMY_AF_OFFSET_LEFT, getOffsetLeftMetric(afDX)));
        stateFeatures.add(new StateFeature(ENEMY_AF_OFFSET_RIGHT, getOffsetRightMetric(afDX)));
        return stateFeatures;
    }

    @Override
    public List<ActionFeaturesQuery> getActionFeaturesSets(State s, List<GroundedAction> actions) {
        getStateFeatures(s); //Must populate stateFeatureMap with new values
        List<ActionFeaturesQuery> result = new ArrayList<>();
        for (GroundedAction action : actions) {
            List<StateFeature> actionFeatures = new ArrayList<>();
            ActionFeaturesQuery afq = new ActionFeaturesQuery(action, actionFeatures);
            result.add(afq);
            switch (action.actionName()) {
                case Nothing:
                    actionFeatures.add(stateToActionFeature(NOTHING, CONSTANT));
                    actionFeatures.add(stateToActionFeature(NOTHING, WILL_DIE_IF_STATIONARY));
                    actionFeatures.add(stateToActionFeature(NOTHING, ALIEN_RANGE_WAIT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ALIEN_RANGE_LEFT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ALIEN_RANGE_RIGHT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ALIEN_RANGE));
                    actionFeatures.add(stateToActionFeature(NOTHING, ENEMY_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ENEMY_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ENEMY_MC_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ENEMY_MC_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ENEMY_AF_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(NOTHING, ENEMY_AF_OFFSET_RIGHT));
                    actionFeatures.add(higherOrderFeature(NOTHING, CAN_SHOOT, ALIEN_RANGE_WAIT));
                    break;
                case MoveLeft:
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, CONSTANT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, WILL_DIE_IF_MOVELEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ALIEN_RANGE_WAIT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ALIEN_RANGE_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ALIEN_RANGE_RIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ALIEN_RANGE));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ENEMY_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ENEMY_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ENEMY_MC_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ENEMY_MC_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ENEMY_AF_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_LEFT, ENEMY_AF_OFFSET_RIGHT));
                    actionFeatures.add(higherOrderFeature(MOVE_LEFT, CAN_SHOOT, ALIEN_RANGE_LEFT));
                    break;
                case MoveRight:
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, CONSTANT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, WILL_DIE_IF_MOVERIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ALIEN_RANGE_WAIT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ALIEN_RANGE_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ALIEN_RANGE_RIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ALIEN_RANGE));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ENEMY_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ENEMY_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ENEMY_MC_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ENEMY_MC_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ENEMY_AF_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(MOVE_RIGHT, ENEMY_AF_OFFSET_RIGHT));
                    actionFeatures.add(higherOrderFeature(MOVE_RIGHT, CAN_SHOOT, ALIEN_RANGE_RIGHT));
                    break;
                case Shoot:
                    actionFeatures.add(stateToActionFeature(SHOOT, CONSTANT));
                    actionFeatures.add(stateToActionFeature(SHOOT, CAN_SHOOT));
                    actionFeatures.add(stateToActionFeature(SHOOT, GOT_MIDDLE_SHIELD));
                    actionFeatures.add(stateToActionFeature(SHOOT, WILL_HIT_PROJECTILE_IF_FIRE));
                    actionFeatures.add(stateToActionFeature(SHOOT, WILL_HIT_ENEMY_SHIELD_IF_FIRE));
                    actionFeatures.add(stateToActionFeature(SHOOT, WILL_HIT_ENEMY_BUILDING_IF_FIRE));
                    actionFeatures.add(stateToActionFeature(SHOOT, ALIEN_RANGE_WAIT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ALIEN_RANGE_LEFT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ALIEN_RANGE_RIGHT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ALIEN_RANGE));
                    actionFeatures.add(stateToActionFeature(SHOOT, ENEMY_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ENEMY_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ENEMY_MC_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ENEMY_MC_OFFSET_RIGHT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ENEMY_AF_OFFSET_LEFT));
                    actionFeatures.add(stateToActionFeature(SHOOT, ENEMY_AF_OFFSET_RIGHT));
                    break;
                case BuildAlienFactory:
                    actionFeatures.add(stateToActionFeature(BUILD_AF, CONSTANT));
                    actionFeatures.add(stateToActionFeature(BUILD_AF, LIVES_LEFT));
                    actionFeatures.add(stateToActionFeature(BUILD_AF, BUILDING_COVER));
                    break;
                case BuildMissileController:
                    actionFeatures.add(stateToActionFeature(BUILD_MC, CONSTANT));
                    actionFeatures.add(stateToActionFeature(BUILD_MC, LIVES_LEFT));
                    actionFeatures.add(stateToActionFeature(BUILD_MC, BUILDING_COVER));
                    break;
                case BuildShield:
                    actionFeatures.add(stateToActionFeature(BUILD_SHIELDS, CONSTANT));
                    actionFeatures.add(stateToActionFeature(BUILD_SHIELDS, LIVES_LEFT));
                    actionFeatures.add(stateToActionFeature(BUILD_SHIELDS, SHIELDS_WILL_BLOCK_PROJECTILE));
                    actionFeatures.add(stateToActionFeature(BUILD_SHIELDS, SHIELDS_WILL_EXPLODE_ALIEN));
                    actionFeatures.add(stateToActionFeature(BUILD_SHIELDS, SHIELDS_WILL_DEFEND_BUILDING));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid action: " + action.actionName());
            }

        }
        return result;
    }



    @Override
    protected Map<Integer, Double> createInitialWeightMap() {
        Map<Integer, Double> result = new HashMap<>();
        return result;
    }




    private double getOffsetLeftMetric(int dx) {
        return dx < 0? 1 + dx/DMAP_HEIGHT: 0.0;
    }

    private double getOffsetRightMetric(int dx) {
        return dx > 0? 1 - dx/DMAP_HEIGHT: 0.0;
    }

    private int getEnemyMCDX(State s, int shipX, String building) {
        ObjectInstance enemy = s.getObject(SHIP_CLASS + "1");
        int mcx = enemy.getIntValForAttribute(building);
        return mcx >= 0? mcx - shipX : MAP_WIDTH + 1;
    }

    private int getEnemyDX(State s, int shipX) {
        ObjectInstance enemy = s.getObject(SHIP_CLASS + "1");
        return enemy.getIntValForAttribute(X) - shipX;
    }

    private double shieldsWillDefendBuilding(int shipX, ObjectInstance ship) {
        int cover = Math.max(getPotentialShieldCover(shipX, ship, ALIEN_FACTORY),
                getPotentialShieldCover(shipX, ship, MISSILE_CONTROL));
        return cover/3.0;
    }

    private int getPotentialShieldCover(int shipX, ObjectInstance ship, String building) {
        int bx = ship.getIntValForAttribute(building);
        int dx = bx >= 0? Math.abs(bx - shipX) : 5;
        return dx < 3 ? 3 - dx : 0;
    }

    /**
     * Returns 1.0 if building a shield will immediately explode an alien
     */
    private double shieldsWillExplodeAlien(int shipX, State s) {
        List<ObjectInstance> aliens = s.getObjectsOfClass(ALIEN_CLASS);
        int damage = aliens.stream().map(
                alien -> {
                    Location l = Location.getObjectLocation(alien);
                    int dx = Math.abs(l.x - shipX);
                    if (dx > 1) return 0;
                    if (l.y == 2) return 10; //We will be killed
                    return (5 - l.y) * (2 - dx); // returns 1 - 8
                }
        ).max(Integer::compare).get();
        return 0.1*damage;
    }

    private double shieldsWillBlockProjectile(int[] radar) {
        boolean willBlock = radar[RADAR_RANGE] < 5
                || radar[RADAR_RANGE-1] < 5
                || radar[RADAR_RANGE+1] < 5;
        return willBlock? 1.0 : 0.0;
    }

    /**
     * Returns the degree of cover for a building. One point is given for each shield that would cover the building.
     * @return the fraction (out of 9) of the number of shields covering the building
     */
    private double getBuildingCover(int shipX, State s) {
        long cover = s.getObjectsOfClass(SHIELD_CLASS).stream().filter(
                shield -> (Math.abs(shield.getIntValForAttribute(X) - shipX) <= 1) && (shield.getIntValForAttribute(Y) < (MAP_HEIGHT / 2))
        ).count();
        return cover/9.0;
    }

    private double willHitProjectileIfFire(int shields, int[] radar) {
        //I won't hit a projectile if I hit my shield first
        if ((shields & MIDDLE_SHIELD) > 0) return 0.0;
        return radar[RADAR_RANGE] < MAP_HEIGHT? 1.0 : 0.0;
    }

    /**
     * NB!! This must be called after CAN_SHOOT, GOT_MIDDLE_SHIELD, WILL_HIT_PROJECTILE_IF_FIRE and ALIEN_RANGE
     * have been calculated
     * @return 1 if and only if an enemy shield (or my alien) will be hit. 0 if nothing, or anything
     * else will be hit
     */
    private double willHitEnemyShieldIfFire(State s, ObjectInstance ship) {
        if (!canPlayerFire(ship)) return 0.0;
        if (getFeature(GOT_MIDDLE_SHIELD).value > 0.0) return 0.0;
        if (getFeature(ALIEN_RANGE).value > 0.0) return 0.0;
        if (getFeature(WILL_HIT_PROJECTILE_IF_FIRE).value > 0.0) return 0.0;
        int shipX = ship.getIntValForAttribute(X);
        boolean willHitShield = s.getObjectsOfClass(SHIELD_CLASS).stream().filter(
                shield -> shield.getIntValForAttribute(Y) > MetaData.MAP_HEIGHT/2 && shield.getIntValForAttribute(X) == shipX
        ).count() > 0;
        return willHitShield? 1.0 : 0.0;
    }

    /**
     * NB!! This must be called after CAN_SHOOT, GOT_MIDDLE_SHIELD, ALIEN_RANGE, WILL_HIT_PROJECTILE_IF_FIRE
     * and WILL_HIT_ENEMY_SHIELD_IF_FIRE have been calculated
     * @return 1 if and only if an enemy building will be hit. 0 if nothing, or anything
     * else will be hit
     */
    private double willHitEnemyBuildingIfFire(State s, ObjectInstance ship) {
        if (!canPlayerFire(ship)) return 0.0;
        if (getFeature(GOT_MIDDLE_SHIELD).value > 0.0) return 0.0;
        if (getFeature(ALIEN_RANGE).value > 0.0) return 0.0;
        if (getFeature(WILL_HIT_PROJECTILE_IF_FIRE).value > 0.0) return 0.0;
        if (getFeature(WILL_HIT_ENEMY_SHIELD_IF_FIRE).value > 0.0) return 0.0;
        int shipX = ship.getIntValForAttribute(X);
        ObjectInstance enemy = s.getObject(SHIP_CLASS + "1");
        boolean willHit = isBuildingInLine(shipX, enemy, MISSILE_CONTROL) || isBuildingInLine(shipX, enemy, ALIEN_FACTORY);
        return willHit? 1.0 : 0.0;
    }

    private boolean isBuildingInLine(int shipX, ObjectInstance enemy, String building) {
        int bx = enemy.getIntValForAttribute(building);
        if (bx < 0) return false;
        return Math.abs(shipX - bx) <= 1;
    }

    /**
     * Simulates alien move and then calculates whether firing will strike. Makes deep copies of state and ship
     */
    private double getFireMetricForMove(State s, ObjectInstance ship, int dx) {
        int shipX = ship.getIntValForAttribute(X);
        if (dx + shipX <= 0 || dx + shipX >= MAP_WIDTH - 1) return 0.0;
        State nextState = s.copy();
        ObjectInstance nextShip = ship.copy();
        List<ObjectInstance> aliens = nextState.getObjectsOfClass(ALIEN_CLASS);
        SpaceInvaderMechanics.moveAliens(nextState, 1, aliens, nextShip);
        nextShip.setValue(X, shipX + dx);
        return getFireMetric(nextState, nextShip);
    }

    private double willDieIfMoveLeft(int[] radar) {
        boolean tooLate = radar[RADAR_RANGE-1] <= 2 || radar[RADAR_RANGE-2] <= 2;
        return tooLate? 1.0 : 0.0;
    }

    private double willDieIfMoveRight(int[] radar) {
        boolean tooLate = radar[RADAR_RANGE+1] <= 2 || radar[RADAR_RANGE+2] <= 2;
        return tooLate? 1.0 : 0.0;
    }

    private double willDieIfDontMove(int[] radar) {
        boolean tooLate = radar[RADAR_RANGE] <= 2 || radar[RADAR_RANGE-1] <= 2 || radar[RADAR_RANGE+1] <= 2;
        return tooLate? 1.0 : 0.0;
    }

    @Override
    public int numberOfFeatures() {
        return 0;
    }

    /**
     * Returns a radar map of incoming projectiles (missiles and bullets).
     * @param ship my ship instance
     * @param s the current state
     * @return a vector of 2*RADAR_RANGE + 1 danger values. radar[RADAR_RANGE] is in line with the ship.
     * The values are the number of moves before we get hit IF WE END UP IN LINE with the projectile after n moves.
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
        int shields = getShieldCover(s, shipX);
        for (ObjectInstance projectile : projectiles) {
            Location pLoc = Location.getObjectLocation(projectile);
            //Don't worry if shields are covering
            if (pLoc.x == shipX && (shields & MIDDLE_SHIELD) > 0 ) continue;
            if (pLoc.x == shipX + 1 && (shields & RIGHT_SHIELD) > 0 ) continue;
            if (pLoc.x == shipX - 1 && (shields & LEFT_SHIELD) > 0 ) continue;
            int dX = Math.abs(pLoc.x - shipX);
            if (dX <= RADAR_RANGE) {
                //Adjust the danger level for projectiles off to the side to account for us having to move there over time
                //i.e. a missile 3 to the left at y=2 will never be able to hit us
                int eventHorizon = dX > 2? dX-1: 1;
                int index = pLoc.x - shipX + RADAR_RANGE;
                int y = pLoc.y - eventHorizon;
                if (y <= 0) y = MAP_HEIGHT; //The missile is no longer a threat -- or it just hit us (which shouldn't be the case)
                value[index] = Math.min(value[index], y); //Get closest missile in column
            }
        }
        return value;
    }

    /**
     * Returns a metric score for the player firing in the current state. A score of 1 means the player should
     * definitely fire, a score of -1 means the player cannot, or should not fire (no target is hit).
     *
     * The metric is calculated as 1 - 2n/H
     * where
     *   H is map height
     *   n is turns before kill is registered
     * @param state the current state
     * @return the shooting kill metric
     */
    private static double getFireMetric(State state, ObjectInstance ship) {
        int hitRange = willHitAlienIfShoot(state, ship);
        return 2.0 - 2.0*hitRange/DMAP_HEIGHT;
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
    private boolean shootingWillHitShield(State s, int x) {
        return (getShieldCover(s, x) & MIDDLE_SHIELD) > 0;
    }

    /**
     * Determine whether the player is behind shields or not
     * @param s the current state
     * @param x the ship position to query
     * @return a 3-bit mask indicating level of shield protection.
     *   421 = left, middle and right ship element
     */
    private int getShieldCover(State s, int x) {
        int result = NO_SHIELD_COVER;
        List<ObjectInstance> shields = s.getObjectsOfClass(SHIELD_CLASS);
        Location l = new Location(x, 1);
        for (ObjectInstance shield : shields) {
            int sx = shield.getIntValForAttribute(X);
            if ((sx == l.x-1) && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) result += LEFT_SHIELD;
            if ((sx == l.x)   && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) result += MIDDLE_SHIELD;
            if ((sx == l.x+1) && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) result += RIGHT_SHIELD;
        }
        return result;
    }

    /**
     * Figures out if a missile fired now will hit an alien
     * @param s the current state
     * @param myShip my ship object
     * @return MAP_HEIGHT  if no alien will be hit, or the number of turns before the strike
     */
    private static int willHitAlienIfShoot(State s, ObjectInstance myShip) {
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
        return MetaData.MAP_HEIGHT;
    }

    public static final int LEFT_SHIELD = 4;
    public static final int MIDDLE_SHIELD = 2;
    public static final int RIGHT_SHIELD = 1;
    public static final int NO_SHIELD_COVER = 0;

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
