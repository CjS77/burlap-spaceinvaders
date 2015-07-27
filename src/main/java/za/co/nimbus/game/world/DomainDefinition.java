package za.co.nimbus.game.world;

import burlap.oomdp.core.*;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.rules.Collision;
import za.co.nimbus.game.rules.OffMap;
import za.co.nimbus.game.rules.PlayerDead;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * Static methods that define common features of both the SGDomain and SADomain of the Space invader domains
 */
public class DomainDefinition {
    private static int AlienCounter = 0;
    private static int ShieldCounter = 0;

    public static void initAttributes(Domain domain, Map<String, Attribute> attributeMap) {
        attributeMap.clear();
        attributeMap.put(ROUND_NUM, new Attribute(domain, ROUND_NUM, Attribute.AttributeType.INT));
        attributeMap.put(X, new Attribute(domain, X, Attribute.AttributeType.INT));
        attributeMap.put(Y, new Attribute(domain, Y, Attribute.AttributeType.INT));
        attributeMap.put(WIDTH, new Attribute(domain, WIDTH, Attribute.AttributeType.INT));
        attributeMap.put(ACTUAL_PNUM, new Attribute(domain, ACTUAL_PNUM, Attribute.AttributeType.INT));
        //Ship (Player) attributes
        attributeMap.put(PNUM, new Attribute(domain, PNUM, Attribute.AttributeType.INT));
        attributeMap.put(MISSILE_CONTROL, new Attribute(domain, MISSILE_CONTROL, Attribute.AttributeType.INT));
        attributeMap.put(ALIEN_FACTORY, new Attribute(domain, ALIEN_FACTORY, Attribute.AttributeType.INT));
        attributeMap.put(MISSILE_COUNT, new Attribute(domain, MISSILE_COUNT, Attribute.AttributeType.INT));
        attributeMap.put(KILLS, new Attribute(domain, KILLS, Attribute.AttributeType.INT));
        attributeMap.put(LIVES, new Attribute(domain, LIVES, Attribute.AttributeType.INT));
        attributeMap.put(RESPAWN_TIME, new Attribute(domain, RESPAWN_TIME, Attribute.AttributeType.INT));
        attributeMap.put(ALIEN_WAVE_SIZE, new Attribute(domain, ALIEN_WAVE_SIZE, Attribute.AttributeType.INT));
        attributeMap.put(ALIEN_SHOT_ENERGY, new Attribute(domain, ALIEN_SHOT_ENERGY, Attribute.AttributeType.INT));
        //Set Bounds for Attributes
        setAttributeBounds(attributeMap, ROUND_NUM, 0, MetaData.ROUND_LIMIT, true);
        setAttributeBounds(attributeMap, ACTUAL_PNUM, 0, 1);
        setAttributeBounds(attributeMap, X, -3, MetaData.MAP_WIDTH - 1);
        setAttributeBounds(attributeMap, Y, -1, MetaData.MAP_HEIGHT);
        setAttributeBounds(attributeMap, WIDTH, 0, 3, true);
        setAttributeBounds(attributeMap, PNUM, 0, 1, true);
        setAttributeBounds(attributeMap, MISSILE_CONTROL, -1, MetaData.MAP_WIDTH);
        setAttributeBounds(attributeMap, ALIEN_FACTORY, -1, MetaData.MAP_WIDTH);
        setAttributeBounds(attributeMap, MISSILE_COUNT, 0, MetaData.MAX_MISSILES);
        setAttributeBounds(attributeMap, KILLS, 0, 50);
        setAttributeBounds(attributeMap, LIVES, -1, 3);
        setAttributeBounds(attributeMap, RESPAWN_TIME, -1, 3);
        setAttributeBounds(attributeMap, ALIEN_WAVE_SIZE, 3, 6);
        setAttributeBounds(attributeMap, ALIEN_SHOT_ENERGY, 0, MetaData.SHOT_COST);
    }

    private static void setAttributeBounds(Map<String, Attribute> attributeMap, String att, int low, int high) {
        setAttributeBounds(attributeMap, att, low, high, false);
    }

    private static void setAttributeBounds(Map<String, Attribute> attributeMap, String att, int low, int high, boolean hidden) {
        Attribute attr = attributeMap.get(att);
        attr.setDiscValuesForRange(low, high, 1);
        attr.hidden = hidden;
    }

    public static void initClass(Domain domain, String className, Map<String, List<Attribute>> attributesToHash,
                                 String[] attributes, boolean[] hashed, Map<String, Attribute> attributeMap) {
        ObjectClass newClass = new ObjectClass(domain, className);
        int numAttributes = attributes.length;
        List<Attribute> attList = new ArrayList<>();
        attributesToHash.put(className, attList);
        for (int i=0; i<numAttributes; i++) {
            Attribute attr = attributeMap.get(attributes[i]);
            newClass.addAttribute(attr);
            if (hashed[i]) attList.add(attr);
        }
    }

