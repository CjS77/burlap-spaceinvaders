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
            case "SittingDuck":
                return new SittingDuck(d);
        }
        throw new IllegalArgumentException(opponentClass + ": No opponent class of this name is registered");
    }
}
