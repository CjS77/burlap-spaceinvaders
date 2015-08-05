package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

/**
 * Bridge class that allows a planner to provide Q-value estimates for a learning or planning algorithm. An example
 * would be using learning to fit a LSPI VFA and then use the results of that to provide the leaf-node estimates
 * for a Sparse Sampling planning algorithm.
 */
public class ValueInititializationFromPlanner implements ValueFunctionInitialization {

    private final QComputablePlanner planner;

    public ValueInititializationFromPlanner(QComputablePlanner planner) {
        this.planner = planner;
    }

    @Override
    public double qValue(State s, AbstractGroundedAction a) {
        return planner.getQ(s, a).q;
    }

    @Override
    public double value(State s) {
       throw new IllegalStateException("This class does not provide value estimates");
    }
}
