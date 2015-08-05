package za.co.nimbus.game.saDomain.opponents;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import za.co.nimbus.game.heuristics.GreedyVFABasedPolicy;
import za.co.nimbus.game.heuristics.PersistentLinearVFA;
import za.co.nimbus.game.heuristics.SpaceInvaderFeatures;
import za.co.nimbus.game.saDomain.*;

import static za.co.nimbus.game.constants.Commands.*;
/**
 * Uses a VFA learnt from Sarsa-λ algorithm
 */
public final class Hawking extends AbstractOpponent {

    private final int actualPNum;
    private final String vfaCoeffFile;
    private Policy policy;

    public Hawking(Domain d, int actualPNum, String vfaCoeffFile) {
        super(d);
        this.actualPNum = actualPNum;
        this.vfaCoeffFile = vfaCoeffFile;
    }

    @Override
    public Action getProposedMove(State s) {
        State flipped = StateFlipper.flipState(s);
        AbstractGroundedAction action = policy.getAction(flipped);
        return flipMove(action.actionName());
    }

    private Action flipMove(String action) {
        String flippedAction;
        switch (action) {
            case MoveLeft:
                flippedAction = MoveRight;
                break;
            case MoveRight:
                flippedAction = MoveLeft;
                break;
            default:
                flippedAction = action;
        }
        return domain.getAction(flippedAction);
    }

    @Override
    public void init(SpaceInvaderSingleAgentDomainFactory spaceInvaderDomainFactory) {
        SpaceInvaderFeatures fd = new SpaceInvaderFeatures();
        PersistentLinearVFA vfa = new PersistentLinearVFA(fd);
        vfa.loadFromFile(vfaCoeffFile);
        policy = new GreedyVFABasedPolicy(vfa, domain);
    }
}
