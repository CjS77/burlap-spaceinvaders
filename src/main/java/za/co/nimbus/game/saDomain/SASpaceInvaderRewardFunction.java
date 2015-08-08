package za.co.nimbus.game.saDomain;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import za.co.nimbus.game.rules.GameOver;

import static za.co.nimbus.game.constants.Attributes.KILLS;
import static za.co.nimbus.game.constants.Attributes.LIVES;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;
import static za.co.nimbus.game.constants.Commands.BuildShield;
import static za.co.nimbus.game.constants.Commands.BuildMissileController;
import static za.co.nimbus.game.constants.Commands.BuildAlienFactory;

/**
 * The Space Invader reward function
 * The nominal (undiscounted) reward is
 *   1000 for winning a game
 *   10 for a kill
 *   -100 for being killed
 *
 */
public class SASpaceInvaderRewardFunction implements RewardFunction {
    public static final double WIN_REWARD = 100.0;
    public static final double LOSE_REWARD = -100.0;
    public static final double KILL_REWARD = 10.0;
    public static final double DIED_COST = 10.0;
    public static final double SURVIVAL_REWARD = 0.1;
    private final GameOver tf;

    public SASpaceInvaderRewardFunction(GameOver tf) {
        this.tf = tf;
    }

    @Override
    public double reward(State s, GroundedAction a, State s_prime) {
        //Check for end game conditions
        if (tf.isTerminal(s_prime)) {
            return tf.weAreWinners(s_prime)? WIN_REWARD : LOSE_REWARD;
        }
        //Also get points for kills
        int[] delta = countDeltas(s, s_prime, a.actionName());
        double reward = SURVIVAL_REWARD;
        reward += delta[0]*KILL_REWARD;
        reward += delta[1]*DIED_COST;
        return reward;
    }

    /**
     * 0 - kills
     * 1 - deaths
     */
    private int[] countDeltas(State s, State sprime, String action) {
        ObjectInstance ship_t1 = sprime.getObject(SHIP_CLASS + "0");
        ObjectInstance ship_t0 = s.getObject(SHIP_CLASS + "0");
        int[] result = new int[2];
        result[0] = ship_t1.getIntValForAttribute(KILLS) - ship_t0.getIntValForAttribute(KILLS);
        result[1] = ship_t1.getIntValForAttribute(LIVES) - ship_t0.getIntValForAttribute(LIVES);
        //Don't penalise for building MissileController
        if (action.equals(BuildMissileController))
            result[1] ++;
        return result;
    }
}
