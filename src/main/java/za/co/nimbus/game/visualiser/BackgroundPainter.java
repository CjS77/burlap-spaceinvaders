package za.co.nimbus.game.visualiser;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.StaticPainter;
import za.co.nimbus.game.constants.ObjectClasses;
import static za.co.nimbus.game.constants.Attributes.*;
import java.awt.*;

public class BackgroundPainter implements StaticPainter {
    @Override
    public void paint(Graphics2D g2, State state, float w, float h) {
        g2.setBackground(Color.BLACK);
        g2.clearRect(0, 0, (int) w, (int) h);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(0, 0, (int) w, (int) h);
        drawStats(g2, state, 0, 0, (int)h/2 - 26);
        drawStats(g2, state, 1, (int)w - 50, (int) h / 2 - 26);
    }

    private void drawStats(Graphics2D g2, State state, int pnum, int x, int y) {
        ObjectInstance ship = state.getObject(ObjectClasses.SHIP_CLASS + pnum);
        int kills = ship.getIntValForAttribute(KILLS);
        int lives = ship.getIntValForAttribute(LIVES);
        g2.drawString("Kills: " + kills, x, y);
        g2.drawString("Lives: " + lives, x, y+13);
    }
}