    public static void defineInteractions(Domain domain) {
        //Set up missile collisions
        new Collision(domain, new String[]{MISSILE_CLASS, ALIEN_CLASS });
        new Collision(domain, new String[]{MISSILE_CLASS, SHIELD_CLASS });
        new Collision(domain, new String[]{MISSILE_CLASS, ALIEN_FACTORY_CLASS});
        new Collision(domain, new String[]{MISSILE_CLASS, MISSILE_CONTROLLER_CLASS});
        new Collision(domain, new String[]{MISSILE_CLASS, SHIP_CLASS});
        new Collision(domain, new String[]{MISSILE_CLASS, MISSILE_CLASS});
        new Collision(domain, new String[]{MISSILE_CLASS, BULLET_CLASS});
        //Set up bullet collisions
        // Bullets *shouldn't* be able to hit aliens or other bullets
        // Bullet - missile already taken care of above
        new Collision(domain, new String[]{BULLET_CLASS, SHIELD_CLASS });
        new Collision(domain, new String[]{BULLET_CLASS, ALIEN_FACTORY_CLASS});
        new Collision(domain, new String[]{BULLET_CLASS, MISSILE_CONTROLLER_CLASS});
        new Collision(domain, new String[]{BULLET_CLASS, SHIP_CLASS});
        //Set up Alien collisions
        new Collision(domain, new String[]{ALIEN_CLASS, SHIELD_CLASS });
        new Collision(domain, new String[]{ALIEN_CLASS, ALIEN_FACTORY_CLASS});
        new Collision(domain, new String[]{ALIEN_CLASS, MISSILE_CONTROLLER_CLASS});
        new Collision(domain, new String[]{ALIEN_CLASS, SHIP_CLASS});
        //Add Alien against wall checks for direction change
        //new AgainstWall(domain);
        //Check for tings off the map
        new OffMap(domain, ALIEN_CLASS);
        new OffMap(domain, BULLET_CLASS);
        new OffMap(domain, MISSILE_CLASS);
        //Player is dead
        new PlayerDead(domain);
    }

    public static State getInitialState(Domain d, int actualPNum) {
        State s = new State();
        addMeta(d, s, 1, 3, 0, actualPNum);
        addShip(d, s, 0, MetaData.MAP_WIDTH / 2, -1, -1, 0, 0, 2, -1);
        addShip(d, s, 1, MetaData.MAP_WIDTH / 2, -1, -1, 0, 0, 2, -1);
        addAlienWave(d, s, 0, 3, actualPNum);
        addAlienWave(d, s, 1, 3, actualPNum);
        addInitialShields(d, s);
        return s;
    }

    private static ObjectInstance addMeta(Domain d, State s, int roundNum, int waveSize, int alienShotEnergy, int actualPNum) {
        ObjectInstance meta = new ObjectInstance(d.getObjectClass(META_CLASS), "MetaData");
        meta.setValue(ROUND_NUM, roundNum);
        meta.setValue(ALIEN_WAVE_SIZE, waveSize);
        meta.setValue(ALIEN_SHOT_ENERGY, alienShotEnergy);
        meta.setValue(ACTUAL_PNUM, actualPNum);
        s.addObject(meta);
        return meta;
    }

    private static void addInitialShields(Domain d, State s) {
        int posX[] = new int[] {1, MetaData.MAP_WIDTH - 4};
        int posY[] = new int[] {2, MetaData.MAP_HEIGHT - 5};
        for (int startX : posX) {
            for (int startY : posY) {
                for (int i = 0; i<3; i++) {
                    for (int j=0; j<3; j++) {
                        addShield(d, s, startX+i, startY+j);
                    }
                }
            }
        }
    }

    /**
     * @param x - the x-pos of the middle of the ship
     */
    public static ObjectInstance addShip(Domain d, State s, int playerNum, int x, int missileController, int alienFactory, int missileCount,
                                         int kills, int lives, int respawn_time) {
        ObjectInstance ship = new ObjectInstance(d.getObjectClass(SHIP_CLASS), SHIP_CLASS + playerNum);
        ship.setValue(X, x);
        ship.setValue(Y, playerNum==0? 1 : MetaData.MAP_HEIGHT-2);
        ship.setValue(WIDTH, 3);
        ship.setValue(PNUM, playerNum);
        ship.setValue(MISSILE_CONTROL, missileController);
        ship.setValue(ALIEN_FACTORY, alienFactory);
        ship.setValue(MISSILE_COUNT, missileCount);
        ship.setValue(KILLS, kills);
        ship.setValue(LIVES, lives);
        ship.setValue(RESPAWN_TIME, respawn_time);
        s.addObject(ship);
        return ship;
    }


    private static ObjectInstance addShield(Domain d, State s, int x, int y) {
        ObjectInstance shield = new ObjectInstance(d.getObjectClass(SHIELD_CLASS), SHIELD_CLASS + ShieldCounter++);
        shield.setValue(X, x);
        shield.setValue(Y, y);
        shield.setValue(WIDTH, 1);
        s.addObject(shield);
        return shield;
    }

    private static void addAlienWave(Domain d, State s, int playerNum, int waveSize, int actualPnum) {
        int pos;
        for (int i=0; i<waveSize; i++) {
            pos = actualPnum == 0? MetaData.MAP_WIDTH - 1 - 3*i : 3*i;
            addAlien(d, s, playerNum, pos);
        }
    }

    private static ObjectInstance addAlien(Domain d, State s, int playerNum, int position) {
        ObjectInstance alien = new ObjectInstance(d.getObjectClass(ALIEN_CLASS), ALIEN_CLASS + AlienCounter++);
        int y = MetaData.MAP_HEIGHT/2 + (playerNum == 0? 1 : -1);
        alien.setValue(X, position);
        alien.setValue(Y, y);
        alien.setValue(WIDTH, 1);
        alien.setValue(PNUM, playerNum);
        s.addObject(alien);
        return alien;
    }
}
