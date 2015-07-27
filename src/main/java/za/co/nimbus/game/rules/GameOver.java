package za.co.nimbus.game.rules;

import burlap.oomdp.core.*;
import za.co.nimbus.game.constants.MetaData;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.ObjectClasses.META_CLASS;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;

/**
 * Determines the terminal function for SpaceInvaders (is it Game Over?)
 */
public class GameOver implements TerminalFunction {
    private final Domain domain;

    public GameOver(Domain domain) {
        this.domain = domain;
    }

    /**
     * Returns true if either player is dead, or the turns have run out
     */
    @Override
    public boolean isTerminal(State state) {
        PropositionalFunction pf = domain.getPropFunction(PlayerDead.NAME);
        if (pf.somePFGroundingIsTrue(state)) return true;
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        int round = meta.getIntValForAttribute(ROUND_NUM);
        return round > MetaData.ROUND_LIMIT;
    }

    /**
     * Checks to see if we won the match. This function ASSUMES that {@link #isTerminal} returns true. If it's not the
     * case, it returns the current leader
     * @param endState - the final state
     * @return true if we won the match. For the purposes of tie breaking, player 1 (which might not be us) wins in the
     * event of ties
     */
    public boolean weAreWinners(State endState) {
        ObjectInstance ship0 = endState.getObject(SHIP_CLASS + "0");
        ObjectInstance ship1 = endState.getObject(SHIP_CLASS + "1");
        ObjectInstance meta = endState.getObject("MetaData");
        int lives0 = ship0.getIntValForAttribute(LIVES);
        int lives1 = ship1.getIntValForAttribute(LIVES);
        int kills0 = ship0.getIntValForAttribute(KILLS);
        int kills1 = ship1.getIntValForAttribute(KILLS);
        int dk = kills0 - kills1;
        int pNum = meta.getIntValForAttribute(ACTUAL_PNUM);
        // Case 1: Both players are dead
        if (lives0 < 0 && lives1 < 0) {
            //We win if: we have more kills, or the same number and we're player 1
            return (dk > 0) || (dk == 0 && pNum == 0);
        }
        // Case 2: We're alive, but opponent is dead
        if ( lives1 < 0 ) return true;
        // Case 3: We're dead
        if ( lives0 < 0 ) return false;
        //Case 4: No-one is dead, so we have more kills, or the same and we're player 1
        return (dk > 0 || (dk == 0 && pNum == 0));
    }
}
