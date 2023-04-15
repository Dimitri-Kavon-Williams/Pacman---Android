package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Color;
import java.io.InputStream;

public class BorgCube extends MovableTileGraphics {

    private int score = -75;
    public int getScore() {return score;}

    public BorgCube(int x, int y, InputStream is) { // receives an inputstream
        super(x, y, is);

        tilePaint.setColor(Color.parseColor("#FF33CC"));
    }

    public boolean isPassable() {
        return true;
    }
}

