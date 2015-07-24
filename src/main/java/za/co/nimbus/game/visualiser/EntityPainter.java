package za.co.nimbus.game.visualiser;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import za.co.nimbus.game.constants.MetaData;
import za.co.nimbus.game.helpers.Location;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static za.co.nimbus.game.constants.Attributes.*;
import static za.co.nimbus.game.constants.ObjectClasses.*;

public class EntityPainter implements ObjectPainter {
    private final BufferedImage ship[] = new BufferedImage[2];
    private final BufferedImage alien[] = new BufferedImage[2];

    public EntityPainter() {
        try {
            ship[0] = ImageIO.read(getClass().getClassLoader().getResourceAsStream("ship.png"));
            // Flip the image vertically
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -ship[0].getHeight(null));
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            ship[1] = op.filter(ship[0], null);
        } catch (IOException e) {
            throw new IllegalStateException("Ship image not found");
        }
        try {
            alien[0] = ImageIO.read(getClass().getClassLoader().getResourceAsStream("alien1.png"));
            alien[1] = ImageIO.read(getClass().getClassLoader().getResourceAsStream("alien2.png"));
            // Flip the image vertically
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -alien[1].getHeight(null));
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            alien[1] = op.filter(alien[1], null);
        } catch (IOException e) {
            throw new IllegalStateException("Alien image not found");
        }
    }

    @Override
    public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float canvasW, float canvasH) {
        String obJType = ob.getTrueClassName();
        int pnum = -1;
        if (ob.getObjectClass().hasAttribute(PNUM)) {
            pnum = ob.getIntValForAttribute(PNUM);
        }
        switch (obJType) {
            case MISSILE_CLASS:
                paintMissile(g2, getEntityLocation(ob, canvasW, canvasH), pnum);
                break;
            case BULLET_CLASS:
                paintBullet(g2, getEntityLocation(ob, canvasW, canvasH));
                break;
            case SHIP_CLASS:
                paintShip(g2, getEntityLocation(ob, canvasW, canvasH), pnum);
                break;
            case ALIEN_CLASS:
                paintAlien(g2, getEntityLocation(ob, canvasW, canvasH), pnum);
                break;
            case SHIELD_CLASS:
                paintShield(g2, getEntityLocation(ob, canvasW, canvasH));
                break;
            case ALIEN_FACTORY_CLASS:
                paintBuilding(g2, getEntityLocation(ob, canvasW, canvasH), Color.GRAY);
                break;
            case MISSILE_CONTROLLER_CLASS:
                paintBuilding(g2, getEntityLocation(ob, canvasW, canvasH), Color.GREEN);
                break;


        }

    }

    Location getEntityLocation(ObjectInstance ob, float canvasW, float canvasH) {
        float cellW = canvasW / MetaData.MAP_WIDTH;
        float cellH = canvasH / MetaData.MAP_HEIGHT;

        int w = ob.getIntValForAttribute(WIDTH);
        int x = ob.getIntValForAttribute(X);
        int y = ob.getIntValForAttribute(Y);
        int cx = (int) ( (x - w/2) * cellW);
        int cy = (int) (canvasH - ( (y+1) * cellH));
        return new Location(cx, cy);
    }
    


    private void paintBuilding(Graphics2D g2, Location loc, Color color) {
        g2.setColor(color);
        g2.fillRoundRect(loc.x, loc.y +2, 3*26, 24, 4, 4);
    }

    private void paintShield(Graphics2D g2, Location loc) {
        g2.setColor(Color.cyan);
        g2.fillRect(loc.x + 1, loc.y + 8, 24, 12);
    }

    private void paintAlien(Graphics2D g2, Location loc, int pnum) {
        g2.drawImage(alien[pnum], loc.x, loc.y, alien[pnum].getWidth(), alien[pnum].getHeight(), null);
    }

    private void paintShip(Graphics2D g2, Location loc, int pnum) {
        g2.drawImage(ship[pnum], loc.x, loc.y, ship[pnum].getWidth(), ship[pnum].getHeight(), null);
    }

    private void paintBullet(Graphics2D g2, Location loc) {
        g2.setColor(Color.RED);
        g2.fillRect(loc.x + 12, loc.y + 3, 3, 20);
    }

    private void paintMissile(Graphics2D g2, Location loc, int pnum) {
        g2.setColor(pnum == 0? new Color(1f, 0.4f, 0f) : Color.YELLOW);
        g2.fillOval(loc.x + 7, loc.y + 7, 15, 15);
    }
}
