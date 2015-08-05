package za.co.nimbus.game.saDomain.opponents;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.saDomain.SpaceInvaderSingleAgentDomainFactory;

import static za.co.nimbus.game.constants.Commands.MoveLeft;
import static za.co.nimbus.game.constants.Commands.Nothing;

/**
 * Autobot oppnent that moves under the shields and waits there
 */
public class SittingDuck extends AbstractOpponent {

    public SittingDuck(Domain d) {
        super(d);
    }

    @Override
    public Action getProposedMove(State s) {
        return domain.getAction(Nothing);
    }

    @Override
    public void init(SpaceInvaderSingleAgentDomainFactory spaceInvaderDomainFactory) { }
}
