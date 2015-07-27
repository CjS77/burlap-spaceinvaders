package za.co.nimbus.game.saDomain.opponents;

import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.helpers.Location;
import za.co.nimbus.game.saDomain.OpponentStrategy;
import za.co.nimbus.game.saDomain.SingleShipAction;

import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * Opponent strategy that implements default ApplyMove function
 */
public abstract class AbstractOpponent implements OpponentStrategy {
    public static int debugCode = 1867701;

    protected final Domain domain;

    public AbstractOpponent(Domain d) {
        domain = d;
    }

    /**
     * Validates the proposed move by calling Action.applicableInState first. If it is invalid, null is returned,
     * which indicates a no-op move
     */
    @Override
    public Action getValidMove(State s) {
        Action move = getProposedMove(s);
        return move.applicableInState(s, "")? move : null;
    }
}
