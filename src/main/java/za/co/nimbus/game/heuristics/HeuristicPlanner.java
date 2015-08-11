package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import za.co.nimbus.game.heuristics.Heuristics;
import za.co.nimbus.game.heuristics.SpaceInvaderHeuristicsBasic;

import java.util.ArrayList;
import java.util.List;

import static za.co.nimbus.game.constants.Commands.*;

public class HeuristicPlanner implements QComputablePlanner {
    private final Domain domain;
    private final SpaceInvaderHeuristicsBasic fd;

    public HeuristicPlanner(Domain d, SpaceInvaderHeuristicsBasic fd) {
        domain = d;
        this.fd = fd;
    }

    @Override
    public List<QValue> getQs(State state) {
        List<GroundedAction> actions = Action.getAllApplicableGroundedActionsFromActionList(domain.getActions(), state);
        List<ActionFeaturesQuery> afs = fd.getActionFeaturesSets(state, actions);
        List<QValue> result = new ArrayList<>();
        for (ActionFeaturesQuery af : afs) {
            result.add(new QValue(state, af.queryAction, af.features.get(0).value));
        }
        return result;
    }

    @Override
    public QValue getQ(State state, AbstractGroundedAction action) {
        fd.calculatePriorities(state);
        int index = fd.getIndexForCommand(action.actionName());
        return new QValue(state, action, fd.priorities[index]);
    }
}
