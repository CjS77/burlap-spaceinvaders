package za.co.nimbus.game.saDomain.opponents;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.heuristics.HeuristicPlanner;
import za.co.nimbus.game.heuristics.SpaceInvaderHeuristicsBasic;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.saDomain.SASpaceInvaderRewardFunction;
import za.co.nimbus.game.saDomain.SpaceInvaderSingleAgentDomainFactory;

/**
 * Turns Hegel on his head to train Nietzsche
 */
public class Marx extends AbstractOpponent {
    private final int actualPNum;
    private Policy policy;

    public Marx(int actualPNum) {
        super(new SpaceInvaderSingleAgentDomainFactory("SittingDuck", null).generateDomain());
        this.actualPNum = actualPNum;
    }

    @Override
    public Action getProposedMove(State s) {
        AbstractGroundedAction action = policy.getAction(s);
        return domain.getAction(action.actionName());
    }

    @Override
    public void init(SpaceInvaderSingleAgentDomainFactory spaceInvaderDomainFactory) {
        GameOver tf = new GameOver(domain);
        SASpaceInvaderRewardFunction rf = new SASpaceInvaderRewardFunction(tf);
        SpaceInvaderHeuristicsBasic fd = new SpaceInvaderHeuristicsBasic(domain);
        // Create the planner
        HeuristicPlanner planner = new HeuristicPlanner(domain, fd);
        policy = new GreedyQPolicy(planner);
    }
}