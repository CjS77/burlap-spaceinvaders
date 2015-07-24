package za.co.nimbus.game.agents;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.PolicyFromJointPolicy;
import burlap.behavior.stochasticgame.agents.mavf.MultiAgentVFPlanningAgent;
import burlap.behavior.stochasticgame.mavaluefunction.SGBackupOperator;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.MaxQ;
import burlap.behavior.stochasticgame.mavaluefunction.policies.EGreedyJointPolicy;
import burlap.behavior.stochasticgame.mavaluefunction.vfplanners.MAValueIteration;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;

/**
 * An instatiantion of the MultiAgent Value function Planning Agent
 */
public class Plato {

    private final MultiAgentVFPlanningAgent agent;

    /**
     * Initializes. The underlining joint policy of the policy must be an instance of {@link MAQSourcePolicy} or a runtime exception will be thrown.
     * The joint policy will automatically be set to use the provided planner as the value function source.
     * @param domain  the domain in which the agent will act
     * @param tf the termination function
     * @param discount the Q-value discount factor for subsequent states
     * @param hashFactory a hashing factory to memoise states
     * @param qInit the initial value for the Q-value
     * @param maxDelta - the change in Q-values before planning stops
     * @param maxIterations - the max number of planning iterations before planning stops
     */
    public Plato(SGDomain domain, JointActionModel gameMechanics, JointReward rf, TerminalFunction tf, double discount,
                 StateHashFactory hashFactory, double qInit, double maxDelta, int maxIterations) {

        SGBackupOperator backupOperator = new MaxQ();
        MAValueIteration planner = new MAValueIteration(domain, gameMechanics, rf, tf, discount, hashFactory, qInit,
                backupOperator, maxDelta, maxIterations);
        PolicyFromJointPolicy policy = new PolicyFromJointPolicy(new EGreedyJointPolicy(0.01));
        agent = new MultiAgentVFPlanningAgent(domain, planner, policy);
    }


    public MultiAgentVFPlanningAgent getAgent() {
        return agent;
    }
}
