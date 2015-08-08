package za.co.nimbus.game.visualiser;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.StaticPainter;
import za.co.nimbus.game.constants.MetaData;
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
        //draw grid
        g2.setColor(Color.LIGHT_GRAY);
        for (int j=1; j < MetaData.MAP_HEIGHT; j++) {
            g2.drawLine(0, j*26, (int) w, j*26);
        }
        for (int i=1; i< MetaData.MAP_WIDTH; i++) {
            g2.drawLine(i*26, 0, i*26, (int) h);
        }
    }

    private void drawStats(Graphics2D g2, State state, int pnum, int x, int y) {
        ObjectInstance ship = state.getObject(ObjectClasses.SHIP_CLASS + pnum);
        if (ship == null) return;
        int kills = ship.getIntValForAttribute(KILLS);
        int lives = ship.getIntValForAttribute(LIVES);
        g2.drawString("Kills: " + kills, x, y);
        g2.drawString("Lives: " + lives, x, y+13);
    }
}
