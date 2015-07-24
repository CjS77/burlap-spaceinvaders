package za.co.nimbus.game.rules;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;

import static za.co.nimbus.game.constants.Attributes.*;

/**
 * Trigger function for detecting moving into walls
 */
public class OffMap extends PropositionalFunction {
    public static final String NAME = "OffMap";

    public OffMap(Domain domain, String entityClass) {
        super(entityClass + NAME, domain, new String[] {entityClass}, NAME);
    }
    @Override
    public boolean isTrue(State state, String[] objects) {
        ObjectInstance ob = state.getObject(objects[0]);
        Location loc;
        int x = ob.getIntValForAttribute(X);
        int y = ob.getIntValForAttribute(Y);
        loc = new Location(x,y);
        return (loc.x < 0 || loc.x >= MetaData.MAP_WIDTH || loc.y < 0 || loc.y >= MetaData.MAP_HEIGHT);
    }
}
