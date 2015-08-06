package za.co.nimbus.game.saDomain.opponents;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import za.co.nimbus.game.heuristics.SpaceInvaderHeuristics2;
import za.co.nimbus.game.heuristics.ValueInitializationFromVFA;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.saDomain.SASpaceInvaderRewardFunction;
import za.co.nimbus.game.saDomain.SpaceInvaderSingleAgentDomainFactory;

/**
 * Use the Sarsa-λ and sparse Sampling strategies learnt by primary AI
 */
public class Feynman extends AbstractOpponent {
    private static final int SEARCH_DEPTH = 2;
    private static final int SAMPLES = 1;
    private final int actualPNum;
    private Policy policy;

    public Feynman(int actualPNum) {
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
        //Load the VFA coefficients file
        String vfaCoeffFile = "SIHeuristics2p" + actualPNum + ".bin";
        SpaceInvaderHeuristics2 fd = new SpaceInvaderHeuristics2();
        fd.setVFAWeightFile(vfaCoeffFile);
        ValueFunctionApproximation vfa = fd.generateVFA();
        //Create the domain
        GameOver tf = new GameOver(domain);
        RewardFunction rf = new SASpaceInvaderRewardFunction(tf);
        // Create the planner
        SparseSampling planner = new SparseSampling(domain, rf, tf, 0.95, new DiscreteStateHashFactory(), SEARCH_DEPTH, SAMPLES);
        planner.setForgetPreviousPlanResults(false);
        // Set the leaf-node function for the planner
        ValueFunctionInitialization vinit = new ValueInitializationFromVFA(vfa);
        planner.setValueForLeafNodes(vinit);
        policy = new GreedyQPolicy(planner);
    }
}
