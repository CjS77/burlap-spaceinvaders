package za.co.nimbus.stochasticgame.agents;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SingleAction;
import za.co.nimbus.game.constants.Commands;

import java.util.List;
import java.util.Map;

import static za.co.nimbus.game.constants.Commands.Nothing;

/**
 * Simple non-learning agent that heads for the shields and stays there
 */
public class RunAndHide extends Agent {
    private int turnCount = 0;

    @Override
    public void gameStarting() {
        turnCount = 0;
    }

    @Override
    public GroundedSingleAction getAction(State s) {
        String command = turnCount++ < 6? Commands.MoveLeft: Nothing;
        List<GroundedSingleAction> gsas = SingleAction.getAllPossibleGroundedSingleActions(s, this.worldAgentName, this.agentType.actions);
        for (GroundedSingleAction gsa : gsas) {
            if (gsa.justActionString().equals(command)) return gsa;
        }
        throw new IllegalStateException("Oops. The desired action should have been found");
    }

    @Override
    public void observeOutcome(State state, JointAction jointAction, Map<String, Double> map, State state1, boolean b) {

    }

    @Override
    public void gameTerminated() {

    }
}
