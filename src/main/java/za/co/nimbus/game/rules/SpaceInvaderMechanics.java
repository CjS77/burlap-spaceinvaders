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
import static za.co.nimbus.game.constants.MetaData.*;

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
        SpaceInvaderMechanics.spawnAliensIfRequiredAndMove(domain, state, 0, ships[0]);
        SpaceInvaderMechanics.spawnAliensIfRequiredAndMove(domain, state, 1, ships[1]);
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
        aliensShootIfPossible(domain, state, 0);
        aliensShootIfPossible(domain, state, 1);
        handleCollisionsAndRemoveDeadEntities(domain, state, deadEntities);
        return updateEnvironmentPostAlienShoot(domain, state, myMove, opponentMove, deadEntities);
    }

    public static boolean aliensWillFire(ObjectInstance ship) {
        int shotEnergy = ship.getIntValForAttribute(ALIEN_SHOT_ENERGY);
        return shotEnergy + 1 >= MetaData.SHOT_COST;
    }

    public static boolean aliensWillFire(State s, int pNum) {
        ObjectInstance ship = s.getObject(SHIP_CLASS + pNum);
        return aliensWillFire(ship);
    }

    private static void aliensShootIfPossible(Domain d, State state, int pNum) {
        ObjectInstance ship = state.getObject(SHIP_CLASS + "0");
        if (aliensWillFire(ship)) {
            List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
            if (playersAliensFire(d, state, pNum, aliens, ship)) {
                int shotEnergy =  ship.getIntValForAttribute(ALIEN_SHOT_ENERGY);
                ship.setValue(ALIEN_SHOT_ENERGY, shotEnergy - MetaData.SHOT_COST);
            }
        }
    }

    private static boolean playersAliensFire(Domain d, State state, int pNum, List<ObjectInstance> aliens, ObjectInstance ship) {
        int strategy = rnd.nextInt(3);
        ObjectInstance alien = getSniper(pNum, aliens, ship);
        if (strategy == 0) {
            alienShoots(d, state, pNum, alien);
            return true;
        } else {
            alien = randomAlienShoots(pNum, alien, aliens);
            if (alien == null) return false;
            alienShoots(d, state, pNum, alien);
            return true;
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
    public static ObjectInstance getSniper(int pNum, List<ObjectInstance> aliens, ObjectInstance ship) {
        int maxDistX = 1000;
        int maxDistY = 1000;
        Location shipLoc = Location.getObjectLocation(ship);
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

    /**
     * Determines whether a new alien row should spawn for player pNum
     * @param pNum the player who OWNS the aliens (i.e. they fire at the other player)
     * @return true if a new row of aliens should be created
     */
    private static boolean shouldAliensSpawn(int pNum, List<ObjectInstance> aliens) {
     int dy = pNum == 0? 1 : -1;
     int spawnRow = MAP_HEIGHT/2 + dy;
     return areRowsClear(aliens, spawnRow, spawnRow + dy);
    }

    private static boolean areRowsClear(List<ObjectInstance> aliens, int... rows) {
        for (ObjectInstance alien : aliens) {
            int y = alien.getIntValForAttribute(Y);
            for (int row : rows) {
                if (y == row) return false;
            }
        }
        return true;
    }

    /**
     * Spawna new wave of aliens (if required) and move them. Aliens are owned by player pNum
     * @param d the domain
     * @param state the current state
     * @param pNum the player that owns the aliens
     * @param ship the player's ship object
     */
    private static void spawnAliensIfRequiredAndMove(Domain d, State state, int pNum, ObjectInstance ship) {
        List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
        if (shouldAliensSpawn(pNum, aliens)) {
            spawnAlienRow(d, state, ship, pNum);
            aliens = state.getObjectsOfClass(ALIEN_CLASS);
        }
        moveAliens(state, pNum, aliens, ship);
    }

    /**
     * Moves alien waves for aliens belonging to pNum. Also mutates ship if aliens shift direction
     */
    public static void moveAliens(State state, int pNum, List<ObjectInstance> aliens, ObjectInstance ship) {
        int[] span = getAlienSpan(aliens, pNum);
        int dx = ship.getIntValForAttribute(DELTA_X);
        boolean mustAdvance = (dx > 0)? span[1] == MAP_WIDTH-1 : span[0] == 0;
        if (mustAdvance) {
            int dy = pNum == 0? 1 : -1;
            shiftAliens(state, aliens, 0, dy, pNum);
            ship.setValue(DELTA_X, -dx);
        } else {
            shiftAliens(state, aliens, dx, 0, pNum);
        }
    }

    private static void shiftAliens(State s, List<ObjectInstance> aliens, int dx, int dy, int pNum) {
        for (ObjectInstance alien : aliens) {
            if (alien.getIntValForAttribute(PNUM) == pNum) {
                Location loc = Location.getObjectLocation(alien);
                setPosition(alien, new Location(loc.x + dx, loc.y + dy));
                //Check if the game is over
                if (pNum == 0 && loc.y + dy >= MetaData.MAP_HEIGHT) killPlayer(1, s);
                if (pNum == 1 && loc.y + dy < 0) killPlayer(0, s);
            }
        }
    }

    private static void killPlayer(int pNum, State state) {
        ObjectInstance ship = state.getObject(SHIP_CLASS + pNum);
        ship.setValue(LIVES, -1);
    }

    /**
     * Spawn a new wave of aliens for the current player
     */
    private static void spawnAlienRow(Domain d, State state, ObjectInstance ship, int pNum) {
        int dx = ship.getIntValForAttribute(DELTA_X);
        List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
        //Determine wave size. It's is one bigger if opponent has an alien factory
        int waveSize = state.getFirstObjectOfClass(META_CLASS).getIntValForAttribute(ALIEN_WAVE_SIZE);
        if (ship.getIntValForAttribute(ALIEN_FACTORY) >= 0) waveSize++;
        int waveCount = getAlienWaveCount(aliens, pNum);
        int x0;
        if (waveCount == 0) {
            x0 = dx==1? 0 : MAP_HEIGHT - 1;
        } else {
            int[] span = getAlienSpan(aliens, pNum);
            x0 = dx==1? span[0] : span[1];
        }
        for (int i=0; i<waveSize; i++) {
            addAlien(d, state, pNum, x0 + i*3*dx);
        }
    }

    private static int[] getAlienSpan(List<ObjectInstance> aliens, int pNum) {
        int[] result = new int[] {MAP_HEIGHT, -1};
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            if (ap == pNum) {
                int x = alien.getIntValForAttribute(X);
                if (x < result[0]) result[0] = x;
                if (x > result[1]) result[1] = x;
            }
        }
        return result;
    }

    /**
     * Count the number of alien waves belonging to player pNum
     */
    private static int getAlienWaveCount(List<ObjectInstance> aliens, int pNum) {
        HashSet<Integer> waves = new HashSet<>(5);
        for (ObjectInstance alien : aliens) {
            if (alien.getIntValForAttribute(PNUM) == pNum) {
                waves.add(alien.getIntValForAttribute(Y));
            }
        }
        return waves.size();
    }

    private static ObjectInstance addAlien(Domain d, State s, int playerNum, int position) {
        ObjectInstance alien =createNewObject(d, s, ALIEN_CLASS);
        int y = MetaData.MAP_HEIGHT/2 + (playerNum == 0? 1 : -1);
        alien.setValue(X, position);
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
                ObjectInstance obj = getObjectAt(state, loc.x, loc.y);
                if (obj == null || !obj.getObjectClass().name.equals(SHIELD_CLASS) ) {
                    //If there was nothing in this space, build shield
                    //If there is, it should be picked up at the next collision detection and the appropriate action should occur
                    buildShield(d, state, loc);
                }
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
     * @param loc - expecting absolute (global) location coords
     */
    private static void setPosition(ObjectInstance ob, Location loc) {
        ob.setValue(X, loc.x);
        ob.setValue(Y, loc.y);
    }

    //Creates a new object with a guaranteed unique name
    public static ObjectInstance createNewObject(Domain domain, State state, String objectClass) {
        int counter = state.getObjectsOfClass(objectClass).size();
        while (state.getObject(objectClass+counter) != null) counter++;
        return new ObjectInstance(domain.getObjectClass(objectClass), objectClass+counter);
    }



    private static int incrAttribute(ObjectInstance ob, String attr) {
        int val = ob.getIntValForAttribute(attr);
        ob.setValue(attr, val+1);
        return val+1;
    }

    private static void decAttribute(ObjectInstance ob, String attr) {
        int val = ob.getIntValForAttribute(attr);
        ob.setValue(attr, val - 1);
    }

    private static void updateMeta(State state) {
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        incrAttribute(meta, ROUND_NUM);
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
        incrAttribute(ship, ALIEN_SHOT_ENERGY);
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
                //if (i == 0 && j == 0 )
                //    ob = state.getObject(collision.params[1]);
                //else ob = getObjectAt(state, loc.x + i, loc.y + j);
                ob = getObjectAt(state, loc.x + i, loc.y + j);
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
        if (mc == null) return;
        int pNum = mc.getIntValForAttribute(PNUM);
        ships[pNum].setValue(buildingAttr, -2);
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
