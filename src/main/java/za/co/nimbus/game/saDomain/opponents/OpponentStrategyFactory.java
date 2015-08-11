package za.co.nimbus.game.saDomain.opponents;

import burlap.oomdp.core.Domain;
import za.co.nimbus.game.saDomain.OpponentStrategy;

/**
 * Factory class for OpponentStrategy objects
 */
public class OpponentStrategyFactory {
    public static OpponentStrategy createOpponent(String opponentClass, Domain d) {
        switch (opponentClass) {
            case "RunAndHide":
                return new RunAndHide(d);
            case "Foxhole":
                return new Foxhole(d);
            case "SittingDuck":
                return new SittingDuck(d);
            case "Feynman0":
                return new Feynman(0);
            case "Feynman1":
                return new Feynman(1);
            case "Marx0":
                return new Marx(0);
            case "Marx1":
                return new Marx(1);
        }
        throw new IllegalArgumentException(opponentClass + ": No opponent class of this name is registered");
    }
}
