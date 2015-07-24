package za.co.nimbus.game.rules;

import burlap.oomdp.core.*;
import za.co.nimbus.game.constants.MetaData;

import java.util.List;

import static za.co.nimbus.game.constants.ObjectClasses.META_CLASS;
import static za.co.nimbus.game.constants.Attributes.ROUND_NUM;

/**
 * Determines the terminal function for SpaceInvaders (is it Game Over?)
 */
public class GameOver implements TerminalFunction {
    public static final String NAME= "GameOver";
    private final Domain domain;

    public GameOver(Domain domain) {
        this.domain = domain;
    }

    @Override
    public boolean isTerminal(State state) {
        PropositionalFunction pf = domain.getPropFunction(PlayerDead.NAME);
        List<GroundedProp> pfs = pf.getAllGroundedPropsForState(state);
        for (GroundedProp prop : pfs) {
            if (prop.isTrue(state)) return true;
        }
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        int round = meta.getIntValForAttribute(ROUND_NUM);
        return round > MetaData.ROUND_LIMIT;
    }
}
