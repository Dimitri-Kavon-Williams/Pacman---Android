package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import de.hs_kl.imst.gatav.tilerenderer.GameView;

public class Yodasprite implements Drawable {
    private static final int BMP_COLUMNS = 6;
    private static final int BMP_ROWS = 1;

    private GameContent gameContent;
    float fracsect;
    private int x = 0;
    private int y = 800;
    int xSpeed = 5;
    private Bitmap bmp;
    private int currentFrame = 0;
    private int width;
    private int height;
    long current_time;
    long lasttime;
    long delta;

    public Yodasprite(GameContent gameContent, Bitmap bmp) {
        this.gameContent = gameContent;
        this.bmp = bmp;
        this.width = bmp.getWidth() / BMP_COLUMNS;
        this.height = bmp.getHeight() / BMP_ROWS;
        this.lasttime = System.currentTimeMillis();
    }

    /*
    Update der Position basierend auf der Spielgröße als auch die Größe der Bitmap.
    CurrentFrame geht jedes einzelne Frame hindruch aufgrund von Modulo BMP_COLUMN
     */

    @Override
    public void update(float fracsec) {

        if (x > gameContent.getGameWidth() - width + xSpeed) {
            xSpeed = -5;
        }
        if (x + xSpeed < 0) {
            xSpeed = 5;
        }
        if (delta / fracsec >999) {
            x = x + xSpeed;
            currentFrame = ++currentFrame % BMP_COLUMNS;
        }
    }
/*
    Transformation und update der Matrix
 */

    @Override
    public void draw(Canvas canvas) {
        current_time = System.currentTimeMillis();
        delta = current_time - lasttime;
        fracsect = (float) delta / 1000;
        update(fracsect);

        int srcX = currentFrame * (width -4);
        int srcY = height - height;

        Rect src = new Rect(srcX, srcY,srcX + width+5,srcY+height); // Generiert Bild zu diesen Maßen - Quellenrechteck
        Rect dst = new Rect(x,y,x+width, y+height); // Skaliert Bild zu diesen Maßen - Skalierendes Rechteck
        canvas.drawBitmap(bmp, src, dst, null);
    }
}


