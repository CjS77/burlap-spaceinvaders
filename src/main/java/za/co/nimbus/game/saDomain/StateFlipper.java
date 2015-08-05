package za.co.nimbus.game.saDomain;

import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import za.co.nimbus.game.constants.MetaData;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.Commands.*;
import static za.co.nimbus.game.constants.ObjectClasses.SHIP_CLASS;
/**
 * Utility class that flips a state representation so that a State looks like player 0 perspective
 */
public class StateFlipper {

    public static State flipState(State s) {
        State flipped = s.copy();
        ObjectInstance[] ship = new ObjectInstance[2];
        for (ObjectInstance o: flipped.getAllObjects()){
            ObjectClass oclass = o.getObjectClass();
            if (o.getName().startsWith(SHIP_CLASS)) {
                //Remove and add it so that the mapping to its new name gets updated
                flipped.removeObject(o);
                int newPNum = 1 - o.getIntValForAttribute(PNUM);
                o.setName(SHIP_CLASS + newPNum);
                flipBuildingMarker(o, MISSILE_CONTROL);
                flipBuildingMarker(o, ALIEN_FACTORY);
                o.setValue(DELTA_X, -o.getIntValForAttribute(DELTA_X));
                //Store reference to ship
                ship[newPNum] = o;
            }
            //Switch player number
            if (oclass.hasAttribute(PNUM)) {
                o.setValue(PNUM, 1 - o.getIntValForAttribute(PNUM));
            }
            //Switch actual player number - this must be done, because I'm switching perspectives, so the actual player number does switch
            if (oclass.hasAttribute(ACTUAL_PNUM)) {
                o.setValue(ACTUAL_PNUM, 1 - o.getIntValForAttribute(ACTUAL_PNUM));
            }
            //Change X coordinate
            if (oclass.hasAttribute(X)) {
                int x = o.getIntValForAttribute(X);
                //Don't muck with objects 'off the map'
                if (x >= 0) o.setValue(X, MetaData.MAP_WIDTH - x - 1);
            }
            //Change Y coordinate
            if (oclass.hasAttribute(Y)) {
                o.setValue(Y, MetaData.MAP_HEIGHT - o.getIntValForAttribute(Y) - 1);
            }
        }
        //Add ships back with new name
        flipped.addObject(ship[0]);
        flipped.addObject(ship[1]);
        return flipped;
    }

    private static void flipBuildingMarker(ObjectInstance o, String buildingAttr) {
        int x = o.getIntValForAttribute(buildingAttr);
        if (x >= 0) {
            o.setValue(buildingAttr, MetaData.MAP_WIDTH - x - 1);
        }
    }

    public static String flipMove(String name) {
        return name;
//        switch (name) {
//            case MoveLeft:
//                return MoveRight;
//            case MoveRight:
//                return MoveLeft;
//            default:
//                return name;
//        }
    }
}
