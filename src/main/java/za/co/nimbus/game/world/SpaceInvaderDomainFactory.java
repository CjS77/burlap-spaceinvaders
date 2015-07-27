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
        domain.setJointActionModel(new SpaceInvaderJAM(domain, seed));
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

    public AgentType createDefaultAgentType(Domain d) {
        return new AgentType(SHIP_CLASS, d.getObjectClass(SHIP_CLASS), actionList);
    }
}
