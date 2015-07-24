package za.co.nimbus.game.rules;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.SIMPLE_STATE_CLASS;

/**
 * Defines the rules for the Ship's allowable Actions in the simplified State
 */
public class SimpleShipAction extends SingleAction {
    public SimpleShipAction(SGDomain d, String name) {
        super(d, name);
    }

    @Override
    public boolean isApplicableInState(State state, String actingAgent, String[] strings) {
        String action = this.actionName;
        // You can always do Nothing
        if (action.equals(Nothing)) return true;
        ObjectInstance player = state.getFirstObjectOfClass(SIMPLE_STATE_CLASS);
        //Not a simplified state agent:
        if (player == null) return true;
        int lives = player.getIntValForAttribute(P1_LIVES);
        switch (action) {
            case MoveLeft:
                return !player.getBooleanValForAttribute(AT_LEFT_WALL);
            case MoveRight:
                return !player.getBooleanValForAttribute(AT_RIGHT_WALL);
            case Shoot:
                return player.getBooleanValForAttribute(CAN_SHOOT);
            case BuildAlienFactory:
            case BuildMissileController:
            case BuildShield:
                return lives > 0;
            default:
                return true;
        }
    }
}
