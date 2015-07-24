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
        DomainDefinition.initAttributes(domain, attributeMap);
        defineClasses(domain, mAttributesToHash);
        //defineActions(domain);
        defineSimpleActions(domain);
        DomainDefinition.defineInteractions(domain);
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



    private void defineClasses(Domain domain, Map<String, List<Attribute>> attributesToHash) {
        //Meta data
        String[] atts = {ROUND_NUM, ALIEN_SHOT_ENERGY, ALIEN_WAVE_SIZE};
        boolean[] hashed = {false, false, false};
        DomainDefinition.initClass(domain, META_CLASS, attributesToHash, atts, hashed, attributeMap);
        //Ships
        atts = new String[]{X, Y, WIDTH, PNUM, MISSILE_CONTROL, ALIEN_FACTORY, MISSILE_COUNT, KILLS, LIVES, RESPAWN_TIME};
        hashed = new boolean[]{true, false, true, true, true, true, false, false, false, false};
        DomainDefinition.initClass(domain, SHIP_CLASS, attributesToHash, atts, hashed, attributeMap);
        //Missiles and Bullets and Buildings
        atts = new String[] {X, Y, WIDTH, PNUM};
        hashed = new boolean[]{true, true, false, true};
        DomainDefinition.initClass(domain, MISSILE_CLASS, attributesToHash, atts, hashed, attributeMap);
        DomainDefinition.initClass(domain, BULLET_CLASS, attributesToHash, atts, hashed, attributeMap);
        DomainDefinition.initClass(domain, ALIEN_CLASS, attributesToHash, atts, hashed, attributeMap);
        DomainDefinition.initClass(domain, ALIEN_FACTORY_CLASS, attributesToHash, atts, hashed, attributeMap);
        DomainDefinition.initClass(domain, MISSILE_CONTROLLER_CLASS, attributesToHash, atts, hashed, attributeMap);
        //Shields
        atts = new String[] {X, Y, WIDTH};
        hashed = new boolean[]{true, true, false};
        DomainDefinition.initClass(domain, SHIELD_CLASS, attributesToHash, atts, hashed, attributeMap);
        //Simple State
        atts = new String[] {IS_BEHIND_SHIELDS, DANGER_LEVEL, AT_LEFT_WALL, AT_RIGHT_WALL, CAN_SHOOT, P1_LIVES, P2_LIVES};
        hashed = new boolean[]{true, true, true, true, true, true, true};
        DomainDefinition.initClass(domain, SIMPLE_STATE_CLASS, attributesToHash, atts, hashed, attributeMap);
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
