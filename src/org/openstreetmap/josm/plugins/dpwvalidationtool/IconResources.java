package org.openstreetmap.josm.plugins.dpwvalidationtool;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class IconResources {

    private IconResources() {}

    public static Icon getPirateIcon(int size) {
        int w = size;
        int h = size;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0,0,0,0));
            g.fillRect(0,0,w,h);

            // scale helper
            double s = size / 18.0; // base design at 18px

            // skull body
            g.setColor(Color.WHITE);
            int skullW = (int) Math.round(12 * s);
            int skullH = (int) Math.round(10 * s);
            int skullX = (int) Math.round(3 * s);
            int skullY = (int) Math.round(2 * s);
            g.fillOval(skullX, skullY, skullW, skullH);

            // eyes
            g.setColor(Color.BLACK);
            int eyeSize = Math.max(1, (int) Math.round(2 * s));
            g.fillOval((int)Math.round((6*s)), (int)Math.round((5*s)), eyeSize, eyeSize);
            g.fillOval((int)Math.round((10*s)), (int)Math.round((5*s)), eyeSize, eyeSize);

            // eye patch strap
            g.setStroke(new BasicStroke(Math.max(1f, (float)(2.0f * (float)s))));
            g.drawLine((int)Math.round(3*s), (int)Math.round(6*s), (int)Math.round(15*s), (int)Math.round(6*s));

            // mouth
            g.drawArc((int)Math.round(7*s), (int)Math.round(8*s), (int)Math.round(4*s), (int)Math.round(3*s), 0, -180);

            // crossbones (two sets)
            g.setStroke(new BasicStroke(Math.max(1f, (float)(1.2f * (float)s))));
            g.drawLine((int)Math.round(2*s), (int)Math.round(14*s), (int)Math.round(8*s), (int)Math.round(9*s));
            g.drawLine((int)Math.round(8*s), (int)Math.round(14*s), (int)Math.round(2*s), (int)Math.round(9*s));
            g.drawLine((int)Math.round(10*s), (int)Math.round(14*s), (int)Math.round(16*s), (int)Math.round(9*s));
            g.drawLine((int)Math.round(16*s), (int)Math.round(14*s), (int)Math.round(10*s), (int)Math.round(9*s));

        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }
}
