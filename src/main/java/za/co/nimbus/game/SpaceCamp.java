package za.co.nimbus.game;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.visualizer.Visualizer;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.saDomain.PlannerFactory;
import za.co.nimbus.game.saDomain.SASpaceInvaderRewardFunction;
import za.co.nimbus.game.saDomain.SpaceInvaderSingleAgentDomainFactory;
import za.co.nimbus.game.visualiser.SpaceInvaderVisualiser;
import za.co.nimbus.game.world.DomainDefinition;


/**
 * Training algorithms for SingleAgent
 */
public class SpaceCamp {

    public static void main(String[] args) {
        int pNumActual;
        String oppStrat;
        if (args.length == 2) {
            pNumActual = Integer.valueOf(args[1]);
            oppStrat = args[0];
        } else {
            pNumActual = 0;
            oppStrat = "RunAndHide";
        }

        DPrint.cf(0, "Running interactive using %s opponentStrategy as playing as player %d\n", oppStrat, pNumActual + 1);
        SpaceInvaderSingleAgentDomainFactory df = new SpaceInvaderSingleAgentDomainFactory(oppStrat, pNumActual);
        SADomain d = (SADomain) df.generateDomain();
        State s0 = DomainDefinition.getInitialState(d, 1);
        GameOver tf = new GameOver(d);
        RewardFunction rf = new SASpaceInvaderRewardFunction(tf);
        String plannerType = "SparseSampling";
        QComputablePlanner planner = PlannerFactory.getPlanner(plannerType, d, rf, tf);
        Policy p = new GreedyQPolicy(planner);
        EpisodeAnalysis ea = p.evaluateBehavior(s0, rf, tf, 5000);
        StateParser sp = new StateJSONParser(d);
        ea.writeToFile("SpaceCamp/" + plannerType, sp);

        Visualizer v = SpaceInvaderVisualiser.getVisualiser();
        new EpisodeSequenceVisualizer(v, d, sp, "SpaceCamp");
    }
}
