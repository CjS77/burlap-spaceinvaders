package za.co.nimbus.game.visualiser;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.explorers.SGVisualExplorer;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.Visualizer;
import za.co.nimbus.game.constants.MetaData;

import java.awt.*;

import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

public class SpaceInvaderVisualiser {
    public static Visualizer getVisualiser() {
        Visualizer vis = new Visualizer(getStateRenderLayer());
        vis.setBGColor(Color.BLACK);
        return vis;
    }

    private static StateRenderLayer getStateRenderLayer() {
        StateRenderLayer rl = new StateRenderLayer();
        rl.addStaticPainter(new BackgroundPainter());
        EntityPainter painter = new EntityPainter();
        rl.addObjectClassPainter(SHIP_CLASS, painter);
        rl.addObjectClassPainter(ALIEN_CLASS, painter);
        rl.addObjectClassPainter(BULLET_CLASS, painter);
        rl.addObjectClassPainter(MISSILE_CLASS, painter);
        rl.addObjectClassPainter(SHIELD_CLASS, painter);
        rl.addObjectClassPainter(ALIEN_FACTORY_CLASS, painter);
        rl.addObjectClassPainter(MISSILE_CONTROLLER_CLASS, painter);

        return rl;
    }

    public static SGVisualExplorer getVisualExplorer(SGDomain d, State s) {
        Visualizer v = getVisualiser();
        SGVisualExplorer exp = new SGVisualExplorer(d, v, s, 26 * MetaData.MAP_WIDTH, 26 * MetaData.MAP_HEIGHT);
        exp.setJAC(" ");   //press c to execute the constructed joint action
        exp.addKeyAction("s", SHIP_CLASS + "0:" + Nothing);
        exp.addKeyAction("a", SHIP_CLASS + "0:" + MoveLeft);
        exp.addKeyAction("d", SHIP_CLASS + "0:" + MoveRight);
        exp.addKeyAction("w", SHIP_CLASS + "0:" + Shoot);
        exp.addKeyAction("z", SHIP_CLASS + "0:" + BuildAlienFactory);
        exp.addKeyAction("x", SHIP_CLASS + "0:" + BuildMissileController);
        exp.addKeyAction("c", SHIP_CLASS + "0:" + BuildShield);
        //Player 2 key bindings
        exp.addKeyAction("k", SHIP_CLASS + "1:" + Nothing);
        exp.addKeyAction("j", SHIP_CLASS + "1:" + MoveLeft);
        exp.addKeyAction("l", SHIP_CLASS + "1:" + MoveRight);
        exp.addKeyAction("i", SHIP_CLASS + "1:" + Shoot);
        exp.addKeyAction("m", SHIP_CLASS + "1:" + BuildAlienFactory);
        exp.addKeyAction(",", SHIP_CLASS + "1:" + BuildMissileController);
        exp.addKeyAction(".", SHIP_CLASS + "1:" + BuildShield);
        return exp;
    }

    public static VisualExplorer getVisualExplorer(SADomain d, State s) {
        Visualizer v = getVisualiser();
        VisualExplorer exp = new VisualExplorer(d, v, s, 26 * MetaData.MAP_WIDTH, 26 * MetaData.MAP_HEIGHT);
        exp.addKeyAction("s", Nothing);
        exp.addKeyAction("a", MoveLeft);
        exp.addKeyAction("d", MoveRight);
        exp.addKeyAction("w", Shoot);
        exp.addKeyAction("z", BuildAlienFactory);
        exp.addKeyAction("x", BuildMissileController);
        exp.addKeyAction("c", BuildShield);
        return exp;
    }
}
