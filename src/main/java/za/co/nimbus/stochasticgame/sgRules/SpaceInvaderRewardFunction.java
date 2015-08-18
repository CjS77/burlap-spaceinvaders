package za.co.nimbus.stochasticgame.sgRules;

import burlap.oomdp.core.*;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import za.co.nimbus.game.constants.Commands;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;
import za.co.nimbus.game.rules.PlayerDead;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.Shoot;
import static za.co.nimbus.game.constants.ObjectClasses.*;

/**
 * The Space Invader reward function
 * The nominal (undiscounted) reward is
 *   1000 for winning a game, 500 for a tie
 *   10 for a kill
 *
 */
public class SpaceInvaderRewardFunction implements JointReward {
    private static final double WIN_REWARD = 1000.0;
    private static final double KILL_REWARD = 10.0;
    private static final double DIED_COST = 100.0;
    private static final double SURVIVAL_REWARD = 1.0;
    private static final double CLEAR_SHOT_REWARD = 2.0;
    private static final double ACCURATE_SHOT_REWARD = 3.0;
    private static final double IN_DANGER_COST = 5.0;
    private final PropositionalFunction playerDead;
    private String[] action = new String[2];

    public SpaceInvaderRewardFunction(Domain d) {
        this.playerDead = d.getPropFunction(PlayerDead.NAME);
    }

    private ObjectInstance[] currentShip = new ObjectInstance[2];
    private ObjectInstance[] prevShip = new ObjectInstance[2];

    /**
     * Prev is the state that resulted in the actions. Current is the state we're entering
     */
    @Override
    public Map<String, Double> reward(State prev, JointAction jointAction, State current) {
        currentShip[0] = current.getObject(SHIP_CLASS+"0");
        currentShip[1] = current.getObject(SHIP_CLASS+"1");
        prevShip[0] = prev.getObject(SHIP_CLASS+"0");
        prevShip[1] = prev.getObject(SHIP_CLASS+"1");
        Map <String, Double> rewards = new HashMap<>();
        assignActionString(jointAction, 0);
        assignActionString(jointAction, 1);
        double[] rw = new double[]{SURVIVAL_REWARD, SURVIVAL_REWARD};
        for (int i=0; i<2; i++) {
            int[] delta = countDeltas(i, current, prev, jointAction);
            rw[i] += delta[0]*KILL_REWARD;
            rw[i] += DIED_COST *(delta[1] + delta[2] + delta[3] + delta[4]);
            switch (action[i]) {
                case Shoot:
                    rw[i] += CLEAR_SHOT_REWARD;
                    if (behindShields(prev, i)) rw[i] += -CLEAR_SHOT_REWARD;
                    int turnsForKill = willHitAlienIfShoot(prev, i);
                    if (turnsForKill >= 0) rw[i] += ACCURATE_SHOT_REWARD/(turnsForKill+1);
                    //drop through
                default:
                    int dc = inLineOfFire(current, i, currentShip[i]);
                    int dp = inLineOfFire(prev, i, prevShip[i]);
                    //Give a reward for reducing the danger level, penalise for increasing it
                    rw[i] += IN_DANGER_COST *(Math.exp(-0.25*dp) - Math.exp(-0.25*dc));
            }
            rewards.put(SHIP_CLASS + i, rw[i]);
        }
        int winner = getWinner(current);
        if (winner >= 0) rewards.put(SHIP_CLASS + winner, WIN_REWARD);
        return rewards;
    }

    private void assignActionString(JointAction jointAction, int pnum) {
        GroundedSingleAction action = jointAction.action(SHIP_CLASS + pnum);
        if (action != null) this.action[pnum] = action.justActionString();
        else this.action[pnum] = Commands.Nothing;
    }

    int willHitAlienIfShoot(State s, int pnum) {
        if (behindShields(s, pnum)) return -1;
        Location ship = Location.getObjectLocation(prevShip[pnum]);
        List<ObjectInstance> aliens = s.getObjectsOfClass(ALIEN_CLASS);
        for (ObjectInstance alien : aliens) {
            int ap = alien.getIntValForAttribute(PNUM);
            if (pnum != ap) {
                Location l = Location.getObjectLocation(alien);
                int j = pnum == 0? 10 - l.y : 20-l.y;
                if (j > 8) j = 8;
                int sx = targetMap[pnum][j][l.x];
                if (sx == ship.x) return Math.abs(ship.y - l.y);
            }
        }
        return -1;
    }

    int inLineOfFire(State s, int pnum, ObjectInstance ship) {
        Location loc = Location.getObjectLocation(ship);
        int result = checkForLineOfFire(s, pnum, loc, MISSILE_CLASS, 100);
        return checkForLineOfFire(s, pnum, loc, BULLET_CLASS, result);
    }

