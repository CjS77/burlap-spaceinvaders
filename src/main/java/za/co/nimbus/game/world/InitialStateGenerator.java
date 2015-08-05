package za.co.nimbus.game.world;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;

/**
 * Generates initial states of the Space Invader game
 */
public class InitialStateGenerator implements StateGenerator {

    private final Domain d;
    private int actualPNum;

    public InitialStateGenerator(Domain d, int actualPNum) {
        this.d = d;
        this.actualPNum = actualPNum;
    }

    public int getActualPNum() {
        return actualPNum;
    }

    public void setActualPNum(int actualPNum) {
        this.actualPNum = actualPNum;
    }

    @Override
    public State generateState() {
        return DomainDefinition.getInitialState(d, actualPNum);
    }
}
