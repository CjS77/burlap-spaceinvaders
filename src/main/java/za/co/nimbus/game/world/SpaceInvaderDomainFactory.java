package za.co.nimbus.game.world;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.rules.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * A domain generator for the Space Invader Game
 */
public class SpaceInvaderDomainFactory implements DomainGenerator {


    private final Map<String, Attribute> attributeMap = new HashMap<>();
    public final List<SingleAction> actionList = new ArrayList<>();
    private final Map<String, List<Attribute>> mAttributesToHash = new HashMap<>();
    private static int AlienCounter = 0;
    private static int ShieldCounter = 0;
    private final Integer seed;

    public SpaceInvaderDomainFactory(Integer seed) {
        this.seed = seed;
    }

    @Override
    public Domain generateDomain() {
        SGDomain domain = new SGDomain();
        mAttributesToHash.clear();
        initAttributes(domain);
        defineClasses(domain, mAttributesToHash);
        //defineActions(domain);
        defineSimpleActions(domain);
        defineRules(domain);
        domain.setJointActionModel(new SpaceInvaderMechanics(domain, seed));
        return domain;
    }

    /**
     * Returns a map of all the state attributes that should be hashed in a learning algorithm. In general, it is a
     * subset of the full attribute list. This list will only be populated after a call to {@link #generateDomain}
     */
    public Map<String, List<Attribute>> getFullAttributesToHashMap() {
        return mAttributesToHash;
    }