    private int checkForLineOfFire(State s, int pnum, Location loc, String aClass, int d0) {
        int result = d0;
        List<ObjectInstance> missiles = s.getObjectsOfClass(aClass);
        for (ObjectInstance missile : missiles) {
            if (missile.getIntValForAttribute(PNUM) != pnum) {
                Location mloc = Location.getObjectLocation(missile);
                if (Math.abs(mloc.x - loc.x) < 2) {
                    int dy = Math.abs(loc.y - mloc.y);
                    if (dy < result) result = dy;
                }
            }
        }
        return result;
    }

    private boolean behindShields(State s, int pnum) {
        List<ObjectInstance> shields = s.getObjectsOfClass(SHIELD_CLASS);
        Location l = Location.getObjectLocation(currentShip[pnum]);
        for (ObjectInstance shield : shields) {
            int sx = shield.getIntValForAttribute(X);
            if ((sx == l.x) && Math.abs(l.y - shield.getIntValForAttribute(Y)) < 5) return true;
        }
        return false;
    }

    /**
     * 0 - kills
     * 1 - lives
     * 2 - missilecontroller
     * 3 - alienfactories
     * 4 - builtshields
     */
    private int[] countDeltas(int pnum, State current, State prev, JointAction ja) {
        ObjectInstance cship = currentShip[pnum];
        ObjectInstance pship = prevShip[pnum];
        int[] result = new int[5];
        result[0] = cship.getIntValForAttribute(KILLS) - pship.getIntValForAttribute(KILLS);
        result[1] = cship.getIntValForAttribute(LIVES) - pship.getIntValForAttribute(LIVES);
        result[2] = (cship.getIntValForAttribute(MISSILE_CONTROL) >= 0 ? 1 : 0) - (pship.getIntValForAttribute(MISSILE_CONTROL) >= 0? 1 : 0);
        result[3] = (cship.getIntValForAttribute(ALIEN_FACTORY) >= 0 ? 1 : 0) - (pship.getIntValForAttribute(ALIEN_FACTORY) >= 0? 1 : 0);
        result[4] = 0;
        GroundedSingleAction action = ja.action(SHIP_CLASS + pnum);
        if (action != null) {
            result[4] = action.justActionString().equals(Commands.BuildShield)? 1 : 0;
        }
        return result;
    }



    /**
     * @return -1 for Game in progress
     *          0 for player 0
     *          1 for player 1
     */
    public int getWinner(State state) {
        boolean[] dead = new boolean[]{ false, false};
        for (GroundedProp pf : playerDead.getAllGroundedPropsForState(state)) {
            boolean isDead = pf.isTrue(state);
            if (isDead) {
                String ls = pf.params[0];
                dead[(int) ls.charAt(ls.length() - 1) - 48] = true;
            }
        }
        if (dead[0] && !dead[1]) return 1;
        if (!dead[0] && dead[1]) return 0;
        //Both players are alive and the game isn't over yet
        ObjectInstance meta = state.getFirstObjectOfClass(META_CLASS);
        int numRounds = meta.getIntValForAttribute(ROUND_NUM);
        if (numRounds < MetaData.ROUND_LIMIT && !dead[0]) return -1;
        //Both players are dead, or we've hit the round limit, either way
        //the most kills wins. Player 1 wins in case of tie
        int kills0 = state.getObject(SHIP_CLASS + "0").getIntValForAttribute(KILLS);
        int kills1 = state.getObject(SHIP_CLASS + "1").getIntValForAttribute(KILLS);
        return kills0 < kills1? 1 : 0;
    }

    public static final int[][][] targetMap = new int[][][]{
            {   {6, 5, 4, 3, 2, 1, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8},
                {7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 15, 14, 13, 12, 11},
                {4, 3, 2, 1, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 15, 14, 13},
                {2, 1, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12},
                {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 15},
                {0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
                {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16},
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}
            },{
                {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16},
                {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,16},
                {0,0,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14},
                {3,4,5,6,7,8,9,10,11,12,13,14,15,16,16,16,15},
                {2,1,0,0,0,1,2,3,4,5,6,7,8,9,10,11,12},
                {5,6,7,8,9,10,11,12,13,14,15,16,16,16,15,14,13},
                {4,3,2,1,0,0,0,1,2,3,4,5,6,7,8,9,10},
                {7,8,9,10,11,12,13,14,15,16,16,16,15,14,13,12,11},
                {6,5,4,3,2,1,0,0,0,1,2,3,4,5,6,7,8}
            }};
}
