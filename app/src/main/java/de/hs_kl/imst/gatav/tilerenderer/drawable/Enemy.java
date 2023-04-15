package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;




import de.hs_kl.imst.gatav.tilerenderer.util.Direction;

public class Enemy extends MovableTileGraphics {
    private int curI = 0;

    private int[] corXY;
    private int score = -25;
    private int currentIndex = 1;
    private List<int[]>route = new ArrayList<int[]>(4);

    public int getScore() {
        return score;
    }

    public Enemy(int x, int y, InputStream is) { // Receives an Inputstream
        super(x, y, is);
        tilePaint.setColor(Color.parseColor("#FF33CC"));
    }

    /*
        Move Methode überträgt Route
     */

    public void move(List<int[]> route_move) {
        route = route_move;
        corXY = route.get(currentIndex); // Hole Element aus Liste
        move(corXY[0], corXY[1]); // 13,3
        }

    /*
        Das Enemey-Objekt wird hier in eine andere Richtung gelenkt, bei dem Erreichen eines Eckpunktes der Route.
     */
    @Override
    public void update(float fracsec) {
        super.update(fracsec);
        if(currentDirection == Direction.IDLE){
            Matrix m = new Matrix();
            m.preScale(-1.0f, 1.0f);
            switch(curI){
                case 0:
                    curI++;
                    tileBitmap = Bitmap.createBitmap(tileBitmap, 0, 0, tileBitmap.getWidth(), tileBitmap.getHeight(), m, true);
                    break;
                case 1:
                    curI++;
                    break;
                case 2:
                    curI++;
                    tileBitmap = Bitmap.createBitmap(tileBitmap, 0, 0, tileBitmap.getWidth(), tileBitmap.getHeight(), m, true);
                    break;
                case 3:
                    curI = 0;
            }
        }

        if(currentDirection == Direction.IDLE && route.size() - 1 > currentIndex){ // Tuscan am Ziel angekommen, Abfrage läuft solange bis die Route am Ende ist
                currentIndex++;
                corXY = route.get(currentIndex);
                move(corXY[0], corXY[1]); // 13,5
            }
    }

    public boolean isPassable () {
        return true;
    }

    @Override
    public int getX() {
        return (int) currentX;
    }

    @Override
    public int getY() {
      return (int) currentY;
    }

}




