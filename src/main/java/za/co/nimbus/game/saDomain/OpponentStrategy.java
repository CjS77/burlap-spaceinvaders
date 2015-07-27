package za.co.nimbus.game.saDomain;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;

/**
 * Interface for opponent strategy
 */
public interface OpponentStrategy {
    /**
     * Returns the proposed opponent move for the given state
     */
    Action getProposedMove(State s);

    /**
     * Get a move that is guaranteed to be valid in the given state. This is typically checked by calling
     * isApplicableInState. If no valid move is available, null can be returned as a default no-op Action
     */
    Action getValidMove(State s);
}
