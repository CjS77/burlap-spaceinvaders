package za.co.nimbus.game;

import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.visualizer.Visualizer;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.heuristics.SpaceInvaderFeatures;
import za.co.nimbus.game.heuristics.SpaceInvaderHeuristics2;
import za.co.nimbus.game.heuristics.SpaceInvaderHeuristics3;
import za.co.nimbus.game.heuristics.VFAFile;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.saDomain.*;
import za.co.nimbus.game.visualiser.SpaceInvaderVisualiser;
import za.co.nimbus.game.world.DomainDefinition;

import java.util.List;

/**
 * Training algorithms for SingleAgent
 */
public class SpaceCamp {

    public static final int DEBUG_CODE = 77061800;
    public static GameOver tf = null;
    public static RewardFunction rf = null;
    private static double ε = 0.1;
    private static double α = 0.1;

    public static void main(String[] args) {
        int pNumActual, numEpisodes;
        String oppStrat;
        if (args.length == 5) {
            oppStrat = args[0];
            pNumActual = Integer.valueOf(args[1]);
            numEpisodes = Integer.valueOf(args[2]);
            ε = Double.valueOf(args[3]);
            α = Double.valueOf(args[4]);
        } else {
            pNumActual = 0;
            oppStrat = "RunAndHide";
            numEpisodes = 1000000;
        }

        //VanillaPlanner(pNumActual, oppStrat);
        //LSPITrainer(pNumActual, oppStrat);
        SarsaλTrainer(pNumActual, oppStrat, numEpisodes);
    }

    private static SADomain initGame(String oppStrat, Integer seed) {
        SpaceInvaderSingleAgentDomainFactory df = new SpaceInvaderSingleAgentDomainFactory(oppStrat, seed);
        SADomain domain = (SADomain) df.generateDomain();
        tf = new GameOver(domain);
        rf = new SASpaceInvaderRewardFunction(tf);
        return domain;
    }

    public static void SarsaλTrainer(int pNumActual, String oppStrat, int numEpisodes) {
        DPrint.cf(0, "Running interactive using %s opponentStrategy as playing as player %d, running %d episodes\n", oppStrat, pNumActual + 1, numEpisodes);
        SADomain d = initGame(oppStrat, null);
        State s0 = DomainDefinition.getInitialState(d, pNumActual);
        double λ = 0.9;
        double γ = 1.0;
        DPrint.cf(0, "Learning parameters:\n\tγ = %5.3f\n\tα = %5.3f\n\tλ = %4.2f\n\tε = %4.2f\n", γ, α, λ, ε);
        DPrint.toggleCode(VFAFile.DEBUG_CODE, false);
        String pFile =  "SIHeuristicsP" + pNumActual;
        SpaceInvaderHeuristics3 fd = new SpaceInvaderHeuristics3(pFile+".bin", 0.0, d);
        ValueFunctionApproximation vfa = fd.generateVFA();
        GradientDescentSarsaLam sarsa = new GradientDescentSarsaLam(d, rf, tf, γ, vfa, α, null, 201, λ);
        sarsa.setLearningPolicy(new EpsilonGreedy(sarsa, ε));
        StateParser sp = new StateJSONParser(d);
        double Rcum = 0.0;
        int PER = 1000;
        for(int i = 0; i < numEpisodes; i++){
            EpisodeAnalysis ea = sarsa.runLearningEpisodeFrom(s0); //run learning episode
            double R = ea.getDiscountedReturn(γ);
            Rcum += R;
            System.out.printf(R > 0? "o" : ".");
            VFAFile.saveVFAToFile(vfa, pFile+".bin", s0, d, fd);
            if (i%PER == 0) {
                System.out.printf("\n%d games simulated\nAverage Return over period: %5.2f\n", i, Rcum/PER);
                Rcum = 0;
                ea.writeToFile(String.format("sarsa%d/e%04d", pNumActual, i), sp); //record episode to a file
                VFAFile.saveVFAToASCIIFile(vfa, pFile+".txt", s0, d, fd);
                List<QValue> qs = sarsa.getQs(s0);
                for (QValue q : qs) {
                    System.out.printf("\tQ(%s) = %4.2f\n", q.a.actionName(), q.q);
                }
            }
        }
        System.out.println("\nLearning complete");
        VFAFile.saveVFAToASCIIFile(vfa, pFile+".txt", s0, d, fd);
        VFAFile.saveVFAToFile(vfa, pFile+".bin", s0, d, fd);
        Visualizer v = SpaceInvaderVisualiser.getVisualiser();
        new EpisodeSequenceVisualizer(v, d, sp, "sarsa" + pNumActual, 26*MetaData.MAP_WIDTH, 26*MetaData.MAP_HEIGHT);
    }


}
