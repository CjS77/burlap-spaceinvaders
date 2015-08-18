package za.co.nimbus.stochasticgame.sgRules;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;
import za.co.nimbus.game.constants.MetaData;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;

/**
 * Defines the rules for the Ship's allowable Actions
 */
public class ShipAction extends SingleAction {
    public ShipAction(SGDomain d, String name) {
        super(d, name);
    }

    @Override
    public boolean isApplicableInState(State state, String actingAgent, String[] strings) {
        String action = this.actionName;
        // You can always do Nothing
        if (action.equals(Nothing)) return true;
        ObjectInstance ship = state.getObject(actingAgent);
        int respawning = ship.getIntValForAttribute(RESPAWN_TIME);
        //If you're dead, you can only do Nothing
        if (respawning >= 0) return false;
        int lives = ship.getIntValForAttribute(LIVES);
        int x = ship.getIntValForAttribute(X);
        int pnum = ship.getIntValForAttribute(PNUM);

        int alienFactory = ship.getIntValForAttribute(ALIEN_FACTORY);
        boolean hasAlienFactory = alienFactory >= 0;
        int missileController = ship.getIntValForAttribute(MISSILE_CONTROL);
        boolean hasMissileController = missileController >= 0;

        switch (action) {
            case MoveLeft:
                return (pnum == 0 && x > 1) || (pnum == 1 && x < MetaData.MAP_WIDTH - 2);
            case MoveRight:
                return (pnum == 1 && x > 1) || (pnum == 0 && x < MetaData.MAP_WIDTH - 2);
            case Shoot:
                int missiles = ship.getIntValForAttribute(MISSILE_COUNT);
                return hasMissileController? missiles <= 1 : missiles == 0;
            case BuildAlienFactory:
                return (lives > 0) && (!hasAlienFactory) && (!hasMissileController || Math.abs(x-missileController) > 2 );
            case BuildMissileController:
                return (lives > 0) && (!hasMissileController) && (!hasAlienFactory || Math.abs(x-alienFactory) > 2 );
            case BuildShield:
                return (lives > 0);
            default:
                return true;
        }
    }
}
