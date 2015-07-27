package za.co.nimbus.game.rules;

import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.*;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
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
public class SpaceInvaderJAM extends JointActionModel {
    private final Random rnd;
    private final Domain domain;
    private static final int debugcode = 18067701;

    public SpaceInvaderJAM(Domain d, Integer randomSeed) {
        rnd = randomSeed == null? RandomFactory.getDefault() : RandomFactory.getMapped(randomSeed);
        domain = d;
    }

    @Override
    public List<TransitionProbability> transitionProbsFor(State state, JointAction ja) {
        List <TransitionProbability> tps = new ArrayList<TransitionProbability>();
        State newState = actionHelper(state, ja);
        tps.add(new TransitionProbability(newState, 1.0));
        return tps;
    }

    @Override
    protected State actionHelper(State state, JointAction jointAction) {
        ObjectInstance[] ships = new ObjectInstance[2];
        Set<ObjectInstance> deadEntities = new HashSet<>();
        ships[0] = state.getObject(SHIP_CLASS + "0");
        ships[1] = state.getObject(SHIP_CLASS + "1");
        moveProjectiles(state, MISSILE_CLASS);
        handleCollisionsAndRemoveDeadEntities(state, ships, deadEntities);
        moveProjectiles(state, BULLET_CLASS);
        handleCollisionsAndRemoveDeadEntities(state, ships, deadEntities);
        spawnAliensIfRequiredAndMove(state, ships);
        handleCollisionsAndRemoveDeadEntities(state, ships, deadEntities);
        aliensShootIfPossible(state, ships);
        handleCollisionsAndRemoveDeadEntities(state, ships, deadEntities);
        removeItemsThatMovedOffMap(state);
        //Process Ship command
        for (GroundedSingleAction action: jointAction) {
            String agent = action.actingAgent;
            String command = action.actionName();
            handleShipCommand(agent, command, state);
        }
        handleCollisionsAndRemoveDeadEntities(state, ships, deadEntities);
        //Update general game state
        updateShipStats(ships[0]);
        updateShipStats(ships[1]);
        updateMeta(state);
        for (ObjectInstance deadEntity : deadEntities) {
            state.removeObject(deadEntity);
        }
        return state;
    }

    private void aliensShootIfPossible(State state, ObjectInstance[] ships) {
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        int shotEnergy =  meta.getIntValForAttribute(ALIEN_SHOT_ENERGY);
        if (shotEnergy >= MetaData.SHOT_COST) {
            meta.setValue(ALIEN_SHOT_ENERGY, shotEnergy - MetaData.SHOT_COST);
            List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
            playersAliensFire(state, 0, aliens, ships);
            playersAliensFire(state, 1, aliens, ships);
        }
    }

    private void playersAliensFire(State state, int pnum, List<ObjectInstance> aliens, ObjectInstance[] ships) {
        int strategy = rnd.nextInt(3);
        ObjectInstance alien = getSniper(pnum, aliens, ships);
        if (strategy == 0) {
            alienShoots(state, pnum, alien);
        } else {
            alien = randomAlienShoots(pnum, alien, aliens);
            alienShoots(state, pnum, alien);
        }
    }

    /**
     * Find random alien to shoot AT pnum
     */
    private ObjectInstance randomAlienShoots(int pnum, ObjectInstance excluded, List<ObjectInstance> aliens) {
        int[] wy = getLastTwoWaveCoords(aliens, pnum);
        List<ObjectInstance> eligible = new ArrayList<>();
        for (ObjectInstance alien : aliens) {
            Location loc = Location.getObjectLocation(alien);
            if (alien != excluded && (loc.y == wy[0] || loc.y == wy[1]) ) eligible.add(alien);
        }
        int pool = eligible.size();
        if (pool > 0) {
            int alien = rnd.nextInt(pool);
            return eligible.get(alien);
        } else return null;
    }

