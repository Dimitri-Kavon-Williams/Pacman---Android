package de.hs_kl.imst.gatav.tilerenderer.drawable;


import android.graphics.Canvas;
import android.util.Log;
import java.io.InputStream;
import de.hs_kl.imst.gatav.tilerenderer.util.Direction;


public abstract class MovableTileGraphics extends TileGraphics {
    private float speed = 10f;
    public void setSpeed(float speed) { this.speed = speed; }

    // Bewegungskoordinaten
    protected int sourceX, sourceY;
    protected float currentX, currentY; // Gleitkomma-Koordinaten zur Bewegung zwischen zwei Kacheln
    protected int targetX, targetY;

    protected volatile Direction currentDirection = Direction.IDLE;  // Absolute nicht verändert alle threads, selbe Wert für alle Threads
    synchronized public boolean isMoving() { return currentDirection != Direction.IDLE; }
    synchronized protected void setMovingDirection(Direction newDirection) { currentDirection = newDirection; } //newDirection is the argument from move



    public MovableTileGraphics(int x, int y, InputStream is) {
        super(x, y, is);
        // Update bekommt ebenfalls die Werte
        currentX = x; // Tuscan 5 zuerst
        currentY = y; // Tuscan 3 zuerst
    }

    // Koordianten werden hier für alle Spielobjekte übergeben
    @Override
    public void move(int x, int y) {
        // einmalig die Bewegung festlegen
        // mittels der gesetzten Direction lassen sich auch weitere Eingaben blocken,
        // bis die Bewegung schließlich (mittels updates) komplett durchgeführt wurde

        // Setzt die Richtung einmalig
        // das this. bezieht sich auf das Objekt bzw auf den Konstruktor einer Methode
        if(this.x > x)
            setMovingDirection(Direction.LEFT);
        else if(this.x < x)
            setMovingDirection(Direction.RIGHT);
        else if(this.y > y)
            setMovingDirection(Direction.UP);
        else if(this.y < y)
            setMovingDirection(Direction.DOWN);
        // Quelle und Zielblock festlegen

        // Speichert den Wert vom Objekt
        sourceX = this.x;
        sourceY = this.y;

        // Speichert den Wert von den Parametern der Methode Move
        targetX = x; // 13
        targetY = y; // 3

        // normaler Move,  vorab logisch schon einmal auf die neue Kachel vornehmen
        super.move(x, y); // Werte aus move werden übergeben
    }

    @Override
    public void update(float fracsec) {

        int deltaX = targetX - sourceX;
        int deltaY = targetY - sourceY;

        if(currentDirection == Direction.LEFT || currentDirection == Direction.RIGHT) {
            currentX += fracsec * deltaX * speed;   // Bewegung

            // Bewegung vollständig durchgeführt => Stillstandssignal setzen
            if((currentDirection == Direction.LEFT && currentX <= targetX) ||
                    (currentDirection == Direction.RIGHT && currentX >= targetX)) {
                currentX = targetX;
                setMovingDirection(Direction.IDLE);
            }
        }else if(currentDirection == Direction.UP || currentDirection == Direction.DOWN) {
            currentY += fracsec * deltaY * speed;   // Bewegung

            // Bewegung vollständig durchgeführt => Stillstandssignal setzen
            if((currentDirection == Direction.UP && currentY <= targetY) ||
                    (currentDirection == Direction.DOWN && currentY >= targetY)) {
                currentY = targetY;
                setMovingDirection(Direction.IDLE);
            }
        }
    }


    @Override
    public void draw(Canvas canvas) {
        // Aktuelle Transformationsmatrix speichern
        canvas.save();
        // Transformationsmatrix an Pixel-Koordinate von Block verschieben
        canvas.translate(currentX * tileSize, currentY * tileSize);
        // An der aktuellen Position ein Rechteck entsprechender Größe oder die existierende Bitmap
        if(tileBitmap == null)
            canvas.drawRect(0, 0, tileSize, tileSize, tilePaint);
        else
            canvas.drawBitmap(tileBitmap, 0 , 0, null);
        // Transformationsmatrix auf den Stand von vorherigem canvas.save() zurücksetzen
        canvas.restore();
    }
}
