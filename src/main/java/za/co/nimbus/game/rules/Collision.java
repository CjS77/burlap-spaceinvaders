package za.co.nimbus.game.rules;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import za.co.nimbus.game.constants.ObjectClasses;
import za.co.nimbus.game.helpers.Location;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.ObjectClasses.MISSILE_CLASS;

/**
 * Define the rules for when objects collide
 */
public class Collision extends PropositionalFunction {
    public static final String NAME = "Collision";

    public Collision(Domain domain, String[] parameterClasses) {
        super(parameterClasses[0]+parameterClasses[1]+NAME, domain, parameterClasses, parameterClasses, NAME);
    }

    @Override
    public boolean isTrue(State state, String[] objects) {
        ObjectInstance objA = state.getObject(objects[0]);
        ObjectInstance objB = state.getObject(objects[1]);

        Location locA, locB;
        locA = new Location(objA.getIntValForAttribute(X), objA.getIntValForAttribute(Y));
        locB = new Location(objB.getIntValForAttribute(X), objB.getIntValForAttribute(Y));
        int Aw = objA.getIntValForAttribute(WIDTH);
        int Bw = objB.getIntValForAttribute(WIDTH);
        //Special case -- missiles 'passing through' each other must collide
        if (getName().equals(MISSILE_CLASS + MISSILE_CLASS + NAME)) {
            int p0 = objA.getIntValForAttribute(PNUM);
            int p1 = objB.getIntValForAttribute(PNUM);
            if ( (locA.x != locB.x) || (p0 == p1)) return false;
            return (p0 == 0 && locA.y >= locB.y) || (p0==1 && locA.y <= locB.y);
        }
        return ( locA.y == locB.y) && (Math.abs(locA.x - locB.x) < (Aw/2 + Bw/2 + 1) );
    }
}
