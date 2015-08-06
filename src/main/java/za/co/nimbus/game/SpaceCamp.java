package za.co.nimbus.game;

import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.learning.tdmethods.vfa.GradientDescentSarsaLam;
import burlap.behavior.singleagent.planning.commonpolicies.EpsilonGreedy;
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
    public static void main(String[] args) {
        int pNumActual, numEpisodes;
        String oppStrat;
        if (args.length == 3) {
            pNumActual = Integer.valueOf(args[1]);
            oppStrat = args[0];
            numEpisodes = Integer.valueOf(args[2]);
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
        double learning_rate = 0.01;
        double γ = 0.99;
        double ε = 0.2;
        String pFile =  "SIHeuristicsP" + pNumActual;
        SpaceInvaderHeuristics2 fd = new SpaceInvaderHeuristics2(pFile+".bin", 0.0);
        ValueFunctionApproximation vfa = fd.generateVFA();
        GradientDescentSarsaLam sarsa = new GradientDescentSarsaLam(d, rf, tf, γ, vfa, learning_rate, null, 201, λ);
        sarsa.setLearningPolicy(new EpsilonGreedy(sarsa, ε));
        StateParser sp = new StateJSONParser(d);
        for(int i = 0; i < numEpisodes; i++){
            EpisodeAnalysis ea = sarsa.runLearningEpisodeFrom(s0); //run learning episode
            System.out.printf(".");
            if ( (10*i)%numEpisodes == 0) {
                System.out.printf("\n%d games simulated\n", i);
                ea.writeToFile(String.format("sarsa%d/e%04d", pNumActual, i), sp); //record episode to a file
                VFAFile.saveVFAToASCIIFile(vfa, pFile+".txt", s0, d, fd);
                VFAFile.saveVFAToFile(vfa, pFile+".bin", s0, d, fd);
                List<QValue> qs = sarsa.getQs(s0);
                for (QValue q : qs) {
                    System.out.printf("\tQ(%s) = %4.2f\n", q.a.actionName(), q.q);
                }
            }
        }
        VFAFile.saveVFAToASCIIFile(vfa, pFile+".txt", s0, d, fd);
        VFAFile.saveVFAToFile(vfa, pFile+".bin", s0, d, fd);
        Visualizer v = SpaceInvaderVisualiser.getVisualiser();
        new EpisodeSequenceVisualizer(v, d, sp, "sarsa" + pNumActual, 26*MetaData.MAP_WIDTH, 26*MetaData.MAP_HEIGHT);
    }


}
