package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.GroundedAction;
import za.co.nimbus.game.constants.Attributes;
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
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * A second attempt at a set of heuristics
 */
public class SpaceInvaderHeuristics3 extends Heuristics {

    private static final int CONSTANT = 1;
    private static final int ROUND_NUM = 2;
    private static final int LIVES_LEFT = 4;
    private static final int ENEMY_LIVES_LEFT = 5;
    private static final int MY_SHIP_X = 6;
    private static final int ENEMY_SHIP_X = 7;
    private static final int MY_MC_POS = 8;
    private static final int ENEMY_MC_POS = 9;
    private static final int MY_AF_POS = 10;
    private static final int ENEMY_AF_POS = 11;
    private static final int WILL_DIE_IF_STATIONARY = 12;
    private static final int WILL_DIE_IF_MOVELEFT = 13;
    private static final int WILL_DIE_IF_MOVERIGHT = 14;
    private static final int GOT_MIDDLE_SHIELD = 15;
    private static final int WILL_HIT_ENEMY_SHIELD_IF_FIRE = 16;
    private static final int WILL_HIT_ENEMY_BUILDING_IF_FIRE = 17;
    private static final int WILL_HIT_PROJECTILE_IF_FIRE = 18;
    private static final int BUILDING_COVER = 19;
    private static final int SHIELDS_WILL_BLOCK_PROJECTILE = 20;
    private static final int SHIELDS_WILL_EXPLODE_ALIEN = 21;
    private static final int SHIELDS_WILL_DEFEND_BUILDING = 22;
    private static final int CAN_SHOOT = 23;
    private static final int GET_KILL_AFTER_S = 24;
    private static final int GET_KILL_AFTER_LS = 25;
    private static final int GET_KILL_AFTER_NS = 26;
    private static final int GET_KILL_AFTER_LLS = 27;
    private static final int GET_KILL_AFTER_LNS = 28;
    private static final int GET_KILL_AFTER_RRS = 29;
    private static final int GET_KILL_AFTER_RNS = 30;
    private static final int GET_KILL_AFTER_RS = 31;
    //Shield positions
    private static final int MY_SHIELD_OFFSET = 35;
    private static final int ENEMY_SHIELD_OFFSET = 55;
    private static final int[] MY_SHIELD_COVER = new int[MAP_WIDTH];
    private static final int[] ENEMY_SHIELD_COVER = new int[MAP_WIDTH];
    static {
        for (int i=0; i<MAP_WIDTH; i++) {
            MY_SHIELD_COVER[i] = MY_SHIELD_OFFSET + i;
            ENEMY_SHIELD_COVER[i] = ENEMY_SHIELD_OFFSET + i;
        }
    }

    //Action flags
    private static final int NOTHING    = 1000000;
    private static final int MOVE_LEFT  = 2000000;
    private static final int MOVE_RIGHT = 3000000;
    private static final int SHOOT      = 4000000;
    private static final int BUILD_MC   = 5000000;
    private static final int BUILD_AF   = 6000000;
    private static final int BUILD_SHIELDS = 7000000;

    private static int[] commonFeatures = new int[] {CONSTANT, ROUND_NUM, LIVES_LEFT, ENEMY_LIVES_LEFT, MY_SHIP_X,
            ENEMY_SHIP_X, MY_MC_POS, ENEMY_MC_POS, MY_AF_POS, ENEMY_AF_POS, CAN_SHOOT, WILL_DIE_IF_MOVELEFT, WILL_DIE_IF_MOVERIGHT,
            WILL_DIE_IF_STATIONARY};
    final int RADAR_RANGE = 3;
    private static final double DMAP_HEIGHT = (double) MAP_HEIGHT;
    private static final double DMAP_WIDTH = (double) MAP_WIDTH;
    private final Domain domain;


    public SpaceInvaderHeuristics3(Domain d) {
        this(null, 0.0, d);
    }

    public SpaceInvaderHeuristics3(String filename, double defaultWeightValue, Domain d) {
        super(filename, defaultWeightValue);
        this.domain = d;
    }

