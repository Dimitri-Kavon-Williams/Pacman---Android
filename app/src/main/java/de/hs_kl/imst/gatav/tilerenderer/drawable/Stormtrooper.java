package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hs_kl.imst.gatav.tilerenderer.util.Direction;

public class Stormtrooper extends MovableTileGraphics {

    private int[] corXY;
    private int score = -10;
    private int currentIndex = 1;
    private List<int[]>route = new ArrayList<int[]>(2);

    public Stormtrooper(int x, int y, InputStream is) {
        super(x, y, is);
        tilePaint.setColor(Color.parseColor("#FF33CC"));
    }

    @Override
    public boolean isPassable() {
        return false;
    }

    public void move(List<int[]> route_move){
        route = route_move;
        corXY = route.get(currentIndex);
        move(corXY[0],corXY[1]);
    }

    @Override
    public void update(float fracsec){
        super.update(fracsec);
        this.x = (int) currentX;
        this.y = (int) currentY;
        if(currentDirection == Direction.IDLE && route.size() -1 > currentIndex){
            currentIndex++;
            corXY = route.get(currentIndex);
            move(corXY[0], corXY[1]);
        }
    }


    @Override
    public int getScore() {
        return score;
    }
}
