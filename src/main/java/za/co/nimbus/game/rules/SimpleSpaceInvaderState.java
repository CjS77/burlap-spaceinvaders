package za.co.nimbus.game.rules;

import burlap.oomdp.auxiliary.StateAbstraction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;

import java.util.List;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;
import static za.co.nimbus.game.constants.ObjectClasses.BULLET_CLASS;


/**
 *  A much simplified state representation of the Space invader game
 *       BehindShields (bool)
 *       DangerLevel
 *       HitIfShoot
 *       Ship.X
 *       Ship.Lives
 *       Ship.HasMissileController
 *       Ship.HasAlienFactory
 *
 */
public class SimpleSpaceInvaderState implements StateAbstraction {

    private final Domain domain;

    public SimpleSpaceInvaderState(Domain d) {
        this.domain = d;
    }

    @Override
    public State abstraction(State state) {
        ObjectInstance ship = state.getObject(SHIP_CLASS+"0");
        Location sloc = Location.getObjectLocation(ship);
        State simpleState = new State();
        ObjectInstance o = new ObjectInstance(domain.getObjectClass(SIMPLE_STATE_CLASS), "SimpleState");
        o.setValue(IS_BEHIND_SHIELDS, getIsBehindShields(state, ship));
        o.setValue(DANGER_LEVEL, getDangerLevel(state, ship));
        o.setValue(AT_LEFT_WALL, sloc.x <= 1);
        o.setValue(AT_RIGHT_WALL, sloc.x >= MetaData.MAP_WIDTH - 1);
        o.setValue(CAN_SHOOT, getCanShoot(state, ship));
        o.setValue(P1_LIVES, ship.getIntValForAttribute(LIVES));
        o.setValue(P2_LIVES, state.getObject(SHIP_CLASS+"1").getIntValForAttribute(LIVES));
        simpleState.addObject(o);
        return simpleState;
    }

    private boolean getCanShoot(State state, ObjectInstance ship) {
        boolean hasMissileController = ship.getIntValForAttribute(MISSILE_CONTROL) >= 0;
        int missileCount = ship.getIntValForAttribute(MISSILE_COUNT);
        if (missileCount > 1 || (missileCount > 0 && !hasMissileController)) return false;
        if (getIsBehindShields(state, ship)) return false;
        int hitRange = willHitAlienIfShoot(state, ship);
        return hitRange >= 0 || willHitEnemyBuildingIfShoot(state, ship);
    }

    private int getDangerLevel(State s, ObjectInstance ship) {
        Location loc = Location.getObjectLocation(ship);
        int result = checkForLineOfFire(s, loc, MISSILE_CLASS, 100);
        return checkForLineOfFire(s, loc, BULLET_CLASS, result);
    }

    private int checkForLineOfFire(State s, Location loc, String aClass, int d0) {
        int result = d0;
        List<ObjectInstance> missiles = s.getObjectsOfClass(aClass);
        for (ObjectInstance missile : missiles) {
            if (missile.getIntValForAttribute(PNUM) != 0) {
                Location mloc = Location.getObjectLocation(missile);
                if (Math.abs(mloc.x - loc.x) < 2) {
                    int dy = Math.abs(loc.y - mloc.y);
                    if (dy < result) result = dy;
                }
            }
        }
        return result;
    }

    private boolean getIsBehindShields(State s, ObjectInstance ship) {
        List<ObjectInstance> shields = s.getObjectsOfClass(SHIELD_CLASS);
        Location l = Location.getObjectLocation(ship);
        for (ObjectInstance shield : shields) {
            int sx = shield.getIntValForAttribute(X);
            if ((sx == l.x) && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) return true;
        }
        return false;
    }

    boolean willHitEnemyBuildingIfShoot(State s, ObjectInstance myShip) {
        if (getIsBehindShields(s, myShip)) return false;
        Location ship = Location.getObjectLocation(myShip);
        ObjectInstance enemy = s.getObject(SHIP_CLASS + "1");
        return Math.abs(enemy.getIntValForAttribute(MISSILE_CONTROL) - ship.x) < 2 ||
               Math.abs(enemy.getIntValForAttribute(ALIEN_FACTORY) - ship.x) < 2;
    }

    int willHitAlienIfShoot(State s, ObjectInstance myShip) {
        if (getIsBehindShields(s, myShip)) return -1;
        Location ship = Location.getObjectLocation(myShip);
        List<ObjectInstance> aliens = s.getObjectsOfClass(ALIEN_CLASS);
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            if (0 != ap) {
                Location l = Location.getObjectLocation(alien);
                int j = 10 - l.y;
                if (j > 8) j = 8;
                int sx = targetMap[0][j][l.x];
                if (sx == ship.x) return Math.abs(ship.y - l.y);
            }
        }
        return -1;
    }

    public static final int[][][] targetMap = new int[][][]{
            {   {6, 5, 4, 3, 2, 1, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8},
                {7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 15, 14, 13, 12, 11},
                {4, 3, 2, 1, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 15, 14, 13},
                {2, 1, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12},
                {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 15},
                {0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
                {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16},
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}
            },{
                {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16},
                {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,16},
                {0,0,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14},
                {3,4,5,6,7,8,9,10,11,12,13,14,15,16,16,16,15},
                {2,1,0,0,0,1,2,3,4,5,6,7,8,9,10,11,12},
                {5,6,7,8,9,10,11,12,13,14,15,16,16,16,15,14,13},
                {4,3,2,1,0,0,0,1,2,3,4,5,6,7,8,9,10},
                {7,8,9,10,11,12,13,14,15,16,16,16,15,14,13,12,11},
                {6,5,4,3,2,1,0,0,0,1,2,3,4,5,6,7,8}
    }};
}
