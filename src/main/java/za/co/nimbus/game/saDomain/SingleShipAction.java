package za.co.nimbus.game.saDomain;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.constants.ObjectClasses;

import java.util.List;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;

/**
 * Defines transition dynamics based on the actions taken by the controlling player's ship
 */
public class SingleShipAction extends Action {

    private final Integer seed;
    private ObjectInstance mMyShip = null;
    private int playerNumber = -1;

    public SingleShipAction(String name, Domain domain, Integer seed) {
        super(name, domain, "");
        this.seed = seed;
    }

    /**
     * Carry out the action and return the resulting state
     * @param s the state in which the action is to be performed.
     * @param params a String array specifying the action object parameters
     * @return the state that resulted from applying this action
     */
    @Override
    protected State performActionHelper(State s, String[] params) {
        return null;
    }

    /**
     * Determines if an action is allowed in this state. (e.g. disallow moving left when at the left wall)
     *
     * @param state  the state to check the validity of the action
     * @param params a String array specifying the action object parameters
     * @return whether the action can be performed on the given state
     */
    @Override
    public boolean applicableInState(State state, String[] params) {
        String action = this.getName();
        // You can always do Nothing
        if (action.equals(Nothing)) return true;
        ObjectInstance ship = getMyShip(state);
        int respawning = ship.getIntValForAttribute(RESPAWN_TIME);
        //If you're dead, you can only do Nothing
        if (respawning >= 0) return false;
        int lives = ship.getIntValForAttribute(LIVES);
        int x = ship.getIntValForAttribute(X);

        int alienFactory = ship.getIntValForAttribute(ALIEN_FACTORY);
        boolean hasAlienFactory = alienFactory >= 0;
        int missileController = ship.getIntValForAttribute(MISSILE_CONTROL);
        boolean hasMissileController = missileController >= 0;

        switch (action) {
            case MoveLeft:
                return x > 1;                            //The ship has a width of 3
            case MoveRight:
                return x < (MetaData.MAP_WIDTH - 2);     //The ship has a width of 3
            case Shoot:
                int missiles = ship.getIntValForAttribute(MISSILE_COUNT);
                return hasMissileController ? missiles <= 1 : missiles == 0;
            case BuildAlienFactory:
                return (lives > 0) && (!hasAlienFactory) && (!hasMissileController || Math.abs(x - missileController) > 2);
            case BuildMissileController:
                return (lives > 0) && (!hasMissileController) && (!hasAlienFactory || Math.abs(x - alienFactory) > 2);
            case BuildShield:
                return (lives > 0);
            default:
                return true;
        }
    }

    private ObjectInstance getMyShip(State state) {
        if (mMyShip != null) return mMyShip;
        ObjectInstance ship = state.getObject(ObjectClasses.SHIP_CLASS + "0");
        if (ship == null) throw new IllegalStateException("Could not find my ship object");
        mMyShip = ship;
        return ship;
    }

    /**
     * The model is largely deterministic, but the aliens firing is the single source of randomness, so that is captured
     * here. The opponent ship actions are also assumed to be deterministic since they follow an OpponentShipPolicy
     * strategy
     * @param s the state from which the transition probabilities when applying this action will be returned.
     * @param params a String array specifying the action object parameters
     * @return a List of transition probabilities for applying this action in the given state with the given set of parameters
     */
    @Override
    public List<TransitionProbability> getTransitions(State s, String[] params) {
        //If aliens will shoot  TODO
        //Return list of transitions
        //else
        return deterministicTransition(s, params);
    }

    public int getPlayerNumber(State s) {
        if (playerNumber >= 0) return playerNumber;
        int pNum = getMyShip(s).getIntValForAttribute(PNUM);
        playerNumber = pNum;
        return pNum;
    }
}