    @Override
    public List<StateFeature> getStateFeatures(State s) {
        stateFeatures.clear();
        stateFeatureMap.clear();
        ObjectInstance ship = s.getObject(SHIP_CLASS + "0");
        ObjectInstance enemy = s.getObject(SHIP_CLASS + "1");
        ObjectInstance meta = s.getObject("MetaData");
        int shipX = ship.getIntValForAttribute(X);
        int enemyX = enemy.getIntValForAttribute(X);
        int[] radar = getProjectileRadar(ship, s);
        //Constant - always returns a value of 1
        stateFeatures.add(new StateFeature(CONSTANT, 1.0));
        stateFeatures.add(new StateFeature(ROUND_NUM, meta.getIntValForAttribute(Attributes.ROUND_NUM)/200.0));
        stateFeatures.add(new StateFeature(LIVES_LEFT, ship.getIntValForAttribute(LIVES)));
        stateFeatures.add(new StateFeature(ENEMY_LIVES_LEFT, enemy.getIntValForAttribute(LIVES)));
        stateFeatures.add(new StateFeature(MY_SHIP_X, shipX/DMAP_WIDTH));
        stateFeatures.add(new StateFeature(ENEMY_SHIP_X, enemyX/DMAP_WIDTH));
        stateFeatures.add(new StateFeature(MY_MC_POS, getBuildingX(ship, MISSILE_CONTROL)/DMAP_WIDTH));
        stateFeatures.add(new StateFeature(ENEMY_MC_POS, getBuildingX(enemy, MISSILE_CONTROL)/DMAP_WIDTH));
        stateFeatures.add(new StateFeature(MY_AF_POS, getBuildingX(ship, ALIEN_FACTORY)/DMAP_WIDTH));
        stateFeatures.add(new StateFeature(ENEMY_AF_POS, getBuildingX(enemy, ALIEN_FACTORY)/DMAP_WIDTH));
        // Will we die if we move / don't move?
        stateFeatures.add(new StateFeature(WILL_DIE_IF_STATIONARY, willDieIfDontMove(radar)));
        stateFeatures.add(new StateFeature(WILL_DIE_IF_MOVELEFT, willDieIfMoveLeft(radar)));
        stateFeatures.add(new StateFeature(WILL_DIE_IF_MOVERIGHT, willDieIfMoveRight(radar)));
        // Middle Shield?
        int shields = getShieldCover(s, shipX);
        stateFeatures.add(new StateFeature(GOT_MIDDLE_SHIELD, (shields & MIDDLE_SHIELD)*1.0));
        // Shooting features
        stateFeatures.add(new StateFeature(CAN_SHOOT, canFireMetric(ship)));
        stateFeatures.add(new StateFeature(WILL_HIT_PROJECTILE_IF_FIRE, willHitProjectileIfFire(shields, radar)));
        stateFeatures.add(new StateFeature(WILL_HIT_ENEMY_SHIELD_IF_FIRE, willHitEnemyShieldIfFire(s, ship)));
        stateFeatures.add(new StateFeature(WILL_HIT_ENEMY_BUILDING_IF_FIRE, willHitEnemyBuildingIfFire(s, ship)));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_S, getKillRange(s, new String[] {Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_NS, getKillRange(s, new String[] {Nothing, Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_LS, getKillRange(s, new String[] {MoveLeft, Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_LLS, getKillRange(s, new String[] {MoveLeft, MoveLeft, Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_LNS, getKillRange(s, new String[] {MoveLeft, Nothing, Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_RS, getKillRange(s, new String[] {MoveRight, Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_RRS, getKillRange(s, new String[] {MoveRight, MoveRight, Shoot})));
        stateFeatures.add(new StateFeature(GET_KILL_AFTER_RNS, getKillRange(s, new String[] {MoveRight, Nothing, Shoot})));
        //Shield coverage - one for each x co-ordinate ranging from 0 (no cover) to 1 (3x cover)
        addShieldCoverage(s);
        // Shield area features
        stateFeatures.add(new StateFeature(SHIELDS_WILL_BLOCK_PROJECTILE, shieldsWillBlockProjectile(radar)));
        stateFeatures.add(new StateFeature(SHIELDS_WILL_EXPLODE_ALIEN, shieldsWillExplodeAlien(shipX, s)));
        stateFeatures.add(new StateFeature(SHIELDS_WILL_DEFEND_BUILDING, shieldsWillDefendBuilding(shipX, ship)));
        // Building features
        stateFeatures.add(new StateFeature(BUILDING_COVER, getBuildingCover(shipX, s)));
        // Alien ranges
        //addAlienTargetFeatures(s);
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
                    addStateActionFeatures(actionFeatures, NOTHING, commonFeatures);
                    addStateActionFeatures(actionFeatures, NOTHING, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, NOTHING, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, NOTHING, GET_KILL_AFTER_S, GET_KILL_AFTER_NS);
                    break;
                case MoveLeft:
                    addStateActionFeatures(actionFeatures, MOVE_LEFT, commonFeatures);
                    addStateActionFeatures(actionFeatures, MOVE_LEFT, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, MOVE_LEFT, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, MOVE_LEFT, GET_KILL_AFTER_S, GET_KILL_AFTER_LS, GET_KILL_AFTER_LNS, GET_KILL_AFTER_LLS);
                    break;
                case MoveRight:
                    addStateActionFeatures(actionFeatures, MOVE_RIGHT, commonFeatures);
                    addStateActionFeatures(actionFeatures, MOVE_RIGHT, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, MOVE_RIGHT, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, MOVE_RIGHT, GET_KILL_AFTER_S, GET_KILL_AFTER_RS, GET_KILL_AFTER_RNS, GET_KILL_AFTER_RRS);
                    break;
                case Shoot:
                    addStateActionFeatures(actionFeatures, SHOOT, commonFeatures);
                    addStateActionFeatures(actionFeatures, SHOOT, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, SHOOT, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, SHOOT, GET_KILL_AFTER_S, GET_KILL_AFTER_NS, GET_KILL_AFTER_LS, GET_KILL_AFTER_LNS, GET_KILL_AFTER_LLS,
                            GET_KILL_AFTER_RS, GET_KILL_AFTER_RNS, GET_KILL_AFTER_RRS, GOT_MIDDLE_SHIELD, WILL_HIT_ENEMY_BUILDING_IF_FIRE,
                            WILL_HIT_PROJECTILE_IF_FIRE, WILL_HIT_ENEMY_SHIELD_IF_FIRE);
                    break;
                case BuildAlienFactory:
                    addStateActionFeatures(actionFeatures, BUILD_AF, commonFeatures);
                    addStateActionFeatures(actionFeatures, BUILD_AF, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, BUILD_AF, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, BUILD_AF, BUILDING_COVER, GET_KILL_AFTER_S);
                    break;
                case BuildMissileController:
                    addStateActionFeatures(actionFeatures, BUILD_MC, commonFeatures);
                    addStateActionFeatures(actionFeatures, BUILD_MC, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, BUILD_MC, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, BUILD_MC, BUILDING_COVER, GET_KILL_AFTER_S);
                    break;
                case BuildShield:
                    addStateActionFeatures(actionFeatures, BUILD_SHIELDS, commonFeatures);
                    addStateActionFeatures(actionFeatures, BUILD_SHIELDS, MY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, BUILD_SHIELDS, ENEMY_SHIELD_COVER);
                    addStateActionFeatures(actionFeatures, BUILD_SHIELDS, BUILDING_COVER, GET_KILL_AFTER_S,
                            SHIELDS_WILL_BLOCK_PROJECTILE, SHIELDS_WILL_DEFEND_BUILDING, SHIELDS_WILL_EXPLODE_ALIEN);
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

    private void addShieldCoverage(State s) {
        int[][] cover = new int[2][MAP_WIDTH];
        for (int i=0; i<MAP_WIDTH; i++) {
            cover[0][i] = 0;
            cover[1][i] = 0;
        }
        List<ObjectInstance> shields = s.getObjectsOfClass(SHIELD_CLASS);
        for (ObjectInstance shield : shields) {
            Location l = Location.getObjectLocation(shield);
            int pNum = l.y < 10? 0 : 1;
            cover[pNum][l.x]++;
        }
        for (int i=0; i<MAP_WIDTH; i++) {
            stateFeatures.add(new StateFeature(MY_SHIELD_OFFSET + i, cover[0][i]/3.0));
            stateFeatures.add(new StateFeature(ENEMY_SHIELD_OFFSET + i, cover[1][i]/3.0));
        }
    }

    private double canFireMetric(ObjectInstance ship) {
        return canPlayerFire(ship)? 1.0 : 0.0;
    }

    private int getBuildingX(ObjectInstance ship, String building) {
        int bx = ship.getIntValForAttribute(building);
        return bx > 0? bx : 0;
    }

    private int getKillRange(State state, String[] moveSequence) {
        State s= state.copy();
        Set<ObjectInstance> deadEntities = new HashSet<>();
        ObjectInstance thisMissile = null;
        for (int i = 0; i < 9; i++){
            deadEntities.clear();
            SpaceInvaderMechanics.moveProjectiles(s, MISSILE_CLASS);
            if (gotKill(s, thisMissile)) return i;
            SpaceInvaderMechanics.handleCollisionsAndRemoveDeadEntities(domain, s, deadEntities);
            SpaceInvaderMechanics.moveProjectiles(s, BULLET_CLASS);
            SpaceInvaderMechanics.handleCollisionsAndRemoveDeadEntities(domain, s, deadEntities);
            SpaceInvaderMechanics.spawnAliensIfRequiredAndMove(domain, s, 0, s.getObject(SHIP_CLASS+"0"));
            SpaceInvaderMechanics.spawnAliensIfRequiredAndMove(domain, s, 1, s.getObject(SHIP_CLASS+"1"));
            if (gotKill(s, thisMissile)) return i;
            SpaceInvaderMechanics.handleCollisionsAndRemoveDeadEntities(domain, s, deadEntities);
            String move = i < moveSequence.length? moveSequence[i] : Nothing;
            SpaceInvaderMechanics.updateEnvironmentPostAlienShoot(domain, s, move, Nothing, deadEntities);
            if (move.equals(Shoot)) {
                try {
                    thisMissile = s.getObjectsOfClass(MISSILE_CLASS).stream().filter(
                            m -> m.getIntValForAttribute(PNUM) == 0 && m.getIntValForAttribute(Y) == 2
                    ).findFirst().get();
                } catch (NoSuchElementException e) {
                    return 1; //Missile hit on impact
                }
            }

        }
        return 0; //Whiffed
    }

    private boolean gotKill(State s, ObjectInstance thisMissile) {
        if (thisMissile == null) return false;
        PropositionalFunction pf = domain.getPropFunction(MISSILE_CLASS + ALIEN_CLASS + Collision.NAME);
        boolean hit = pf.getAllGroundedPropsForState(s).stream().filter(
                gp -> gp.params[0].equals(thisMissile.getName())
                        && s.getObject(gp.params[1]).getIntValForAttribute(PNUM) == 1
        ).anyMatch(
                gp -> gp.isTrue(s)
        );
        return hit;
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
        //if (getFeature(ALIEN_RANGE).value > 0.0) return 0.0;  //TODO
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
        //if (getFeature(ALIEN_RANGE).value > 0.0) return 0.0; TODO
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
     * @param ship the player's ship object
     * @return true if the player is able to fire a missile, false otherwise
     */
    private boolean canPlayerFire(ObjectInstance ship) {
        boolean hasMissileController = ship.getIntValForAttribute(MISSILE_CONTROL) >= 0;
        int missileCount = ship.getIntValForAttribute(MISSILE_COUNT);
        return missileCount == 0 || (missileCount == 1 && hasMissileController);
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

    public static final int LEFT_SHIELD = 4;
    public static final int MIDDLE_SHIELD = 2;
    public static final int RIGHT_SHIELD = 1;
    public static final int NO_SHIELD_COVER = 0;



}
