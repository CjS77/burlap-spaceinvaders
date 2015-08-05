package za.co.nimbus.game.saDomain.opponents;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.saDomain.SpaceInvaderSingleAgentDomainFactory;

import static za.co.nimbus.game.constants.Attributes.MISSILE_CONTROL;
import static za.co.nimbus.game.constants.Attributes.X;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;

/**
 * Autobot opponent that moves under the shields and sits there, shooting whenever possible
 */
public class Foxhole extends AbstractOpponent {

    public Foxhole(Domain d) {
        super(d);
    }

    @Override
    public Action getProposedMove(State s) {
        ObjectInstance ship = s.getObject(SHIP_CLASS + "0");
        int x = ship.getIntValForAttribute(X);
        if (x > 2) {
            return domain.getAction(MoveLeft);
        } else {
            return ship.getIntValForAttribute(MISSILE_CONTROL) < 0? domain.getAction(BuildMissileController) : domain.getAction(Shoot);
        }
    }

    @Override
    public void init(SpaceInvaderSingleAgentDomainFactory spaceInvaderDomainFactory) { }
}