    /**
     * Find y-coords of closest two ENEMY alien waves
     */
    private int[] getLastTwoWaveCoords(List<ObjectInstance> aliens, int pnum) {
        int[] result = pnum == 0 ? new int[]{1000, 1000} : new int[]{-1, -1};
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            if (ap != pnum) {
                Location loc = Location.getObjectLocation(alien);
                if (pnum==1) {
                    if (loc.y > result[0]) {
                        result[1] = result[0];
                        result[0] = loc.y;
                    } else if (loc.y < result[0] && loc.y > result[1]) {
                        result[1] = loc.y;
                    }
                }
                if (pnum==0) {
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
     * Assign an alien to shoot AT pnum
     */
    private void alienShoots(State state, int pnum, ObjectInstance alien) {
        if (alien != null) {
            ObjectInstance bullet = createNewObject(state, BULLET_CLASS);
            int ydir = pnum==0? -1: 1;
            Location aloc = Location.getObjectLocation(alien);
            Location bloc = new Location(aloc.x, aloc.y + ydir);
            setPosition(bullet, bloc);
            bullet.setValue(WIDTH, 1);
            bullet.setValue(PNUM, 1-pnum);
            state.addObject(bullet);
        }
    }

    /**
     * Find closest ENEMY alien to shoot AT pnum
     */
    private ObjectInstance getSniper(int pnum, List<ObjectInstance> aliens, ObjectInstance[] ships) {
        int maxDistX = 1000;
        int maxDistY = 1000;
        Location shipLoc = Location.getObjectLocation(ships[pnum]);
        ObjectInstance closestAlien = null;
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            //The other player's aliens are shooting at me
            if (ap == 1-pnum) {
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

    private void spawnAliensIfRequiredAndMove(State state, ObjectInstance[] ships) {
        List<ObjectInstance> aliens = state.getObjectsOfClass(ALIEN_CLASS);
        int[][] onWall = getAlienPosition(aliens);
        int roundNum = state.getFirstObjectOfClass(META_CLASS).getIntValForAttribute(ROUND_NUM);
        if (roundNum > 1) {
             if (onWall[0][0] > 0 && (onWall[0][1] > MetaData.MAP_HEIGHT/2 + 2)) spawnAlienRow(state, ships, 0);
             if (onWall[1][0] > 0 && (onWall[1][1] < MetaData.MAP_HEIGHT/2 - 2)) spawnAlienRow(state, ships, 1);
        }
        aliens = state.getObjectsOfClass(ALIEN_CLASS);
        for (ObjectInstance alien : aliens) {
            Location loc = Location.getObjectLocation(alien);
            int pnum = alien.getIntValForAttribute(PNUM);
            setPosition(alien, nextAlienLocation(loc, onWall[pnum][0]));
        }

    }

    private void spawnAlienRow(State state, ObjectInstance[] ships, int pnum) {
        int waveSize = state.getFirstObjectOfClass(META_CLASS).getIntValForAttribute(ALIEN_WAVE_SIZE);
        //Wave size is one bigger if opponent has an alien factory
        if (ships[pnum].getIntValForAttribute(ALIEN_FACTORY) >= 0) waveSize++;
        int pos;
        for (int i=0; i<waveSize; i++) {
            pos = i * 3;
            addAlien(state, pnum, pos);
        }
    }

    private ObjectInstance addAlien(State s, int playerNum, int position) {
        ObjectInstance alien =createNewObject(s, ALIEN_CLASS);
        int y = MetaData.MAP_HEIGHT/2 + (playerNum == 0? 1 : -1);
        alien.setValue(X, MetaData.MAP_WIDTH - 1 - position);
        alien.setValue(Y, y);
        alien.setValue(WIDTH, 1);
        alien.setValue(PNUM, playerNum);
        s.addObject(alien);
        return alien;
    }

    private void removeItemsThatMovedOffMap(State state) {
        Set<PropositionalFunction> offMapFns = domain.getPropositionlFunctionsMap().get(OffMap.NAME);
        for (PropositionalFunction pf : offMapFns) {
            List<GroundedProp> pfs = pf.getAllGroundedPropsForState(state);
            for (GroundedProp offMap : pfs) {
                if (offMap.isTrue(state)) {
                    state.removeObject(offMap.params[0]);
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
            int pnum = alien.getIntValForAttribute(PNUM);
            Location loc = Location.getObjectLocation(alien);
            if (loc.x == 0 || loc.x == MetaData.MAP_WIDTH-1) {
                onWall[pnum][0] = loc.x;
            }
            if (pnum == 0 && loc.y < onWall[0][1]) onWall[0][1] = loc.y;
            if (pnum == 1 && loc.y > onWall[1][1]) onWall[1][1] = loc.y;
        }
        return onWall;
    }

    /**
     * Calculates next position for an alient at cur.
     * @param cur - cur pos
     * @param atWall - 0, MAP_WIDTH-1, or -1 for left, right, or none
     * @return - next pos
     */
    public static Location nextAlienLocation(Location cur, int atWall) {
        int xdir = (cur.y % 2 == 0)? -1 : 1;
        int ydir = (cur.y > MetaData.MAP_HEIGHT/2)? 1 : -1;
        //Must move down
        if ((atWall == 0 && xdir == -1) || (atWall > 0 && xdir == 1)) return new Location(cur.x, cur.y + ydir);
        //normal move
        return new Location(cur.x + xdir, cur.y);
    }

    private void moveProjectiles(State state, String objectClass) {
        List<ObjectInstance> missiles = state.getObjectsOfClass(objectClass);
        for (ObjectInstance missile : missiles) {
            int y = missile.getIntValForAttribute(Y);
            int pnum = missile.getIntValForAttribute(PNUM);
            missile.setValue(Y, pnum == 0? y+1 : y-1);
        }
    }

    private void handleShipCommand(String agent, String command, State state) {
        ObjectInstance ship = state.getObject(agent);
        Location shipLoc = Location.getObjectLocation(ship);
        int pnum = ship.getIntValForAttribute(PNUM);
        switch (command) {
            case MoveLeft:
                if (pnum == 0) decAttribute(ship, X);
                else incrAttribute(ship, X);
                break;
            case MoveRight:
                if (pnum == 0) incrAttribute(ship, X);
                else decAttribute(ship, X);
                break;
            case Shoot:
                buildMissile(state, ship, shipLoc, pnum);
                break;
            case BuildAlienFactory:
                buildBuilding(ALIEN_FACTORY_CLASS, ALIEN_FACTORY, state, ship, shipLoc, pnum);
                break;
            case BuildMissileController:
                buildBuilding(MISSILE_CONTROLLER_CLASS, MISSILE_CONTROL, state, ship, shipLoc, pnum);
                break;
            case BuildShield:
                buildShields(state, ship, shipLoc, pnum);
            case Nothing:
            default:
        }
    }

    private void buildShields(State state, ObjectInstance ship, Location shipLoc, int pnum) {
        int dir = pnum==0? 1 : -1;
        for (int i=-1; i<2; i++) {
            for (int j = 1; j <= 3; j++) {
                Location loc = new Location(shipLoc.x + i, shipLoc.y + dir * j);
                if (getObjectAt(state, loc.x, loc.y) == null) buildShield(state, loc);
            }
        }
        decAttribute(ship, LIVES);
    }

    private String buildShield(State state, Location loc) {
        ObjectInstance shield = createNewObject(state, SHIELD_CLASS);
        setPosition(shield, loc);
        shield.setValue(WIDTH, 1);
        state.addObject(shield);
        return shield.getName();
    }

    private void buildMissile(State state, ObjectInstance ship, Location loc, int pnum) {
        ObjectInstance missile = createNewObject(state, MISSILE_CLASS);
        Location objLoc = new Location(loc.x, pnum==0? loc.y + 1 : loc.y -1 );
        setPosition(missile, objLoc);
        missile.setValue(PNUM, pnum);
        missile.setValue(WIDTH, 1);
        state.addObject(missile);
        //Update Ship records
        incrAttribute(ship, MISSILE_COUNT);
    }

    private void buildBuilding(String objectClass, String playerAttr, State state, ObjectInstance ship, Location loc, int pnum) {
        ObjectInstance building = createNewObject(state, objectClass);
        Location objLoc = new Location(loc.x, pnum==0? loc.y - 1 : loc.y  + 1 );
        setPosition(building, objLoc);
        building.setValue(PNUM, pnum);
        building.setValue(WIDTH, 3);
        state.addObject(building);
        //Update Ship records
        ship.setValue(playerAttr, loc.x);
        decAttribute(ship, LIVES);
    }

    /**
     * @param loc - expecting absolute (global) location corrids
     */
    private void setPosition(ObjectInstance ob, Location loc) {
        ob.setValue(X, loc.x);
        ob.setValue(Y, loc.y);
    }

    //Creates a new object with a guaranteed unique name
    private ObjectInstance createNewObject(State state, String objectClass) {
        int counter = state.getObjectsOfClass(objectClass).size();
        while (state.getObject(objectClass+counter) != null) counter++;
        return new ObjectInstance(domain.getObjectClass(objectClass), objectClass+counter);
    }



    private void incrAttribute(ObjectInstance ob, String attr) {
        int val = ob.getIntValForAttribute(attr);
        ob.setValue(attr, val+1);
    }

    private void decAttribute(ObjectInstance ob, String attr) {
        int val = ob.getIntValForAttribute(attr);
        ob.setValue(attr, val - 1);
    }

    private void updateMeta(State state) {
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        incrAttribute(meta, ROUND_NUM);
        incrAttribute(meta, ALIEN_SHOT_ENERGY);
        int roundNum = meta.getIntValForAttribute(ROUND_NUM);
        if (roundNum == MetaData.WAVE_BUMP_ROUND) incrAttribute(meta, ALIEN_WAVE_SIZE);
    }

    private void updateShipStats(ObjectInstance ship) {
        int respawn_timer = ship.getIntValForAttribute(RESPAWN_TIME);
        if (respawn_timer > 0) {
            ship.setValue(RESPAWN_TIME, respawn_timer-1);
        }
        if (respawn_timer == 0) {
            ship.setValue(RESPAWN_TIME, -1);
            ship.setValue(X, MetaData.MAP_WIDTH/2);
        }
    }

    private void handleCollisionsAndRemoveDeadEntities(State state, ObjectInstance[] ships, Set<ObjectInstance> deadEntities) {
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

    private void handleCollision(State state, ObjectInstance[] ships, String collisionClass, GroundedProp collision, Set<ObjectInstance> deadEntities) {
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
                recordMissileLoss(state.getObject(collision.params[0]), ships);
                recordMissileLoss(state.getObject(collision.params[1]), ships);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+SHIELD_CLASS+Collision.NAME:
            case MISSILE_CLASS+BULLET_CLASS+Collision.NAME:
                ObjectInstance m = state.getObject(collision.params[0]);
                recordMissileLoss(m, ships);
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case BULLET_CLASS+SHIELD_CLASS+Collision.NAME:
                queueRemoveEntities(state, collision, deadEntities);
                break;
            case MISSILE_CLASS+SHIP_CLASS+Collision.NAME:
                int pnum = addPossibleKill(state, ships, collision);
                assert (pnum>=0);
                freezePlayer(ships[1 - pnum]);
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

    private void explodeAlien(State state, GroundedProp collision, ObjectInstance[] ships) {
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
                            recordMissileLoss(ob, ships);
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

    private ObjectInstance getObjectAt(State state, int x, int y) {
        for (ObjectInstance o : state.getAllObjects()) {
            if (!o.getObjectClass().hasAttribute(X)) continue;
            int ox = o.getIntValForAttribute(X);
            int ow = o.getIntValForAttribute(WIDTH);
            int oy = o.getIntValForAttribute(Y);
            if (oy == y && Math.abs(x - ox) < ow) return o;
        }
        return null;
    }

    private void freezePlayer(ObjectInstance ship) {
        if (isFrozen(ship)) {
            DPrint.cl(debugcode, "Hmm. Trying to freeze an already frozen ship. This should not happen");
        }
        //Move off the map
        ship.setValue(X, -3);
        //Reduce lives by 1
        ship.setValue(LIVES, ship.getIntValForAttribute(LIVES)-1);
        //Set respawn timer
        ship.setValue(RESPAWN_TIME, 3);
    }

    private void queueRemoveEntities(State state, GroundedProp collision, Set<ObjectInstance> deadEntities) {
        deadEntities.add(state.getObject(collision.params[0]));
        deadEntities.add(state.getObject(collision.params[1]));
    }

    private void recordMissileLoss(ObjectInstance m, ObjectInstance[] ships) {
        int pnum = m.getIntValForAttribute(PNUM);
        decAttribute(ships[pnum], MISSILE_COUNT);
    }

    private void recordBuildingStatus(State state, ObjectInstance[] ships, GroundedProp collision, String buildingAttr) {
        ObjectInstance mc = state.getObject(collision.params[1]);
        int pnum = mc.getIntValForAttribute(PNUM);
        ships[pnum].setValue(buildingAttr, -1);
    }


    /**
     * Checks a missile collision for validity (right player owns the entities) and scores a kill
     * RecordMissileLoss is called
     * Return the player num for player that scored the kill or -1 if no kill
     */
    private int addPossibleKill(State state, ObjectInstance[] ships, GroundedProp collision) {
        ObjectInstance m = state.getObject(collision.params[0]);
        ObjectInstance o = state.getObject(collision.params[1]);
        int pnum0 = m.getIntValForAttribute(PNUM);
        int pnum1 = o.getIntValForAttribute(PNUM);
        recordMissileLoss(m, ships);
        //Frozen ships are immune
        if (o.getTrueClassName().equals(SHIP_CLASS) && isFrozen(o))
            throw new IllegalStateException("Somehow we hit a frozen ship");
        if (pnum0 != pnum1) {
            incrAttribute(ships[pnum0], KILLS);
            return pnum0;
        }
        return -1;
    }

    private boolean isFrozen(ObjectInstance ship) {
        int timerVal = ship.getIntValForAttribute(RESPAWN_TIME);
        return timerVal >= 0;
    }
}
