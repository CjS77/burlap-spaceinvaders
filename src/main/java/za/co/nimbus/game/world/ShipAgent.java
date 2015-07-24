package za.co.nimbus.game.world;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;

import java.util.Map;

/**
 * The Container class for managing the Ship's strategy
 */
public final class ShipAgent extends Agent {
    private final int playerNum;

    public ShipAgent(SGDomain d, int playerNum) {
        init(d);
        this.playerNum = playerNum;
    }

    @Override
    public void gameStarting() {
        throw new IllegalAccessError("Not quite ready to go yet");
    }

    @Override
    public GroundedSingleAction getAction(State state) {
        return null;
    }

    @Override
    public void observeOutcome(State state, JointAction jointAction, Map<String, Double> map, State state1, boolean b) {

    }

    @Override
    public void gameTerminated() {

    }
}
