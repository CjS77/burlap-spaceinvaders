package za.co.nimbus.game.saDomain;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.rules.*;
import za.co.nimbus.game.world.DomainDefinition;

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
public class SpaceInvaderSingleAgentDomainFactory implements DomainGenerator {

    private final Map<String, Attribute> attributeMap = new HashMap<>();
    public final List<SingleAction> actionList = new ArrayList<>();
    private final Map<String, List<Attribute>> mAttributesToHash = new HashMap<>();
    private static int AlienCounter = 0;
    private static int ShieldCounter = 0;
    private final Integer seed;

    public SpaceInvaderSingleAgentDomainFactory(Integer seed) {
        this.seed = seed;
    }

    @Override
    public Domain generateDomain() {
        SADomain domain = new SADomain();
        mAttributesToHash.clear();
        DomainDefinition.initAttributes(domain, attributeMap);
        defineClasses(domain, mAttributesToHash);
        defineActions(domain);
        DomainDefinition.defineInteractions(domain);
        return domain;
    }

    private void defineActions(SADomain domain) {
        domain.addAction(new SingleShipAction(MoveLeft, domain, seed));
        domain.addAction(new SingleShipAction(Nothing, domain, seed));
        domain.addAction(new SingleShipAction(MoveRight, domain, seed));
        domain.addAction(new SingleShipAction(Shoot, domain, seed));
        domain.addAction(new SingleShipAction(BuildAlienFactory, domain, seed));
        domain.addAction(new SingleShipAction(BuildMissileController, domain, seed));
        domain.addAction(new SingleShipAction(BuildShield, domain, seed));
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
}
