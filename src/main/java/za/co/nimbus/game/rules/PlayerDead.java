package za.co.nimbus.game.rules;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;
import static za.co.nimbus.game.constants.Attributes.LIVES;

/**
 * Determine whether a player is dead
 */
public class PlayerDead extends PropositionalFunction {
    public static final String NAME = "PlayerDead";
    public PlayerDead(Domain domain) {
        super(NAME, domain, new String[] {SHIP_CLASS});
    }

    @Override
    public boolean isTrue(State state, String[] params) {
        ObjectInstance ship = state.getObject(params[0]);
        int lives = ship.getIntValForAttribute(LIVES);
        return lives < 0;
    }
}
