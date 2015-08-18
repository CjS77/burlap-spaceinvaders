package za.co.nimbus.stochasticgame.agents;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.*;

/**
 * A factory method that produces Agents for the Space Invader simulation
 */
public class SpaceInvaderAgentFactory implements AgentFactory {
    private final String agentType;
    private final SGDomain domain;
    private final JointActionModel gameMechanics;
    private final JointReward rf;
    private final TerminalFunction tf;
    private final double discount;
    private final StateHashFactory hashFactory;
    private final double qInit;
    private double maxDelta = 1e-3;
    private int maxIterations = 1000;

    public SpaceInvaderAgentFactory(String agentType, SGDomain domain, JointActionModel gameMechanics, JointReward rf,
                                    TerminalFunction tf, double discount, StateHashFactory hashFactory, double qInit) {
        this.agentType = agentType;
        this.domain = domain;
        this.gameMechanics = gameMechanics;
        this.rf = rf;
        this.tf = tf;
        this.discount = discount;
        this.hashFactory = hashFactory;
        this.qInit = qInit;
    }

    public void setPlanningParams(double maxDelta, int maxIterations) {
        this.maxDelta = maxDelta;
        this.maxIterations = maxIterations;
    }

    @Override
    public Agent generateAgent() {
        switch (agentType) {
            case "Plato":
                return new Plato(domain, gameMechanics, rf, tf, discount, hashFactory, qInit, maxDelta, maxIterations).getAgent();
        }
        throw new IllegalArgumentException("Unknown Agent type: " + agentType);
    }
}
