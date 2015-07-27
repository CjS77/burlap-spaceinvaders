package za.co.nimbus.game.rules;

import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.*;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;

import java.util.*;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * A stateless implementation for the transition dynamics for the Space invader game. All the functions are static,
 * allowing the same mechanics to be used for Single Agents, Stoachastic game implementations, or opponent strategies
 */
public class SpaceInvaderMechanics {
    private static Random rnd = RandomFactory.getDefault();
    private static final int DEBUG_CODE = 18067703;

    public static Random seedRNG(Integer seed) {
        rnd = seed == null? RandomFactory.getDefault() : RandomFactory.getMapped(seed);
        return rnd;
    }

    public static State updateEnvironmentPreAlienShoot(Domain domain, State state, Set<ObjectInstance> deadEntities) {
        ObjectInstance[] ships = new ObjectInstance[2];
        ships[0] = state.getObject(SHIP_CLASS + "0");
        ships[1] = state.getObject(SHIP_CLASS + "1");
        SpaceInvaderMechanics.moveProjectiles(state, MISSILE_CLASS);
        SpaceInvaderMechanics.handleCollisionsAndRemoveDeadEntities(domain, state, deadEntities);
        SpaceInvaderMechanics.moveProjectiles(state, BULLET_CLASS);
        SpaceInvaderMechanics.handleCollisionsAndRemoveDeadEntities(domain, state, deadEntities);
        SpaceInvaderMechanics.spawnAliensIfRequiredAndMove(domain, state, ships);
        SpaceInvaderMechanics.handleCollisionsAndRemoveDeadEntities(domain, state, deadEntities);
        return state;
    }

    public static State updateEnvironmentPostAlienShoot(Domain domain, State state,
            String myMove, String opponentMove, Set<ObjectInstance> deadEntities) {
        removeItemsThatMovedOffMap(domain, state);
        //Process Ship commands
        handleShipCommand(domain, 0, myMove, state);
        handleShipCommand(domain, 1, opponentMove, state);
        handleCollisionsAndRemoveDeadEntities(domain, state, deadEntities);
        //Update general game state
        updateShipStats(state, state.getObject(SHIP_CLASS + "0"));
        updateShipStats(state, state.getObject(SHIP_CLASS + "1"));
        updateMeta(state);
        for (ObjectInstance deadEntity : deadEntities) {
            state.removeObject(deadEntity);
        }
        return state;
    }

    public static State advanceGameByOneRound(Domain domain, State state, String myMove, String opponentMove) {
        Set<ObjectInstance> deadEntities = new HashSet<>();
        state = updateEnvironmentPreAlienShoot(domain, state, deadEntities);
        aliensShootIfPossible(domain, state);
        handleCollisionsAndRemoveDeadEntities(domain, state, deadEntities);
        return updateEnvironmentPostAlienShoot(domain, state, myMove, opponentMove, deadEntities);
    }

    public static boolean aliensWillFire(State s) {
        ObjectInstance meta = s.getFirstObjectOfClass(META_CLASS);
        int shotEnergy =  meta.getIntValForAttribute(ALIEN_SHOT_ENERGY);
        return shotEnergy >= MetaData.SHOT_COST;
    }

    private static void aliensShootIfPossible(Domain d, State state) {
        if (aliensWillFire(state)) {
            ObjectInstance[] ships = new ObjectInstance[2];
            ships[0] = state.getObject(SHIP_CLASS + "0");
            ships[1] = state.getObject(SHIP_CLASS + "1");
            ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
            int shotEnergy =  meta.getIntValForAttribute(ALIEN_SHOT_ENERGY);
            meta.setValue(ALIEN_SHOT_ENERGY, shotEnergy - MetaData.SHOT_COST);
            List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
            playersAliensFire(d, state, 0, aliens, ships);
            playersAliensFire(d, state, 1, aliens, ships);
        }
    }

    private static void playersAliensFire(Domain d, State state, int pNum, List<ObjectInstance> aliens, ObjectInstance[] ships) {
        int strategy = rnd.nextInt(3);
        ObjectInstance alien = getSniper(pNum, aliens, ships);
        if (strategy == 0) {
            alienShoots(d, state, pNum, alien);
        } else {
            alien = randomAlienShoots(pNum, alien, aliens);
            alienShoots(d, state, pNum, alien);
        }
    }

