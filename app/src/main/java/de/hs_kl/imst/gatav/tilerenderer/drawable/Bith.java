package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hs_kl.imst.gatav.tilerenderer.util.Direction;

public class Bith extends MovableTileGraphics {

    private int[] corXY;
    private int score = 50;
    private int currentIndex = 1;
    private List<int[]> route = new ArrayList<int[]>(3);

    public Bith(int x, int y, InputStream is) {
        super(x, y, is);
        tilePaint.setColor(Color.parseColor("#FF33CC"));
    }

    public void move(List<int[]> route_move) {
        route = route_move;
        corXY = route.get(currentIndex);
        move(corXY[0], corXY[1]);
    }


/**
    Update-Methode verwandelt das Bith-Objekt in einen kleineren Zustand, als auch ändert die Richtung bei dem Erreichen des ersten Ziels der Route.
 */

    public void update(float fracsec) {
        super.update(fracsec);

        this.x = (int) currentX;
        this.y = (int) currentY;

        if (currentDirection == Direction.IDLE){
            Matrix matrix = new Matrix();
            float scalewidth = ((float)tileBitmap.getWidth()) / 225;
            float scaleheight = ((float)tileBitmap.getHeight()) / 225;
            matrix.postScale(scalewidth,scaleheight);
            matrix.preScale(-1,1);
            tileBitmap = Bitmap.createBitmap(tileBitmap,0 , 0, tileBitmap.getWidth(), tileBitmap.getHeight(), matrix, true);
        }

        if(currentDirection == Direction.IDLE && route.size() - 1 > currentIndex){ // Tuscan am Ziel angekommen, Abfrage läuft solange bis die Route am Ende ist
            currentIndex++;
            corXY = route.get(currentIndex);
            move(corXY[0], corXY[1]);
        }
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public int getScore() {
        return score;
    }
}
