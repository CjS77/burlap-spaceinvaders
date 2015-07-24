package za.co.nimbus.game;

import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.auxiliary.performance.AgentFactoryAndType;
import burlap.behavior.stochasticgame.auxiliary.performance.MultiAgentExperimenter;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.*;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;
import burlap.oomdp.stochasticgames.common.VisualWorldObserver;
import org.apache.log4j.Logger;
import za.co.nimbus.game.agents.RunAndHide;
import za.co.nimbus.game.agents.SpaceInvaderAgentFactory;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.rules.SimpleSpaceInvaderState;
import za.co.nimbus.game.rules.SpaceInvaderRewardFunction;
import za.co.nimbus.game.visualiser.SpaceInvaderVisualiser;
import za.co.nimbus.game.world.SpaceInvaderDomainFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;

public class SpaceInvaderGame implements WorldGenerator {
    private static final double INITIAL_Q = 0.0;
    public static final double DISCOUNT = 0.99;
    public final SpaceInvaderDomainFactory domainFactory;
    private static final Logger logger = Logger.getLogger(SpaceInvaderGame.class);
    private final Domain domain;

    /**
     * @param seed   - null for random play, or an integer for repeatability
     */
    public SpaceInvaderGame(Integer seed) {
        domainFactory = new SpaceInvaderDomainFactory(seed);
        domain = domainFactory.generateDomain();
    }

    @Override
    public World generateWorld() {
        TerminalFunction tf = new GameOver(domain);
        JointReward rf = new SpaceInvaderRewardFunction(domain);
        ConstantSGStateGenerator sg = new ConstantSGStateGenerator(SpaceInvaderDomainFactory.getInitialState(domain));
        //SimpleSpaceInvaderState simpleState = new SimpleSpaceInvaderState(domain);
        return new World((SGDomain) domain, rf, tf, sg);
    }

    public void run(AgentFactoryAndType opponent) {
        StateHashFactory hf = new DiscreteStateHashFactory(domainFactory.getFullAttributesToHashMap());
        World world = generateWorld();
        AgentFactory agentFactory = new SpaceInvaderAgentFactory("Plato", (SGDomain) domain, world.getActionModel(),
                world.getRewardModel(), world.getTF(), DISCOUNT, hf, INITIAL_Q);
        Agent p0 = agentFactory.generateAgent();
        p0.joinWorld(world, domainFactory.createDefaultAgentType(domain));
        Agent p1 = opponent.agentFactory.generateAgent();
        p1.joinWorld(world, opponent.at);
        GameAnalysis ga = world.runGame();
        StateParser sp = new StateJSONParser(domain);
        ga.writeToFile("output/saussure", sp);
        VisualWorldObserver replayer = new VisualWorldObserver((SGDomain) domain, SpaceInvaderVisualiser.getVisualiser());
        replayer.setFrameDelay(400);
        replayer.initGUI();
        replayer.replayGame(ga);
    }

    /**
     * @param nTrials     - the number of times to run each experiment (for fully deterministic models, just make this 1)
     * @param trialLength
     * @param opponent
     */
    public void experimenterAndPlotter(int nTrials, int trialLength, AgentFactoryAndType opponent) {
//        logger.info("Starting learning process with "+nTrials+" trials per iteration.");
//        TerminalFunction tf = new GameOver(domain);
//        AgentFactoryAndType af0 = new AgentFactoryAndType(agentFactory, domainFactory.createDefaultAgentType(domain));
//        MultiAgentExperimenter exp = new MultiAgentExperimenter(this, tf, nTrials, trialLength, af0, opponent);
//        exp.setUpPlottingConfiguration(400, 400, 2, 850, TrialMode.MOSTRECENTANDAVERAGE,
//                PerformanceMetric.CUMULATIVESTEPSPEREPISODE,
//                PerformanceMetric.MEDIANEPISODEREWARD,
//                PerformanceMetric.AVERAGEEPISODEREWARD);
//        //exp.toggleVisualPlots(false);
//        exp.startExperiment();
//        LocalDateTime date = LocalDateTime.now();
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("mm-dd-hhmm");
//        exp.writeStepAndEpisodeDataToCSV("output/saussure" + date.format(df));
    }


    public static void main(String[] args) {
        SpaceInvaderGame game = new SpaceInvaderGame(null);
        List<SingleAction> actions = new ArrayList<>();
        actions.add(game.domainFactory.actionList.get(0)); //MoveLeft
        actions.add(game.domainFactory.actionList.get(1)); //Nothing
        AgentFactoryAndType opponent = new AgentFactoryAndType(
                RunAndHide::new,
                new AgentType(SHIP_CLASS, game.domain.getObjectClass(SHIP_CLASS), actions)
        );
        //game.experimenterAndPlotter(1, 1000000, opponent);
        game.run(opponent);

    }
}


