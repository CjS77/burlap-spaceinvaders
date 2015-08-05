package za.co.nimbus.game.saDomain;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.constants.ObjectClasses;
import za.co.nimbus.game.rules.SpaceInvaderMechanics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.ALIEN_CLASS;
import static za.co.nimbus.game.constants.ObjectClasses.META_CLASS;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;

/**
 * Defines transition dynamics based on the actions taken by the controlling player's ship
 */
public class SingleShipAction extends Action {

    private final OpponentStrategy opponentStrategy;
    private int playerNumber = -1;

    public SingleShipAction(String name, Domain domain, OpponentStrategy opponent) {
        super(name, domain, "");
        this.opponentStrategy = opponent;
    }

    /**
     * Carry out the action and return the resulting state - here an actual dice is rolled to determine which alien
     * fires (if applicable)
     * @param s the state in which the action is to be performed.
     * @param params a String array specifying the action object parameters
     * @return the state that resulted from applying this action
     */
    @Override
    protected State performActionHelper(State s, String[] params) {
        String oppMove = getOpponentMove(s);
        return SpaceInvaderMechanics.advanceGameByOneRound(getDomain(), s, this.getName(), oppMove);
    }

    /**
     * Utility method that returns the current opponent move (guaranteed to be valid) as a string, for the given state
     */
    public String getOpponentMove(State s) {
        State flipped = StateFlipper.flipState(s);
        Action oppAction = opponentStrategy.getValidMove(flipped);
        String flippedMove = oppAction== null? Nothing : StateFlipper.flipMove(oppAction.getName());
        return flippedMove;
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
        ObjectInstance ship = state.getObject(ObjectClasses.SHIP_CLASS + "0");
        if (ship == null) throw new IllegalStateException("Could not find my ship object");
        return ship;
    }

    /**
     * Return all possible transition states (only returns more than one when aliens fire, in which case n0xn1
     * TransitionProbabilities are returned for each set of aliens in the first 2 rows for each player)
     * @param s the state from which the transition probabilities when applying this action will be returned.
     * @param params a String array specifying the action object parameters
     * @return a List of transition probabilities for applying this action in the given state with the given set of parameters
     */
    @Override
    public List<TransitionProbability> getTransitions(State s, String[] params) {
        // Set up work variables for this function
        List<TransitionProbability> transitions = new ArrayList<>();
        ObjectInstance[] ships = new ObjectInstance[2];
        Set<ObjectInstance> deadEntities = new HashSet<>();
        ships[0] = s.getObject(SHIP_CLASS + "0");
        ships[1] = s.getObject(SHIP_CLASS + "1");
        String myMove = this.getName();
        String oppMove = getOpponentMove(s);
        State preFireState;
        Domain d = getDomain();
        // Carry out common events
        preFireState = SpaceInvaderMechanics.updateEnvironmentPreAlienShoot(d, s, deadEntities);
        boolean[] aliensFire = new boolean[] {
            SpaceInvaderMechanics.aliensWillFire(ships[0]),
            SpaceInvaderMechanics.aliensWillFire(ships[1])
        };
        if (!(aliensFire[0] || aliensFire[1])) {
            s = SpaceInvaderMechanics.updateEnvironmentPostAlienShoot(d, preFireState, myMove, oppMove, deadEntities);
            transitions.add(new TransitionProbability(s, 1.0));
        } else {
            List<ObjectInstance> aliens = preFireState.getObjectsOfClass(ALIEN_CLASS);
            ObjectInstance sniper0 = aliensFire[0]? SpaceInvaderMechanics.getSniper(0, aliens, ships[0]) : null;
            ObjectInstance sniper1 = aliensFire[1]? SpaceInvaderMechanics.getSniper(1, aliens, ships[1]) : null;
            List<ObjectInstance> eligible0 = SpaceInvaderMechanics.getAliensFromFirstTwoWaves(0, aliens, null);
            List<ObjectInstance> eligible1 = SpaceInvaderMechanics.getAliensFromFirstTwoWaves(1, aliens, null);
            State newState;
            List<TransitionProbability> p0Transitions = new ArrayList<>(8);
            //The alien mechanics are independent for each player, so they can be handled separately
            if (eligible0.size()==1) {
                //67% of time there is no shot
                p0Transitions.add(new TransitionProbability(preFireState.copy(), 2.0/3.0));
                //33% of time the sniper shoots
                newState = preFireState.copy();
                SpaceInvaderMechanics.alienShoots(d, newState, 0, sniper0);
                adjustShotEnergy(ships[0]);
                p0Transitions.add(new TransitionProbability(newState, 1.0/3.0));
            } else {
                for (ObjectInstance alien0 : eligible0) {
                    double p0 = alien0 == sniper0 ? 1.0 / 3.0 : 2.0 / (3.0 * (eligible0.size() - 1));
                    newState = preFireState.copy();
                    SpaceInvaderMechanics.alienShoots(d, newState, 0, alien0);
                    adjustShotEnergy(ships[0]);
                    p0Transitions.add(new TransitionProbability(newState, p0));
                }
            }
            //For each of those probabilities, add a set for player 2 with updated states and total probabilities
            for (TransitionProbability p0Transition : p0Transitions) {
                State p0State = p0Transition.s.copy();
                double p0 = p0Transition.p;
                if (eligible1.size()==1) {
                    //67% of time there is no shot
                    transitions.add(new TransitionProbability(p0State, p0*2.0/3.0));
                    //33% of time the sniper shoots
                    newState = p0State.copy();
                    SpaceInvaderMechanics.alienShoots(d, newState, 1, sniper1);
                    adjustShotEnergy(ships[1]);
                    transitions.add(new TransitionProbability(newState, p0*1.0/3.0));
                } else {
                    for (ObjectInstance alien1 : eligible1) {
                        double p1 = alien1 == sniper1 ? 1.0 / 3.0 : 2.0 / (3.0 * (eligible1.size() - 1));
                        newState = p0State.copy();
                        SpaceInvaderMechanics.alienShoots(d, newState, 1, alien1);
                        adjustShotEnergy(ships[1]);
                        transitions.add(new TransitionProbability(newState, p0*p1));
                    }
                }
            }

        }
        return transitions;
    }

    private void adjustShotEnergy(ObjectInstance ship) {
        int shotEnergy = ship.getIntValForAttribute(ALIEN_SHOT_ENERGY);
        ship.setValue(ALIEN_SHOT_ENERGY, shotEnergy - MetaData.SHOT_COST);
    }


    public int getPlayerNumber(State s) {
        if (playerNumber >= 0) return playerNumber;
        int pNum = getMyShip(s).getIntValForAttribute(PNUM);
        playerNumber = pNum;
        return pNum;
    }


}
