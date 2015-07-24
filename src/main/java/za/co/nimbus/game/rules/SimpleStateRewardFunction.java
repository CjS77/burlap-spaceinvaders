package za.co.nimbus.game.rules;

import burlap.oomdp.core.*;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import za.co.nimbus.game.constants.Commands;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * The Space Invader reward function
 * The nominal (undiscounted) reward is
 *   1000 for winning a game, 500 for a tie
 *   10 for a kill
 *
 */
public class SimpleStateRewardFunction implements JointReward {
    private static final double WIN_REWARD = 1000.0;
    private static final double KILL_REWARD = 10.0;
    private static final double DIED_COST = 100.0;
    private static final double SURVIVAL_REWARD = 1.0;
    private static final double CLEAR_SHOT_REWARD = 2.0;
    private static final double BAD_MOVE_COST = 1.0;
    private static final double IN_DANGER_COST = 5.0;
    private String[] action = new String[2];

    public SimpleStateRewardFunction(Domain d) {

    }

    /**
     * Prev is the state that resulted in the actions. Current is the state we're entering
     */
    @Override
    public Map<String, Double> reward(State prevState, JointAction jointAction, State currentState) {
        ObjectInstance curr = currentState.getFirstObjectOfClass(SIMPLE_STATE_CLASS);
        ObjectInstance prev = prevState.getFirstObjectOfClass(SIMPLE_STATE_CLASS);
        Map <String, Double> rewards = new HashMap<>();
        assignActionString(jointAction, 0);
        assignActionString(jointAction, 1);
        double rw = SURVIVAL_REWARD;
        int dangerLevelP = prev.getIntValForAttribute(DANGER_LEVEL);
        int dangerLevelC = curr.getIntValForAttribute(DANGER_LEVEL);
        //Give a reward for reducing the danger level, penalise for increasing it
        rw += IN_DANGER_COST *(Math.exp(-0.25*dangerLevelP) - Math.exp(-0.25*dangerLevelC));
        int[] deltaLives = getDeltaLives(curr, prev);
        int buildDelta = (
                action[1].equals(BuildMissileController) ||
                action[1].equals(BuildShield) ||
                action[1].equals(BuildAlienFactory) )? 1 : 0;
        if (deltaLives[1] - buildDelta > 0 ) rw += KILL_REWARD;
        if (deltaLives[0] > 0) rw -= DIED_COST;
        rewards.put(SHIP_CLASS + 0, rw);
        rewards.put(SHIP_CLASS + 1, SURVIVAL_REWARD);
        int winner = getWinner(curr, prev);
        if (winner == 0) rw += WIN_REWARD;
        if (winner == 1) rw -= WIN_REWARD;
        if (winner == 2) rw -= WIN_REWARD/2;
        rewards.put(SHIP_CLASS + 0, rw);
        rewards.put(SHIP_CLASS + 1, SURVIVAL_REWARD);
        return rewards;
    }

    /**
     * Returns 0 or 1 for P1 or P2 winning
     * 2 for a tie
     * -1 for no result yet
     */
    private int getWinner(ObjectInstance curr, ObjectInstance prev) {
        int p1 = curr.getIntValForAttribute(P1_LIVES);
        int p2 = curr.getIntValForAttribute(P2_LIVES);
        if (p1 >= 0 && p2 >= 0) return -1;
        if (p1 < 0 && p2 < 0) return 2;
        if (p1 < 0) return 1;
        else return 0;
    }

    private int[] getDeltaLives(ObjectInstance curr, ObjectInstance prev) {
        return new int[]{
            prev.getIntValForAttribute(P1_LIVES) - curr.getIntValForAttribute(P1_LIVES),
            prev.getIntValForAttribute(P2_LIVES) - curr.getIntValForAttribute(P2_LIVES)
        };
    }

    private void assignActionString(JointAction jointAction, int pnum) {
        GroundedSingleAction action = jointAction.action(SHIP_CLASS + pnum);
        if (action != null) this.action[pnum] = action.justActionString();
        else this.action[pnum] = Commands.Nothing;
    }




}
