package za.co.nimbus.game.saDomain.opponents;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;

import static za.co.nimbus.game.constants.Commands.MoveLeft;
import static za.co.nimbus.game.constants.Commands.MoveRight;
import static za.co.nimbus.game.constants.Commands.Nothing;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;
import static za.co.nimbus.game.constants.Attributes.X;

/**
 * Autobot oppnent that moves under the shields and waits there
 */
public class RunAndHide extends AbstractOpponent {

    public RunAndHide(Domain d) {
        super(d);
    }

    @Override
    public Action getProposedMove(State s) {
        int x = s.getObject(SHIP_CLASS + "1").getIntValForAttribute(X);
        return x > 3 ? domain.getAction(MoveRight) : domain.getAction(Nothing);
    }
}
