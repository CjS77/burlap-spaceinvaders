package za.co.nimbus.game.saDomain;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.SADomain;
import za.co.nimbus.game.rules.SpaceInvaderMechanics;
import za.co.nimbus.game.saDomain.opponents.OpponentStrategyFactory;
import za.co.nimbus.game.world.DomainDefinition;

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
    private final Map<String, List<Attribute>> mAttributesToHash = new HashMap<>();
    private static int AlienCounter = 0;
    private static int ShieldCounter = 0;
    private OpponentStrategy opponentStrategy = null;
    private final String opponentClass;

    public SpaceInvaderSingleAgentDomainFactory(String opponentClass, Integer seed) {
        SpaceInvaderMechanics.seedRNG(seed);
        this.opponentClass = opponentClass;
    }

    @Override
    public Domain generateDomain() {
        SADomain domain = new SADomain();
        opponentStrategy = OpponentStrategyFactory.createOpponent(opponentClass, domain);
        mAttributesToHash.clear();
        DomainDefinition.initAttributes(domain, attributeMap);
        defineClasses(domain, mAttributesToHash);
        defineActions(domain);
        DomainDefinition.defineInteractions(domain);
        return domain;
    }

    private void defineActions(SADomain domain) {
        domain.addAction(new SingleShipAction(MoveLeft, domain, opponentStrategy));
        domain.addAction(new SingleShipAction(Nothing, domain, opponentStrategy));
        domain.addAction(new SingleShipAction(MoveRight, domain, opponentStrategy));
        domain.addAction(new SingleShipAction(Shoot, domain, opponentStrategy));
        domain.addAction(new SingleShipAction(BuildAlienFactory, domain, opponentStrategy));
        domain.addAction(new SingleShipAction(BuildMissileController, domain, opponentStrategy));
        domain.addAction(new SingleShipAction(BuildShield, domain, opponentStrategy));
    }

    private void defineClasses(Domain domain, Map<String, List<Attribute>> attributesToHash) {
        //Meta data
        String[] atts = {   ROUND_NUM, ALIEN_SHOT_ENERGY, ALIEN_WAVE_SIZE, ACTUAL_PNUM};
        boolean[] hashed = {false,     false,             false,           true};
        DomainDefinition.initClass(domain, META_CLASS, attributesToHash, atts, hashed, attributeMap);
        //Ships
        atts =    new String[]{X,    Y,     WIDTH,  PNUM, MISSILE_CONTROL, ALIEN_FACTORY, MISSILE_COUNT, KILLS, LIVES, RESPAWN_TIME};
        hashed = new boolean[]{true, false, false, false, true,            true,          false,         false, false, false};
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
    }

    /**
     * Returns a map of all the state attributes that should be hashed in a learning algorithm. In general, it is a
     * subset of the full attribute list. This list will only be populated after a call to {@link #generateDomain}
     */
    public Map<String, List<Attribute>> getFullAttributesToHashMap() {
        return mAttributesToHash;
    }

}