    /**
     * Find random alien to shoot AT pNum
     */
    private static ObjectInstance randomAlienShoots(int pNum, ObjectInstance excluded, List<ObjectInstance> aliens) {
        List<ObjectInstance> eligible = getAliensFromFirstTwoWaves(pNum, aliens, excluded);
        int pool = eligible.size();
        if (pool > 0) {
            int alien = rnd.nextInt(pool);
            return eligible.get(alien);
        } else return null;
    }

    /**
     * Count number of aliens in front 2 waves for player pNum
     */
    public static List<ObjectInstance> getAliensFromFirstTwoWaves(int pNum, List<ObjectInstance> aliens, ObjectInstance excluded) {
        int[] wy = getLastTwoWaveCoords(aliens, pNum);
        List<ObjectInstance> eligible = new ArrayList<>();
        for (ObjectInstance alien : aliens) {
            Location loc = Location.getObjectLocation(alien);
            if (alien != excluded && (loc.y == wy[0] || loc.y == wy[1]) ) eligible.add(alien);
        }
        return eligible;
    }

    /**
     * Find y-coords of closest two ENEMY alien waves
     */
    private static int[] getLastTwoWaveCoords(List<ObjectInstance> aliens, int pNum) {
        int[] result = pNum == 0 ? new int[]{1000, 1000} : new int[]{-1, -1};
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            if (ap != pNum) {
                Location loc = Location.getObjectLocation(alien);
                if (pNum==1) {
                    if (loc.y > result[0]) {
                        result[1] = result[0];
                        result[0] = loc.y;
                    } else if (loc.y < result[0] && loc.y > result[1]) {
                        result[1] = loc.y;
                    }
                }
                if (pNum==0) {
                    if (loc.y < result[0]) {
                        result[1] = result[0];
                        result[0] = loc.y;
                    } else if (loc.y > result[0] && loc.y < result[1]) {
                        result[1] = loc.y;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Assign an alien to shoot AT pNum
     */
    public static void alienShoots(Domain d, State state, int pNum, ObjectInstance alien) {
        if (alien != null) {
            ObjectInstance bullet = createNewObject(d, state, BULLET_CLASS);
            int ydir = pNum==0? -1: 1;
            Location aloc = Location.getObjectLocation(alien);
            Location bloc = new Location(aloc.x, aloc.y + ydir);
            setPosition(bullet, bloc);
            bullet.setValue(WIDTH, 1);
            bullet.setValue(PNUM, 1-pNum);
            state.addObject(bullet);
        }
    }

    /**
     * Find closest ENEMY alien to shoot AT pNum
     */
    public static ObjectInstance getSniper(int pNum, List<ObjectInstance> aliens, ObjectInstance[] ships) {
        int maxDistX = 1000;
        int maxDistY = 1000;
        Location shipLoc = Location.getObjectLocation(ships[pNum]);
        ObjectInstance closestAlien = null;
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            //The other player's aliens are shooting at me
            if (ap == 1-pNum) {
                Location loc = Location.getObjectLocation(alien);
                int dy = Math.abs(loc.y - shipLoc.y);
                if (dy <= maxDistY) {
                    int dx = Math.abs(loc.x - shipLoc.x);
                    if (dx < maxDistX) {
                        maxDistY = dy;
                        maxDistX = dx;
                        closestAlien = alien;
                    }
                }
            }
        }
        return closestAlien;
    }

    private static void spawnAliensIfRequiredAndMove(Domain d, State state, ObjectInstance[] ships) {
        List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
        int[][] onWall = getAlienPosition(aliens);
        ObjectInstance meta = state.getObject("MetaData");
        int actualPNum = meta.getIntValForAttribute(ACTUAL_PNUM);
        int roundNum = state.getFirstObjectOfClass(META_CLASS).getIntValForAttribute(ROUND_NUM);
        if (roundNum > 1) {
             if (onWall[0][0] > 0 && (onWall[0][1] > MetaData.MAP_HEIGHT/2 + 2)) spawnAlienRow(d, state, ships, 0);
             if (onWall[1][0] > 0 && (onWall[1][1] < MetaData.MAP_HEIGHT/2 - 2)) spawnAlienRow(d, state, ships, 1);
        }
        aliens = state.getObjectsOfClass(ALIEN_CLASS);
        for (ObjectInstance alien : aliens) {
            Location loc = Location.getObjectLocation(alien);
            int pNum = alien.getIntValForAttribute(PNUM);
            setPosition(alien, nextAlienLocation(loc, onWall[pNum][0], actualPNum));
        }

    }

    private static void spawnAlienRow(Domain d, State state, ObjectInstance[] ships, int pNum) {
        int waveSize = state.getFirstObjectOfClass(META_CLASS).getIntValForAttribute(ALIEN_WAVE_SIZE);
        //Wave size is one bigger if opponent has an alien factory
        if (ships[pNum].getIntValForAttribute(ALIEN_FACTORY) >= 0) waveSize++;
        int pos;
        for (int i=0; i<waveSize; i++) {
            pos = i * 3;
            addAlien(d, state, pNum, pos);
        }
    }

    private static ObjectInstance addAlien(Domain d, State s, int playerNum, int position) {
        ObjectInstance alien =createNewObject(d, s, ALIEN_CLASS);
        int y = MetaData.MAP_HEIGHT/2 + (playerNum == 0? 1 : -1);
        alien.setValue(X, MetaData.MAP_WIDTH - 1 - position);
        alien.setValue(Y, y);
        alien.setValue(WIDTH, 1);
        alien.setValue(PNUM, playerNum);
        s.addObject(alien);
        return alien;
    }

    private static void removeItemsThatMovedOffMap(Domain domain, State state) {
        Set<PropositionalFunction> offMapFns = domain.getPropositionlFunctionsMap().get(OffMap.NAME);
        for (PropositionalFunction pf : offMapFns) {
            List<GroundedProp> pfs = pf.getAllGroundedPropsForState(state);
            for (GroundedProp offMap : pfs) {
                if (offMap.isTrue(state)) {
                    state.removeObject(offMap.params[0]);
                    //DPrint.cf(DEBUG_CODE, "Removed object %s when it moved off map\n", offMap.params[0]);
                }
            }
        }
    }

    /**
     * @return a vector for each player with the following convention
     * 0 : co-ordinate of wall. Either 0 or MAPWIDTH-1 or -1 if not on a wall
     * 1 : y-coord of bottom-most alien for player 0 or top-most for layer 1
     */
    public static int[][] getAlienPosition(List<ObjectInstance> aliens) {
        int[][] onWall = new int[][]{
                new int[]{-1, 9999},
                new int[] {-1, -1}
        };
        for (ObjectInstance alien: aliens) {
            int pNum = alien.getIntValForAttribute(PNUM);
            Location loc = Location.getObjectLocation(alien);
            if (loc.x == 0 || loc.x == MetaData.MAP_WIDTH-1) {
                onWall[pNum][0] = loc.x;
            }
            if (pNum == 0 && loc.y < onWall[0][1]) onWall[0][1] = loc.y;
            if (pNum == 1 && loc.y > onWall[1][1]) onWall[1][1] = loc.y;
        }
        return onWall;
    }

    /**
     * Calculates next position for an alient at cur.
     * @param cur - cur pos
     * @param atWall - 0, MAP_WIDTH-1, or -1 for left, right, or none
     * @return - next pos
     */
    public static Location nextAlienLocation(Location cur, int atWall, int actualPNum) {
        int dirswitch = actualPNum == 0? 1 : -1;
        int xdir = dirswitch * ((cur.y % 2 == 0)? -1 : 1);
        int ydir = (cur.y > MetaData.MAP_HEIGHT/2)? 1 : -1;
        //Must move down
        if ((atWall == 0 && xdir == -1) || (atWall > 0 && xdir == 1)) return new Location(cur.x, cur.y + ydir);
        //normal move
        return new Location(cur.x + xdir, cur.y);
    }

    private static void moveProjectiles(State state, String objectClass) {
        List<ObjectInstance> missiles = state.getObjectsOfClass(objectClass);
        for (ObjectInstance missile : missiles) {
            int y = missile.getIntValForAttribute(Y);
            int pNum = missile.getIntValForAttribute(PNUM);
            missile.setValue(Y, pNum == 0? y+1 : y-1);
            //if (objectClass.equals(MISSILE_CLASS)) checkMissileBounds(state, missile, pNum, y);
        }
    }

    /**
     * Removes missile if it moves off map -- this is actually done automatically
     */
//    @Deprecated
//    private static void checkMissileBounds(State s, ObjectInstance missile, int pNum, int y) {
//        if ((pNum == 0 && y >= MetaData.MAP_HEIGHT) || (pNum == 1 && y <= 0 )) {
//            s.removeObject(missile);
//            ObjectInstance owner = s.getObject(SHIP_CLASS + pNum);
//            decAttribute(owner, MISSILE_COUNT);
//        }
//    }

    public static State handleShipCommand(Domain d, int pNum, String command, State state) {
        ObjectInstance ship = state.getObject(SHIP_CLASS + pNum);
        Location shipLoc = Location.getObjectLocation(ship);
        switch (command) {
            case MoveLeft:
                if (pNum == 0) decAttribute(ship, X);
                else incrAttribute(ship, X);
                break;
            case MoveRight:
                if (pNum == 0) incrAttribute(ship, X);
                else decAttribute(ship, X);
                break;
            case Shoot:
                buildMissile(d, state, shipLoc, pNum);
                break;
            case BuildAlienFactory:
                buildBuilding(d, ALIEN_FACTORY_CLASS, ALIEN_FACTORY, state, ship, shipLoc, pNum);
                break;
            case BuildMissileController:
                buildBuilding(d, MISSILE_CONTROLLER_CLASS, MISSILE_CONTROL, state, ship, shipLoc, pNum);
                break;
            case BuildShield:
                buildShields(d, state, ship, shipLoc, pNum);
            case Nothing:
            default:
        }
        return state;
    }

    private static void buildShields(Domain d, State state, ObjectInstance ship, Location shipLoc, int pNum) {
        int dir = pNum==0? 1 : -1;
        for (int i=-1; i<2; i++) {
            for (int j = 1; j <= 3; j++) {
                Location loc = new Location(shipLoc.x + i, shipLoc.y + dir * j);
                if (getObjectAt(state, loc.x, loc.y) == null) buildShield(d, state, loc);
            }
        }
        decAttribute(ship, LIVES);
    }

    private static String buildShield(Domain d, State state, Location loc) {
        ObjectInstance shield = createNewObject(d, state, SHIELD_CLASS);
        setPosition(shield, loc);
        shield.setValue(WIDTH, 1);
        state.addObject(shield);
        return shield.getName();
    }

    private static void buildMissile(Domain d, State state, Location loc, int pNum) {
        ObjectInstance missile = createNewObject(d, state, MISSILE_CLASS);
        Location objLoc = new Location(loc.x, pNum==0? loc.y + 1 : loc.y -1 );
        setPosition(missile, objLoc);
        missile.setValue(PNUM, pNum);
        missile.setValue(WIDTH, 1);
        state.addObject(missile);
        //Update Ship records
        //incrAttribute(ship, MISSILE_COUNT);
    }

    private static void buildBuilding(Domain d, String objectClass, String playerAttr, State state, ObjectInstance ship, Location loc, int pNum) {
        ObjectInstance building = createNewObject(d, state, objectClass);
        Location objLoc = new Location(loc.x, pNum==0? loc.y - 1 : loc.y  + 1 );
        setPosition(building, objLoc);
        building.setValue(PNUM, pNum);
        building.setValue(WIDTH, 3);
        state.addObject(building);
        //Update Ship records
        ship.setValue(playerAttr, loc.x);
        decAttribute(ship, LIVES);
    }

    /**
     * @param loc - expecting absolute (global) location corrids
     */
    private static void setPosition(ObjectInstance ob, Location loc) {
        ob.setValue(X, loc.x);
        ob.setValue(Y, loc.y);
    }

    //Creates a new object with a guaranteed unique name
    private static ObjectInstance createNewObject(Domain domain, State state, String objectClass) {
        int counter = state.getObjectsOfClass(objectClass).size();
        while (state.getObject(objectClass+counter) != null) counter++;
        return new ObjectInstance(domain.getObjectClass(objectClass), objectClass+counter);
    }



    private static void incrAttribute(ObjectInstance ob, String attr) {
        int val = ob.getIntValForAttribute(attr);
        ob.setValue(attr, val+1);
    }

    private static void decAttribute(ObjectInstance ob, String attr) {
        int val = ob.getIntValForAttribute(attr);
        ob.setValue(attr, val - 1);
    }

    private static void updateMeta(State state) {
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        incrAttribute(meta, ROUND_NUM);
        incrAttribute(meta, ALIEN_SHOT_ENERGY);
        int roundNum = meta.getIntValForAttribute(ROUND_NUM);
        if (roundNum == MetaData.WAVE_BUMP_ROUND) incrAttribute(meta, ALIEN_WAVE_SIZE);
    }

    private static void updateShipStats(State s, ObjectInstance ship) {
        int respawn_timer = ship.getIntValForAttribute(RESPAWN_TIME);
        if (respawn_timer > 0) {
            ship.setValue(RESPAWN_TIME, respawn_timer-1);
        }
        if (respawn_timer == 0) {
            ship.setValue(RESPAWN_TIME, -1);
            ship.setValue(X, MetaData.MAP_WIDTH/2);
        }
        ship.setValue(MISSILE_COUNT, getMissileCount(s, ship));
    }

    private static int getMissileCount(State s, ObjectInstance ship) {
        List<ObjectInstance> missiles = s.getObjectsOfClass(MISSILE_CLASS);
        int pNum = ship.getIntValForAttribute(PNUM);
        int missileCount = 0;
        for (ObjectInstance missile : missiles) {
            if (missile.getIntValForAttribute(PNUM) == pNum) missileCount++;
        }
        return missileCount;
    }

    private static void handleCollisionsAndRemoveDeadEntities(Domain domain, State state, Set<ObjectInstance> deadEntities) {
        ObjectInstance[] ships = new ObjectInstance[2];
        ships[0] = state.getObject(SHIP_CLASS + "0");
        ships[1] = state.getObject(SHIP_CLASS + "1");
        Set<PropositionalFunction> collisionFunctions = domain.getPropositionlFunctionsMap().get(Collision.NAME);
        List<GroundedProp> actualCollisions = new ArrayList<>();
        //Loop over each type of collision, e.g. missile - alien
        for (PropositionalFunction cf : collisionFunctions) {
            // Get the collision instances for the given class
            List<GroundedProp> collisions = cf.getAllGroundedPropsForState(state);
            for (GroundedProp collision: collisions) {
                // Handle the collision mechanics
                if (collision.isTrue(state)) {
                    actualCollisions.add(collision);
                }
            }
        }
        for (GroundedProp actualCollision : actualCollisions) {
            handleCollision(state, ships, actualCollision.pf.getName(), actualCollision, deadEntities);
        }
        for (ObjectInstance deadEntity : deadEntities) {
            state.removeObject(deadEntity);
        }
        deadEntities.clear();
    }

    private static void handleCollision(State state, ObjectInstance[] ships, String collisionClass, GroundedProp collision, Set<ObjectInstance> deadEntities) {
        switch (collisionClass) {
            case MISSILE_CLASS+ALIEN_CLASS+ Collision.NAME:
                addPossibleKill(state, ships, collision);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+ALIEN_FACTORY_CLASS+Collision.NAME:
                addPossibleKill(state, ships, collision);
                recordBuildingStatus(state, ships, collision, ALIEN_FACTORY);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+MISSILE_CONTROLLER_CLASS+Collision.NAME:
                addPossibleKill(state, ships, collision);
                recordBuildingStatus(state, ships, collision, MISSILE_CONTROL);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+MISSILE_CLASS+Collision.NAME:
                //recordMissileLoss(state.getObject(collision.params[0]), ships);
                //recordMissileLoss(state.getObject(collision.params[1]), ships);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+SHIELD_CLASS+Collision.NAME:
            case MISSILE_CLASS+BULLET_CLASS+Collision.NAME:
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case BULLET_CLASS+SHIELD_CLASS+Collision.NAME:
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+SHIP_CLASS+Collision.NAME:
                int pNum = addPossibleKill(state, ships, collision);
                assert (pNum>=0);
                freezePlayer(ships[1 - pNum]);
                //Don't remove the ship
                state.removeObject(collision.params[0]);
                break;
            case BULLET_CLASS+ALIEN_FACTORY_CLASS+Collision.NAME:
                recordBuildingStatus(state, ships, collision, ALIEN_FACTORY);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case BULLET_CLASS+MISSILE_CONTROLLER_CLASS+Collision.NAME:
                recordBuildingStatus(state, ships, collision, MISSILE_CONTROL);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case BULLET_CLASS+SHIP_CLASS+Collision.NAME:
                ObjectInstance ship = state.getObject(collision.params[1]);
                freezePlayer(ship);
                state.removeObject(collision.params[0]);
                break;
            case ALIEN_CLASS+SHIELD_CLASS+Collision.NAME:
            case ALIEN_CLASS+ALIEN_FACTORY_CLASS+Collision.NAME:
            case ALIEN_CLASS+MISSILE_CONTROLLER_CLASS+Collision.NAME:
            case ALIEN_CLASS+SHIP_CLASS+Collision.NAME:
                explodeAlien(state, collision, ships);
                break;
        }
    }

    private static void explodeAlien(State state, GroundedProp collision, ObjectInstance[] ships) {
        //Simulate collision for the 3x3 area around the impact
        ObjectInstance alien = state.getObject(collision.params[0]);
        state.removeObject(alien);
        Location loc = Location.getObjectLocation(alien);
        for (int i=-1; i<2; i++) {
            for (int j=-1; j<2; j++) {
                ObjectInstance ob;
                if (i == 0 && j == 0 )
                    ob = state.getObject(collision.params[1]);
                else ob = getObjectAt(state, loc.x + i, loc.y + j);
                if (ob != null) {
                    String obClass = ob.getTrueClassName();
                    switch (obClass) {
                        case SHIP_CLASS:
                            freezePlayer(ob);
                            break;
                        case ALIEN_CLASS:
                            throw new IllegalStateException("Aliens shouldn't explode other aliens");
                        case BULLET_CLASS:
                        case SHIELD_CLASS:
                            state.removeObject(ob);
                            break;
                        case MISSILE_CLASS:
                            //recordMissileLoss(ob, ships);
                            state.removeObject(ob);
                            break;
                        case ALIEN_FACTORY_CLASS:
                            recordBuildingStatus(state, ships, collision, ALIEN_FACTORY);
                            state.removeObject(ob);
                            break;
                        case MISSILE_CONTROLLER_CLASS:
                            recordBuildingStatus(state, ships, collision, MISSILE_CONTROL);
                            state.removeObject(ob);
                            break;
                        default:
                            throw new IllegalStateException("Some unexpected object was exploded by an alien");
                    }
                }
            }
        }
    }

    private static ObjectInstance getObjectAt(State state, int x, int y) {
        for (ObjectInstance o : state.getAllObjects()) {
            if (!o.getObjectClass().hasAttribute(X)) continue;
            int ox = o.getIntValForAttribute(X);
            int ow = o.getIntValForAttribute(WIDTH);
            int oy = o.getIntValForAttribute(Y);
            if (oy == y && Math.abs(x - ox) < ow) return o;
        }
        return null;
    }

    private static void freezePlayer(ObjectInstance ship) {
        if (isFrozen(ship)) {
            DPrint.cl(DEBUG_CODE, "Hmm. Trying to freeze an already frozen ship. This should not happen");
        }
        //Move off the map
        ship.setValue(X, -3);
        //Reduce lives by 1
        ship.setValue(LIVES, ship.getIntValForAttribute(LIVES)-1);
        //Set respawn timer
        ship.setValue(RESPAWN_TIME, 3);
    }

    private static void queueRemoveEntities(State state, GroundedProp collision, Set<ObjectInstance> deadEntities) {
        deadEntities.add(state.getObject(collision.params[0]));
        deadEntities.add(state.getObject(collision.params[1]));
    }

//    private static void recordMissileLoss(ObjectInstance m, ObjectInstance[] ships) {
//        int pNum = m.getIntValForAttribute(PNUM);
//        decAttribute(ships[pNum], MISSILE_COUNT);
//    }

    private static void recordBuildingStatus(State state, ObjectInstance[] ships, GroundedProp collision, String buildingAttr) {
        ObjectInstance mc = state.getObject(collision.params[1]);
        int pNum = mc.getIntValForAttribute(PNUM);
        ships[pNum].setValue(buildingAttr, -1);
    }


    /**
     * Checks a missile collision for validity (right player owns the entities) and scores a kill
     * RecordMissileLoss is called
     * Return the player num for player that scored the kill or -1 if no kill
     */
    private static int addPossibleKill(State state, ObjectInstance[] ships, GroundedProp collision) {
        ObjectInstance m = state.getObject(collision.params[0]);
        ObjectInstance o = state.getObject(collision.params[1]);
        int pNum0 = m.getIntValForAttribute(PNUM);
        int pNum1 = o.getIntValForAttribute(PNUM);
        //recordMissileLoss(m, ships);
        //Frozen ships are immune
        if (o.getTrueClassName().equals(SHIP_CLASS) && isFrozen(o))
            throw new IllegalStateException("Somehow we hit a frozen ship");
        if (pNum0 != pNum1) {
            incrAttribute(ships[pNum0], KILLS);
            return pNum0;
        }
        return -1;
    }

    private static boolean isFrozen(ObjectInstance ship) {
        int timerVal = ship.getIntValForAttribute(RESPAWN_TIME);
        return timerVal >= 0;
    }
}
