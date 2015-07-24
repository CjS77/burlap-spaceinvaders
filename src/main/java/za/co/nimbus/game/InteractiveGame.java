package za.co.nimbus.game;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.explorers.SGVisualExplorer;
import za.co.nimbus.game.rules.GameOver;
import za.co.nimbus.game.rules.SpaceInvaderMechanics;
import za.co.nimbus.game.rules.SpaceInvaderRewardFunction;
import za.co.nimbus.game.visualiser.SpaceInvaderVisualiser;
import za.co.nimbus.game.world.SpaceInvaderDomainFactory;

public class InteractiveGame {

    public static void main(String[] args) {
        SpaceInvaderDomainFactory df = new SpaceInvaderDomainFactory(0);
        SGDomain d = (SGDomain) df.generateDomain();
        State s = SpaceInvaderDomainFactory.getInitialState(d);
        SGVisualExplorer exp = SpaceInvaderVisualiser.getVisualExplorer(d, s);
        exp.setTerminalFunction(new GameOver(d));
        exp.setRewardFunction(new SpaceInvaderRewardFunction(d));
        exp.initGUI();
    }
}
