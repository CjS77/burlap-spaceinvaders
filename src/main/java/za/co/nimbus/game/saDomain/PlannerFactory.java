package za.co.nimbus.game.saDomain;

import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.rules.GameOver;

/**
 * Factory class for producing Planning Strategies
 */
public class PlannerFactory {
    public static QComputablePlanner getPlanner(String strategy, Domain domain, RewardFunction rf, TerminalFunction tf) {
        switch (strategy) {
            case "SparseSampling":
                return getSparseSampling(domain, rf, tf);
        }
        throw new IllegalArgumentException("Unknown Planning Method requested");
    }

    private static SparseSampling getSparseSampling(Domain d, RewardFunction rf, TerminalFunction tf) {
        SparseSampling ss = new SparseSampling(d, rf, tf, 0.95, new DiscreteStateHashFactory(), 3, 1);
        ss.setForgetPreviousPlanResults(false);
        return ss;
    }

}
