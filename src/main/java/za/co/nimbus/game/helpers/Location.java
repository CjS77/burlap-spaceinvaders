package za.co.nimbus.game.helpers;

import burlap.oomdp.core.ObjectInstance;
import za.co.nimbus.game.constants.Attributes;
import za.co.nimbus.game.constants.MetaData;

/**
 * An immutable 2-D location helper
 */
public final class Location {
    public final int x;
    public final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Location(Location loc) {
        this(loc.x, loc.y);
    }

    public static Location getObjectLocation(ObjectInstance ob) {
        int x = ob.getIntValForAttribute(Attributes.X);
        int y = ob.getIntValForAttribute(Attributes.Y);
        return new Location(x, y);
    }

}
