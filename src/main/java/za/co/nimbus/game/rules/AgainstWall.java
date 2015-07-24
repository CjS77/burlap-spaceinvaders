package za.co.nimbus.game.rules;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;

import static za.co.nimbus.game.constants.ObjectClasses.ALIEN_CLASS;

/**
 * Trigger function for detecting moving into walls
 */
public class AgainstWall extends PropositionalFunction {
    public static final String NAME = "AgainstWall";
    public AgainstWall(Domain domain) {
        super(NAME, domain, new String[] {ALIEN_CLASS});
    }
    @Override
    public boolean isTrue(State state, String[] objects) {
        ObjectInstance alien = state.getObject(objects[0]);
        Location loc = Location.getObjectLocation(alien);
        return (loc.x == 0 || loc.x == MetaData.MAP_WIDTH - 1);
    }
}
