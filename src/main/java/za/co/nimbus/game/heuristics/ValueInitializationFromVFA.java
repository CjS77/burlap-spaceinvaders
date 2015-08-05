package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.vfa.ActionApproximationResult;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridge class that allows a planner to provide Q-value estimates for a learning or planning algorithm. An example
 * would be using learning to fit a VFA using Sarsa-λ and then use the results of that to provide the leaf-node estimates
 * for a Sparse Sampling planning algorithm.
 */
public class ValueInitializationFromVFA implements ValueFunctionInitialization {
    private final ValueFunctionApproximation vfa;

    public ValueInitializationFromVFA(ValueFunctionApproximation vfa) {
        this.vfa = vfa;
    }

    @Override
    public double qValue(State s, AbstractGroundedAction a) {
        ArrayList<GroundedAction> gal = new ArrayList<>();
        gal.add((GroundedAction) a);
        ActionApproximationResult result = vfa.getStateActionValues(s, gal).get(0);
        return result.approximationResult.predictedValue;
    }

    @Override
    public double value(State s) {
        return vfa.getStateValue(s).predictedValue;
    }
}
