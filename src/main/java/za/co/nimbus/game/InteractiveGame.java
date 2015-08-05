package za.co.nimbus.game;

import burlap.debugtools.DPrint;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.saDomain.SASpaceInvaderRewardFunction;
import za.co.nimbus.game.saDomain.SpaceInvaderSingleAgentDomainFactory;
import za.co.nimbus.game.visualiser.SpaceInvaderVisualiser;
import za.co.nimbus.game.world.DomainDefinition;

public class InteractiveGame {

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

        DPrint.cf(0, "Running interactive using %s opponentStrategy as playing as player %d\n", oppStrat, pNumActual+1);
        SpaceInvaderSingleAgentDomainFactory df = new SpaceInvaderSingleAgentDomainFactory(oppStrat, pNumActual);
        SADomain d = (SADomain) df.generateDomain();
        State s = DomainDefinition.getInitialState(d, pNumActual);
        VisualExplorer exp = SpaceInvaderVisualiser.getVisualExplorer(d, s);
        GameOver tf = new GameOver(d);
        exp.setTerminalFunction(tf);
        exp.setTrackingRewardFunction(new SASpaceInvaderRewardFunction(tf));
        exp.initGUI();
    }
}