    private void defineRules(SGDomain domain) {
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

    private void defineActions(SGDomain domain) {
        actionList.clear();
        actionList.add(new ShipAction(domain, MoveLeft));
        actionList.add(new ShipAction(domain, Nothing));
        actionList.add(new ShipAction(domain, MoveRight));
        actionList.add(new ShipAction(domain, Shoot));
        actionList.add(new ShipAction(domain, BuildAlienFactory));
        actionList.add(new ShipAction(domain, BuildMissileController));
        actionList.add(new ShipAction(domain, BuildShield));
    }

    private void defineSimpleActions(SGDomain domain) {
        actionList.clear();
        actionList.add(new SimpleShipAction(domain, MoveLeft));
        actionList.add(new SimpleShipAction(domain, Nothing));
        actionList.add(new SimpleShipAction(domain, MoveRight));
        actionList.add(new SimpleShipAction(domain, Shoot));
        actionList.add(new SimpleShipAction(domain, BuildAlienFactory));
        actionList.add(new SimpleShipAction(domain, BuildMissileController));
        actionList.add(new SimpleShipAction(domain, BuildShield));
    }

    private void initAttributes(Domain domain) {
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
        setAttributeBounds(ROUND_NUM, 0, MetaData.ROUND_LIMIT, true);
        setAttributeBounds(X, 0, MetaData.MAP_WIDTH - 1);
        setAttributeBounds(Y, 0, MetaData.MAP_HEIGHT - 1);
        setAttributeBounds(WIDTH, 0, 3, true);
        setAttributeBounds(PNUM, 0, 1, true);
        setAttributeBounds(MISSILE_CONTROL, -1, MetaData.MAP_WIDTH);
        setAttributeBounds(ALIEN_FACTORY, -1, MetaData.MAP_WIDTH);
        setAttributeBounds(MISSILE_COUNT, 0, MetaData.MAX_MISSILES);
        setAttributeBounds(KILLS, 0, 200);
        setAttributeBounds(LIVES, -1, 3);
        setAttributeBounds(RESPAWN_TIME, -1, 3);
        setAttributeBounds(ALIEN_WAVE_SIZE, 3, 6);
        setAttributeBounds(ALIEN_SHOT_ENERGY, 0, MetaData.SHOT_COST);
        setAttributeBounds(DANGER_LEVEL, 0, MetaData.MAP_HEIGHT/2);
        setAttributeBounds(WILL_KILL_IF_FIRE, -1, MetaData.MAP_HEIGHT/2);
        setAttributeBounds(P1_LIVES, -1, 3);
        setAttributeBounds(P2_LIVES, -1, 3);
    }

    private void setAttributeBounds(String att, int low, int high, boolean hidden) {
        Attribute attr = attributeMap.get(att);
        attr.setDiscValuesForRange(low, high, 1);
        attr.hidden = hidden;
    }

    private void setAttributeBounds(String att, int low, int high) {
        setAttributeBounds(att, low, high, false);
    }
    
    private void initClass(Domain domain, String className, Map<String, List<Attribute>> attributesToHash, String[] attributes, boolean[] hashed) {
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

    private void defineClasses(Domain domain, Map<String, List<Attribute>> attributesToHash) {
        //Meta data
        String[] atts = {ROUND_NUM, ALIEN_SHOT_ENERGY, ALIEN_WAVE_SIZE};
        boolean[] hashed = {false, false, false};
        initClass(domain, META_CLASS, attributesToHash, atts, hashed);
        //Ships
        atts = new String[]{X, Y, WIDTH, PNUM, MISSILE_CONTROL, ALIEN_FACTORY, MISSILE_COUNT, KILLS, LIVES, RESPAWN_TIME};
        hashed = new boolean[]{true, false, true, true, true, true, false, false, false, false};
        initClass(domain, SHIP_CLASS, attributesToHash, atts, hashed);
        //Missiles and Bullets and Buildings
        atts = new String[] {X, Y, WIDTH, PNUM};
        hashed = new boolean[]{true, true, false, true};
        initClass(domain, MISSILE_CLASS, attributesToHash, atts, hashed);
        initClass(domain, BULLET_CLASS, attributesToHash, atts, hashed);
        initClass(domain, ALIEN_CLASS, attributesToHash, atts, hashed);
        initClass(domain, ALIEN_FACTORY_CLASS, attributesToHash, atts, hashed);
        initClass(domain, MISSILE_CONTROLLER_CLASS, attributesToHash, atts, hashed);
        //Shields
        atts = new String[] {X, Y, WIDTH};
        hashed = new boolean[]{true, true, false};
        initClass(domain, SHIELD_CLASS, attributesToHash, atts, hashed);
        //Simple State
        atts = new String[] {IS_BEHIND_SHIELDS, DANGER_LEVEL, AT_LEFT_WALL, AT_RIGHT_WALL, CAN_SHOOT, P1_LIVES, P2_LIVES};
        hashed = new boolean[]{true, true, true, true, true, true, true};
        initClass(domain, SIMPLE_STATE_CLASS, attributesToHash, atts, hashed);
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

    public static State getInitialState(Domain d) {
        State s = new State();
        addMeta(d, s, 1, 3, 0);
        addShip(d, s, 0, MetaData.MAP_WIDTH / 2, -1, -1, 0, 0, 2, -1);
        addShip(d, s, 1, MetaData.MAP_WIDTH / 2, -1, -1, 0, 0, 2, -1);
        addAlienWave(d, s, 0, 3);
        addAlienWave(d, s, 1, 3);
        addInitialShields(d, s);
        return s;
    }

    private static ObjectInstance addMeta(Domain d, State s, int roundNum, int waveSize, int alienShotEnergy) {
        ObjectInstance meta = new ObjectInstance(d.getObjectClass(META_CLASS), "MetaData");
        meta.setValue(ROUND_NUM, roundNum);
        meta.setValue(ALIEN_WAVE_SIZE, waveSize);
        meta.setValue(ALIEN_SHOT_ENERGY, alienShotEnergy);
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

    private static ObjectInstance addShield(Domain d, State s, int x, int y) {
        ObjectInstance shield = new ObjectInstance(d.getObjectClass(SHIELD_CLASS), SHIELD_CLASS + ShieldCounter++);
        shield.setValue(X, x);
        shield.setValue(Y, y);
        shield.setValue(WIDTH, 1);
        s.addObject(shield);
        return shield;
    }

    private static void addAlienWave(Domain d, State s, int playerNum, int waveSize) {
        int pos;
        for (int i=0; i<waveSize; i++) {
            pos = i*3;
            addAlien(d, s, playerNum, pos);
        }
    }

    private static ObjectInstance addAlien(Domain d, State s, int playerNum, int position) {
        ObjectInstance alien = new ObjectInstance(d.getObjectClass(ALIEN_CLASS), ALIEN_CLASS + AlienCounter++);
        int y = MetaData.MAP_HEIGHT/2 + (playerNum == 0? 1 : -1);
        alien.setValue(X, MetaData.MAP_WIDTH - 1 - position);
        alien.setValue(Y, y);
        alien.setValue(WIDTH, 1);
        alien.setValue(PNUM, playerNum);
        s.addObject(alien);
        return alien;
    }

    public AgentType createDefaultAgentType(Domain d) {
        return new AgentType(SHIP_CLASS, d.getObjectClass(SHIP_CLASS), actionList);
    }
}
