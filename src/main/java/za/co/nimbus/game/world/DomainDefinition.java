package za.co.nimbus.game.world;

import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.rules.Collision;
import za.co.nimbus.game.rules.OffMap;
import za.co.nimbus.game.rules.PlayerDead;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Attributes.P2_LIVES;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * Static methods that define common features of both the SGDomain and SADomain of the Space invader domains
 */
public class DomainDefinition {
    public static void initAttributes(Domain domain, Map<String, Attribute> attributeMap) {
        attributeMap.clear();
        attributeMap.put(ROUND_NUM, new Attribute(domain, ROUND_NUM, Attribute.AttributeType.INT));
        attributeMap.put(X, new Attribute(domain, X, Attribute.AttributeType.INT));
        attributeMap.put(Y, new Attribute(domain, Y, Attribute.AttributeType.INT));
        attributeMap.put(WIDTH, new Attribute(domain, WIDTH, Attribute.AttributeType.INT));
        //Ship (Player) attributes
        attributeMap.put(PNUM, new Attribute(domain, PNUM, Attribute.AttributeType.INT));
        attributeMap.put(MISSILE_CONTROL, new Attribute(domain, MISSILE_CONTROL, Attribute.AttributeType.INT));
        attributeMap.put(ALIEN_FACTORY, new Attribute(domain, ALIEN_FACTORY, Attribute.AttributeType.INT));
        attributeMap.put(MISSILE_COUNT, new Attribute(domain, MISSILE_COUNT, Attribute.AttributeType.INT));
        attributeMap.put(KILLS, new Attribute(domain, KILLS, Attribute.AttributeType.INT));
        attributeMap.put(LIVES, new Attribute(domain, LIVES, Attribute.AttributeType.INT));
        attributeMap.put(RESPAWN_TIME, new Attribute(domain, RESPAWN_TIME, Attribute.AttributeType.INT));
        //attributeMap.put(MISSILE_LIMIT, new Attribute(domain, MISSILE_LIMIT, Attribute.AttributeType.INT));
        attributeMap.put(ALIEN_WAVE_SIZE, new Attribute(domain, ALIEN_WAVE_SIZE, Attribute.AttributeType.INT));
        attributeMap.put(ALIEN_SHOT_ENERGY, new Attribute(domain, ALIEN_SHOT_ENERGY, Attribute.AttributeType.INT));
        // Simplified State Classes
        attributeMap.put(IS_BEHIND_SHIELDS, new Attribute(domain, IS_BEHIND_SHIELDS, Attribute.AttributeType.BOOLEAN));
        attributeMap.put(DANGER_LEVEL, new Attribute(domain, DANGER_LEVEL, Attribute.AttributeType.INT));
        attributeMap.put(WILL_KILL_IF_FIRE, new Attribute(domain, WILL_KILL_IF_FIRE, Attribute.AttributeType.INT));
        attributeMap.put(AT_LEFT_WALL, new Attribute(domain, AT_LEFT_WALL, Attribute.AttributeType.BOOLEAN));
        attributeMap.put(AT_RIGHT_WALL, new Attribute(domain, AT_RIGHT_WALL, Attribute.AttributeType.BOOLEAN));
        attributeMap.put(CAN_SHOOT, new Attribute(domain, CAN_SHOOT, Attribute.AttributeType.BOOLEAN));
        attributeMap.put(P1_LIVES, new Attribute(domain, P1_LIVES, Attribute.AttributeType.INT));
        attributeMap.put(P2_LIVES, new Attribute(domain, P2_LIVES, Attribute.AttributeType.INT));
        //Set Bounds for Attributes
        setAttributeBounds(attributeMap, ROUND_NUM, 0, MetaData.ROUND_LIMIT, true);
        setAttributeBounds(attributeMap, X, 0, MetaData.MAP_WIDTH - 1);
        setAttributeBounds(attributeMap, Y, 0, MetaData.MAP_HEIGHT - 1);
        setAttributeBounds(attributeMap, WIDTH, 0, 3, true);
        setAttributeBounds(attributeMap, PNUM, 0, 1, true);
        setAttributeBounds(attributeMap, MISSILE_CONTROL, -1, MetaData.MAP_WIDTH);
        setAttributeBounds(attributeMap, ALIEN_FACTORY, -1, MetaData.MAP_WIDTH);
        setAttributeBounds(attributeMap, MISSILE_COUNT, 0, MetaData.MAX_MISSILES);
        setAttributeBounds(attributeMap, KILLS, 0, 200);
        setAttributeBounds(attributeMap, LIVES, -1, 3);
        setAttributeBounds(attributeMap, RESPAWN_TIME, -1, 3);
        setAttributeBounds(attributeMap, ALIEN_WAVE_SIZE, 3, 6);
        setAttributeBounds(attributeMap, ALIEN_SHOT_ENERGY, 0, MetaData.SHOT_COST);
        setAttributeBounds(attributeMap, DANGER_LEVEL, 0, MetaData.MAP_HEIGHT/2);
        setAttributeBounds(attributeMap, WILL_KILL_IF_FIRE, -1, MetaData.MAP_HEIGHT/2);
        setAttributeBounds(attributeMap, P1_LIVES, -1, 3);
        setAttributeBounds(attributeMap, P2_LIVES, -1, 3);
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
}
