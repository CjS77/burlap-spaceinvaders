package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.vfa.ActionApproximationResult;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A greedy policy that takes it's Q-value estimates from a Value Function Approximation
 */
public class GreedyVFABasedPolicy extends Policy {
    private final ValueFunctionApproximation vfa;
    private final Domain domain;

    public GreedyVFABasedPolicy(ValueFunctionApproximation vfa, Domain d) {
        this.vfa = vfa;
        this.domain = d;
    }

    @Override
    public AbstractGroundedAction getAction(State s) {
        List<Action> actions = domain.getActions();
        List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(actions,s);
        List<ActionApproximationResult> stateActionValues = vfa.getStateActionValues(s, gas);
        GroundedAction chosenAction = null;
        double maxValue = Double.NEGATIVE_INFINITY;
        for (ActionApproximationResult stateActionValue : stateActionValues) {
            double val = stateActionValue.approximationResult.predictedValue;
            if (val > maxValue) {
                maxValue = val;
                chosenAction = stateActionValue.ga;
            }
        }
        return chosenAction;
    }

    @Override
    public List<ActionProb> getActionDistributionForState(State s) {
        List<ActionProb> result = new ArrayList<>();
        result.add(new ActionProb(getAction(s), 1.0));
        return result;
    }

    @Override
    public boolean isStochastic() {
        return false;
    }

    @Override
    public boolean isDefinedFor(State s) {
        return true;
    }
}
