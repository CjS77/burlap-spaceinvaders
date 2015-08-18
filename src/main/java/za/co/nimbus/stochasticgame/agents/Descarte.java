package za.co.nimbus.stochasticgame.agents;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.oomdp.stochasticgames.SGDomain;

/**
 * A Q-learning agent that uses the simplified state space and ignores the strategy of the opponent
 */
public class Descarte extends SGNaiveQLAgent {
    public Descarte(SGDomain d, double discount, double learningRate) {
        this(d, discount, learningRate,
                null,
                null);  //TODO
    }

    public Descarte(SGDomain d, double discount, double learningRate, ValueFunctionInitialization qInitizalizer, StateHashFactory hashFactory) {
        super(d, discount, learningRate, qInitizalizer, hashFactory);
    }
}
