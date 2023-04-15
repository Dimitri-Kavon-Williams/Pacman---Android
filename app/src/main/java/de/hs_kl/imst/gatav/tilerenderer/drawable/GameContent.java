package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.hs_kl.imst.gatav.tilerenderer.GameView;
import de.hs_kl.imst.gatav.tilerenderer.R;
import de.hs_kl.imst.gatav.tilerenderer.util.Direction;

public class GameContent implements Drawable {

    /**
     * Breite und Höhe des Spielfeldes in Pixel
     *
     */

    private int gameWidth = -1;
    private int gameHeight = -1;

    public int getGameWidth() {
        return gameWidth;
    }

    public int getGameHeight() {
        return gameHeight;
    }

    /**
     * Beinhaltet alle Tiles, die das Spielfeld als solches darstellen. Diese werden als erstes
     * gezeichnet und bilden somit die unterste Ebene.
     */

    private TileGraphics[][] tiles;// [zeilen][spalten]

    /**
     * Beinhaltet Referenzen auf alle dynamischen Kacheln, deren {@link Drawable#update(float)} Methode
     * aufgerufen werden muss. Damit lassen sich Kachel-Animationen durchführen.
     */

    private ArrayList<TileGraphics> dynamicTiles = new ArrayList<>(); //


    /**
     * Beinhaltet alle Ziele. Diese werden als zweites und somit über die in {@link GameContent#tiles}
     * definierten Elemente gezeichnet.
     */

    private TileGraphics[][] targetTiles;   // [zeilen][spalten]

    /**
     * Beinhaltet Referenzen auf alle Ziele
     */

    private ArrayList<TileGraphics> targets = new ArrayList<>();

    /**
     * Beinhaltet Referenzen auf Kacheln (hier alle vom Typ {@link Floor}), auf welchen ein Ziel
     * erscheinen kann.
     */

    // private ArrayList<TileGraphics> possibleEnemies = new ArrayList<>(); // GGF Verwenden

    private ArrayList<TileGraphics> possibleTargets = new ArrayList<>(); // floor no wall no space no player check

    /**
     * Anzahl der eingesammelten Ziele
     */

    private int collectedTargets = 0;

    public int getCollectedTargets() {
        return collectedTargets;
    }

    /**
     * Anzahl der gesammelten Punkte
     */
    private int collectedScore = 0;

    public int getCollectedScore() {
        return collectedScore;
    }

    /**
     * Beinhaltet Referenz auf Spieler, der bewegt wird.
     */
    private Player player = null;

    /**
     *  Spielobjekte
     */

    private Enemy dynTuscan = null;
    private Bith dynBith = null;
    private DynamicTarget dynTarget = null;
    private BorgCube dynTargetBorg = null;
    private Lukesprite luke_sprite;
    private Bitmap bmp;
    private Bitmap bmp_1;
    private Yodasprite yodasprite;
    private Stormtrooper stormtrooper;



    public DynamicTarget getDynTarget() {
        return dynTarget;
    }

    /**
     * Wird in {@link GameContent#movePlayer(Direction)} verwendet, um dem Game Thread
     * die Bewegungsrichtung des Players zu übergeben.
     * <p>
     * Wird vom Game Thread erst auf IDLE zurückgesetzt, sobald die Animation abgeschlossen ist
     */

    private volatile Direction playerDirection = Direction.IDLE;

    synchronized public void resetPlayerDirection() {
        playerDirection = Direction.IDLE;
    }

    synchronized public boolean isPlayerDirectionIDLE() {
        return playerDirection == Direction.IDLE;
    }

    synchronized public void setPlayerDirection(Direction newDirection) {
        playerDirection = newDirection;
    }

    synchronized public Direction getPlayerDirection() {
        return playerDirection;
    }


    /**
     * Zufallszahlengenerator zum Hinzufügen neuer Ziele
     */
    private Random random = new Random();


    private Context context;

    /**
     * {@link AssetManager} über den wir unsere Leveldaten beziehen
     */
    private AssetManager assetManager;

    /**
     * Name des Levels
     */

    private String levelName;

    /**
     * @param context   TODO <Zugriff auf Inhalt der Applikation wir wissen nun das GameContent zu der Anwendung gehört/>
     * @param levelName Name des zu ladenden Levels
     */

    // Constructor für gameContent Varaible in GameView
    public GameContent(Context context, String levelName) { // Welcher Context wird hieraufgerufen
        this.context = context;
        this.assetManager = context.getAssets(); // Zugriff auf Assets in der Applikation welche zu context gehört.
        this.levelName = levelName;

        // Level laden mit Wall (W), Floor (F) und Player (P)
        // Target wird im geladenen Level zum Schluss zusätzlich gesetzt
        try {
            loadLevel(assetManager.open(String.format("levels/%s.txt", levelName))); // Hier wird der ganze Levelname eingefügt z.B Level1,2,3
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
            Erstellung der Sprite und Bmp Objekte
         */
        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.spritesheet_lasermove);
        luke_sprite = new Lukesprite(this, bmp);

        bmp_1 = BitmapFactory.decodeResource(context.getResources(),R.drawable.spirte_yodalasersword);
        yodasprite = new Yodasprite(this, bmp_1);


        // Player ist animiert und muss deshalb updates auf seine Position erfahren
        // ggf. hier Animationen einbauen
        dynamicTiles.add(player);
    }


    /**
     * Überprüfung der Möglichkeit einer Verschiebung des Players in eine vorgegebene Richtung
     * <p>
     * <p>
     * <p>
     * /** Geprüft wird auf Spielfeldrand und Hindernisse. -  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * /*
     * <p>
     * <p>
     * Falls das zulässige Zielfeld ein Target ist, wird dieses konsumiert und ein neues Target gesetzt.
     * Dann wird die Bewegung des Players durchgeführt bzw. angestoßen (Animation)
     *
     * @param direction Richtung in die der Player bewegt werden soll
     * @return true falls Zug erfolgreich durchgeführt bzw. angestoßen, false falls Zug nicht durchgeführt
     */

    public boolean movePlayer(@NonNull Direction direction) { // Spielerfigur läuft
        // Erster Schritt: Basierend auf Zugrichtung die Zielposition bestimmen

        int newX = -1; // Neues X
        int newY = -1; // Neues Y

        switch (direction) { // Switch case für Parameter Direction when this then ....
            case UP:
                newX = player.getX();
                newY = player.getY() - 1;
                break; // Remember Gitterkoordinaten System / Pixelkoordinatensystem up is down and down is up
            case DOWN:
                newX = player.getX();
                newY = player.getY() + 1;
                break; // Left and Right movement does not change for the coordinates
            case RIGHT:
                newX = player.getX() + 1;
                newY = player.getY();
                break;
            case LEFT:
                newX = player.getX() - 1;
                newY = player.getY();
                break;
        }  // Methode hat Bewegungsrichtung erhalten.

        if ((!(newX >= 0 && newX < gameWidth && newY >= 0 && newY < gameHeight))) // Prüfen ob Spieler sich außerhalb vom Spielfeld befindet
            throw new AssertionError("Spieler wurde außerhalb des Spielfeldes bewegt. Loch im Level?"); // Behaupte XYZ

        // Zweiter Schritt: Prüfen ob Spieler sich an Zielposition bewegen kann (Zielkachel.isPassable())

        TileGraphics targetTile = tiles[newY][newX]; // Position wird gespeichert auf Ebene 1

        if (tiles[newY][newX] != null && tiles[newY][newX] instanceof Wall) { // Part of one Algorithm did not fully understand yet, that I am implementing a condition that is checked and move is still executed
            collectedScore -= 4;
        }

        if (targetTile == null || !targetTile.isPassable()) // Gespeicherte Postion auf Ebene 1 ist null oder nicht passierbar ?
            return false; // return false


        // Dritter Schritt: Spieler verschieben bzw. Verschieben starten.
        // Hinterher steht der Spieler logisch bereits auf der neuen Position

        player.move(newX, newY); // Methode move wird aufgerufen - Spieler hat sich bewegt auf neue Position -

        // Aufgabe Wandberührung: Verschiebe -4;

        // Vierter Schritt: Prüfen ob auf der Zielkachel ein Target existiert
        if (targetTiles[newY][newX] != null && targetTiles[newY][newX] instanceof Target) {
            if (samePosition(player, targetTiles[newY][newX])) {// targetTiles not null and targetTiles is an instance of target meaning the tile has a target
                collectedTargets++; // Increase CollectedTarget Amount By 1
                collectedScore += targetTiles[newY][newX].getScore();
                // Altes Ziel entfernen
                targets.remove(targetTiles[newY][newX]); // Remove the Object saved in targets Arraylist. We are deleting the object directly not the index.
                targetTiles[newY][newX] = null; // targetTiles null setzen
                // Neues Ziel erzeugen
                createNewTarget(); // Okay
            }
        }

        // Prüfen ob auf der Zielposition das dynamische Target existert => Sonderpunkte :-)
        if (dynTarget != null && dynTarget instanceof DynamicTarget) {
            if (samePosition(player, dynTarget)) {
                collectedScore += dynTarget.getScore();
                dynamicTiles.remove(dynTarget);
                dynTarget = null;
                createAndMoveDynamicTarget();
                // TODO Done
            }
        }

        if (dynTargetBorg != null && dynTargetBorg instanceof BorgCube) {
            if (samePosition(player, dynTargetBorg)) {
                collectedScore += dynTargetBorg.getScore();
                dynamicTiles.remove(dynTargetBorg);
                dynTargetBorg = null;
                createAndMoveDynamicBorgCube();
            }
        }


        if (dynTuscan != null && dynTuscan instanceof Enemy) {
            if (samePosition(player, dynTuscan)) {
                collectedScore += dynTuscan.getScore();
                dynamicTiles.remove(dynTuscan);
                dynTuscan = null;
                createAndMoveEnemy();
            }
        }

        if (stormtrooper != null && stormtrooper instanceof Stormtrooper) {
            if(samePosition(player,stormtrooper)) {
                collectedScore += stormtrooper.getScore();
                dynamicTiles.remove(stormtrooper);
                stormtrooper = null;
                createAndMoveStormtrooper();
            }
        }

        if (dynBith != null && dynBith instanceof Bith){
            if(samePosition(player,dynBith)){
                collectedScore += dynBith.getScore();
                dynamicTiles.remove(dynBith);
                dynBith = null;
                createAndMoveBith();
            }
        }
        return true;
    }

    /**
     * Spielinhalt zeichnen
     *
     * @param canvas Zeichenfläche, auf die zu Zeichnen ist
     */

    @Override
    public void draw(Canvas canvas) {
        // Erste Ebene zeichnen (Wände und Boden)
        for (int yIndex = 0; yIndex < tiles.length; yIndex++)
            for (int xIndex = 0; xIndex < tiles[yIndex].length; xIndex++) {
                if (tiles[yIndex][xIndex] == null) continue;
                tiles[yIndex][xIndex].draw(canvas);
            }
        // Zweite Ebene zeichnen
        for (int yIndex = 0; yIndex < targetTiles.length; yIndex++)
            for (int xIndex = 0; xIndex < targetTiles[yIndex].length; xIndex++) {
                if (targetTiles[yIndex][xIndex] == null) continue;
                targetTiles[yIndex][xIndex].draw(canvas);
            }

            // Draw Sprites
            luke_sprite.draw(canvas);
            yodasprite.draw(canvas);

        // Dynamisches Ziel zeichnen
        if (dynTarget != null)
            dynTarget.draw(canvas);

        if (dynTargetBorg != null)
            dynTargetBorg.draw(canvas);

        if (dynTuscan != null)
            dynTuscan.draw(canvas);

        if(dynBith != null)
            dynBith.draw(canvas);

        if(stormtrooper != null)
            stormtrooper.draw(canvas);

        // Spieler zeichnen
        player.draw(canvas);
    }



    /**
     * Spielinhalt aktualisieren (hier Player und Animation dynamischer Kacheln)
     *
     * @param fracsec Teil einer Sekunde, der seit dem letzten Update des gesamten Spielzustandes vergangen ist
     */


    @Override
    public void update(float fracsec) {
        // 1. Schritt: Auf mögliche Player Bewegung prüfen und ggf. durchführen/anstoßen
        // vorhandenen Player Move einmalig ausführen bzw. anstoßen, falls
        // PlayerDirection nicht IDLE ist und Player aktuell nicht in einer Animation

        //Log.d("updateGameContent", ""+isPlayerDirectionIDLE()+" "+player.isMoving());

        if (!isPlayerDirectionIDLE() && !player.isMoving())
            movePlayer(getPlayerDirection());

        if (stormtrooper == null) {
            if(random.nextDouble() < 0.002){
                createAndMoveStormtrooper();
            }
        }

        if (dynTarget == null) {
            if (random.nextDouble() < 0.004)
                createAndMoveDynamicTarget();
        }

        if (dynTargetBorg == null) {
            if (random.nextDouble() < 0.005)
                createAndMoveDynamicBorgCube();
        }

        if (dynTuscan == null) {
            if (random.nextDouble() < 0.05)
                createAndMoveEnemy();
        }

        if (dynBith == null) {
            if (random.nextDouble() <0.06)
                createAndMoveBith();
        }
        // Dynamisches Ziel erzeugen - BorgCube

        // 2. Schritt: Updates bei allen dynamischen Kacheln durchführen (auch Player)

        for (TileGraphics dynamicTile : dynamicTiles)
            dynamicTile.update(fracsec);


        // 3. Schritt: Animationen auf Ende überprüfen und ggf. wieder freischalten
        // Player Move fertig ausgeführt => Sperre für neues Player Event freischalten

        if (!player.isMoving())
            resetPlayerDirection(); // Steht wieder still set PlayerDirection Idle

        if (dynTarget != null && !dynTarget.isMoving()) {
            dynamicTiles.remove(dynTarget);
            dynTarget = null;
        }

        if (dynTargetBorg != null && !dynTargetBorg.isMoving()) {
            dynamicTiles.remove(dynTargetBorg);
            dynTargetBorg = null;
        }

        if (dynTuscan != null && !dynTuscan.isMoving()) {
            dynamicTiles.remove(dynTuscan);
            dynTuscan = null;
        }

        if(dynBith != null && !dynBith.isMoving()){
            dynamicTiles.remove(dynBith);
            dynBith = null;
        }

        if(stormtrooper != null && !stormtrooper.isMoving()){
            dynamicTiles.remove(stormtrooper);
            stormtrooper = null;
        }
    }


    /**
     * Level aus Stream laden und Datenstrukturen entsprechend initialisieren
     *
     * @param levelIs InputStream von welchem Leveldaten gelesen werden sollen
     * @throws IOException falls beim Laden etwas schief geht (IO Fehler, Fehler in Leveldatei)
     */

    private void loadLevel(InputStream levelIs) throws IOException {
        ArrayList<String> levelLines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(levelIs));
        int maxLineLength = 0;
        String currentLine = null;
        while ((currentLine = br.readLine()) != null) {
            maxLineLength = Math.max(maxLineLength, currentLine.length());
            levelLines.add(currentLine);
        }
        br.close();
        gameWidth = (int) (maxLineLength * TileGraphics.getTileSize());
        gameHeight = (int) (levelLines.size() * TileGraphics.getTileSize());

        // Zweiter Schritt: basierend auf dem Inhalt der Leveldatei die Datenstrukturen befüllen
        tiles = new TileGraphics[levelLines.size()][];
        targetTiles = new TileGraphics[levelLines.size()][];

        for (int yIndex = 0; yIndex < levelLines.size(); yIndex++) {
            tiles[yIndex] = new TileGraphics[maxLineLength];
            targetTiles[yIndex] = new TileGraphics[maxLineLength];
            String line = levelLines.get(yIndex);
            for (int xIndex = 0; xIndex < maxLineLength && xIndex < line.length(); xIndex++) {
                TileGraphics tg = getTileByCharacter(line.charAt(xIndex), xIndex, yIndex);

                if (tg instanceof Floor) {
                    possibleTargets.add(tg);
                    tiles[yIndex][xIndex] = tg;
                } else if (tg instanceof Player) {
                    tiles[yIndex][xIndex] = getTileByCharacter('f', xIndex, yIndex);
                    possibleTargets.add(tiles[yIndex][xIndex]);
                    if (player != null)
                        throw new IOException("Invalid level file, contains more than one player!");
                    player = (Player) tg;
                } else {                            // Wall Kacheln
                    tiles[yIndex][xIndex] = tg;
                }
            }
        }

        // Dritter Schritt: erste Ziele erzeugen und platzieren
        createNewTarget();
        createNewTarget();
        createNewTarget();
    }


    /**
     * Erzeugt ein dynamisches Ziel TODO Done
     * Dynamisches Ziel wird nicht erstellt wenn die Zielkachel aus unbekannten Gründen Null ist. Das Objekt keine Werte erhalten hat.
     * Dynamisches Ziel wird nicht erzeugt falls die übergebenen Werte der Kachel die größe des Spiels überschreiten und oder kleiner als 0 sind
     * Ansonsten befindet sich das dynamische Ziel logisch "über" der Ebene der anderen Ziele.
     * Nach erfolgreichem Anlegen wird der Move direkt initiiert.
     *
     * @return dynamisches Ziel, kann null sein, falls es von einer gewählten Source nicht erzeugt werden konnte
     */


    @Nullable
    public void createAndMoveDynamicTarget() { // Method Returned Void
        // Source zufällig aber gültig auswählen
        TileGraphics sourceTile = possibleTargets.get(random.nextInt(possibleTargets.size())); // Source tile zufällig aber gültig (so groß wie Arrayliste possibleTargets möglich)
        // Sicherstellen, dass das Ziel nicht an der gleichen Position wie der Spieler erzeugt wird
        // und sich dort nicht bereits ein normales Ziel befindet
        while (samePosition(sourceTile, player) || targetTiles[sourceTile.getY()][sourceTile.getX()] != null) // Falls Selbe Position Spieler oder das Feld der Erstellung des dynamischen Ziels nicht Null ist
            sourceTile = possibleTargets.get(random.nextInt(possibleTargets.size()));

        ArrayList<Integer> dl = new ArrayList<Integer>();
        dl.add(0);
        dl.add(1);
        dl.add(2);
        dl.add(3);
        Collections.shuffle(dl);

        TileGraphics destinationTile = null;
        Direction destinationDirection = Direction.IDLE;
        int destDir = -1;
        int newX = -1, newY = -1;

        // Zufällige Richtungsbestimmung

        for (int i = 0; i < 4; i++) {
            switch (dl.get(i)) {
                case 0:
                    newX = sourceTile.getX() - 1;
                    newY = sourceTile.getY();
                    destinationDirection = Direction.LEFT;
                    destDir = 0;
                    break;
                case 1:
                    newX = sourceTile.getX() + 1;
                    newY = sourceTile.getY();
                    destinationDirection = Direction.RIGHT;
                    destDir = 1;
                    break;
                case 2:
                    newX = sourceTile.getX();
                    newY = sourceTile.getY() - 1; // we go up y - 1
                    destinationDirection = Direction.UP;
                    destDir = 2;
                    break;
                case 3:
                    newX = sourceTile.getX();
                    newY = sourceTile.getY() + 1; // We go down y + 1
                    destinationDirection = Direction.DOWN;
                    destDir = 3;
                    break;
            }

            if ((!(newX >= 0 && newX < gameWidth && newY >= 0 && newY < gameHeight)))
                continue;

            destinationTile = tiles[newY][newX];
            if (destinationTile == null || !destinationTile.isPassable()) { // destinationTile is not null and is not passable ?
                destinationTile = null;
                continue; // Fortfahren
            }
            break;
        }
        if (destinationTile == null)
            return;

        // Dynamischen Ziel erzeugen und Move einstellen
        dynTarget = new DynamicTarget(sourceTile.getX(), sourceTile.getY(), getGraphicsStream(levelName, "dynobj" + destDir));  // TODO Done
        dynTarget.move(newX, newY); // bestimmt bewegung
        dynTarget.setSpeed(0.4f);   // Bestimmt Geschwindigkeit TODO Done
        dynamicTiles.add(dynTarget); // Füge zu dynamicTiles dynTarget hinzu.
    }

/*
    Erstellung einer Route
 */
    public void createAndMoveEnemy() {

        // Route mit Koordinaten besetzten
        // x = Reihenposition, Y = Spaltenposition

        List<int[]> corList = new ArrayList<int[]>(5);
        int[] intArray0 = new int[]{5,3}; // Spawn
        int[] intArray1 = new int []{13,3}; // Rechts
        int[] intArray2 = new int[]{13,1}; // Hoch
        int[] intArray3 = new int[]{5,1}; // Links
        int[] intArray4 = new int[]{5,3}; // Runter
        corList.add(0, intArray0);
        corList.add(1, intArray1);
        corList.add(2, intArray2);
        corList.add(3, intArray3);
        corList.add(4, intArray4);

        int currentIndex = 0;
        int[] corXY = corList.get(currentIndex);

        if(corXY[0] >= 0 && corXY[0] <= gameWidth && corXY[1] >= 0 && corXY[1] < gameHeight){
            dynTuscan = new Enemy(corXY[0], corXY[1], getGraphicsStream(levelName, "enemy1"));
        }else{
            dynTuscan = new Enemy(5, 5, getGraphicsStream(levelName, "enemy1"));
        }

        dynTuscan.move(corList);
        dynTuscan.setSpeed(0.3f);
        dynamicTiles.add(dynTuscan);
    }

    /*
        Erstelle das Bith Objekt unter Einhaltung der Bith's
     */
    public void createAndMoveBith(){
            List<int[]> corList = new ArrayList<int[]>(3);
            int[] intArray0 = new int[]{3,5};
            int[] intArray1 = new int []{13,5};
            int[] intArray2 = new int[]{3,5};
            corList.add(0, intArray0);
            corList.add(1, intArray1);
            corList.add(2, intArray2);

            int currentIndex = 0;
            int[] corXY = corList.get(currentIndex);

            if(corXY[0] >= 0 && corXY[0] <= gameWidth && corXY[1] >= 0 && corXY[1] < gameHeight){
                dynBith = new Bith(corXY[0],corXY[1], getGraphicsStream(levelName,"bith"));
            }else{
                dynBith = new Bith(corXY[0],corXY[1], getGraphicsStream(levelName,"bith"));
            }

            dynBith.move(corList);
            dynBith.setSpeed(0.7f);
            dynamicTiles.add(dynBith);
    }

    /*
        Erstelle den Stormtrooper unter Einhaltung der Spielbreite
     */

    public void createAndMoveStormtrooper(){
        List<int[]> corList = new ArrayList<int[]>(2);
        int[] intArray0 = new int[] {2,1};
        int[] intArray1 = new int[] {2,5};
        corList.add(0,intArray0);
        corList.add(1,intArray1);

        int currentIndex = 0;
        int[] corXY = corList.get(currentIndex);

        if (corXY[0] >= 0 && corXY[0] <= gameWidth && corXY[1] >= 0 && corXY[1] < gameHeight){
            stormtrooper = new Stormtrooper(corXY[0],corXY[1], getGraphicsStream(levelName,"stormyie"));
        }else{
            stormtrooper = new Stormtrooper(corXY[0],corXY[1], getGraphicsStream(levelName,"stormyie"));
        }
        stormtrooper.move(corList);
        stormtrooper.setSpeed(0.3f);
        dynamicTiles.add(stormtrooper);
    }




    public void createAndMoveDynamicBorgCube() {
        TileGraphics sourceTile = possibleTargets.get(random.nextInt(possibleTargets.size())); // Source tile zufällig aber gültig (so groß wie Arrayliste possibleTargets möglich)
        // Sicherstellen, dass das Ziel nicht an der gleichen Position wie der Spieler erzeugt wird
        // und sich dort nicht bereits ein normales Ziel befindet
        while (samePosition(sourceTile, player) || targetTiles[sourceTile.getY()][sourceTile.getX()] != null) // Falls Selbe Position Spieler oder das Feld der Erstellung des dynamischen Ziels nicht Null ist
            sourceTile = possibleTargets.get(random.nextInt(possibleTargets.size()));

        // Destination bestimmen, falls möglich, ansonsten Abbruch

        ArrayList<Integer> dl = new ArrayList<Integer>();
        dl.add(0);
        dl.add(1);
        dl.add(2);
        dl.add(3);
        Collections.shuffle(dl);
        TileGraphics destinationTile = null;

        int destDir = -1;
        int newX = -1, newY = -1;

        // Zwei Werte X und Y -1
        // alle vier Richtungen zufällig durchgehen, bis die erste passt oder eben keine

        for (int i = 0; i < 4; i++) {
            switch (dl.get(i)) {
                case 0:
                    newX = sourceTile.getX() - 1;
                    newY = sourceTile.getY();
                    destDir = 0; // Links
                    break;
                case 1:
                    newX = sourceTile.getX() + 1;
                    newY = sourceTile.getY();
                    destDir = 1; // Rechts
                    break; // desired Destination will be 1
                case 2:
                    newX = sourceTile.getX();
                    newY = sourceTile.getY() - 1;
                    destDir = 2; // Oben
                    break;
                case 3:
                    newX = sourceTile.getX();
                    newY = sourceTile.getY() + 1; // We go down y + 1
                    destDir = 3; // Unten
                    break;
            } // Sucht im Prinzip nach einer der vier Richtungen die eine der Grenzen/Konditionen nicht überschreitet von der Spiellänger als auch > 0

            if ((!(newX >= 0 && newX < gameWidth && newY >= 0 && newY < gameHeight)))
                continue;
            destinationTile = tiles[newY][newX];

            // Zugriff auf Objekt .isPassable() greift auf das Objekt zu und liefert den boolean Wert aus der Klasse Wall
            if (destinationTile == null || !destinationTile.isPassable()) {
                destinationTile = null;
                continue; // Fortfahren
            }
            break;
        }
        if (destinationTile == null)
            return;

        // Dynamischen Ziel erzeugen und Move einstellen

        dynTargetBorg = new BorgCube(sourceTile.getX(), sourceTile.getY(), getGraphicsStream(levelName, "bigobj" + destDir));  // TODO Done
        dynTargetBorg.move(newX, newY); // bestimmt bewegung
        dynTargetBorg.setSpeed(0.2f);   // Bestimmt Geschwindigkeit TODO Done
        dynamicTiles.add(dynTargetBorg); // Füge zu dynamicTiles dynTarget hinzu.
    }


    // Zweites dynamisches Ziel erstellen und fortbewegen lassen

    /**
     * Erzeugt ein neues Ziel und sorgt dafür, dass dieses sich nicht auf der Position des Spielers
     * oder eines vorhandenen Ziels befindet
     *
     * @return neues Ziel
     */

    private void createNewTarget() {
        TileGraphics targetTile = possibleTargets.get(random.nextInt(possibleTargets.size()));
        // Sicherstellen, dass das Ziel nicht an der gleichen Position wie der Spieler erzeugt wird
        // und sich dort nicht bereits ein Ziel befindet
        while (samePosition(targetTile, player) || targetTiles[targetTile.getY()][targetTile.getX()] != null)
            targetTile = possibleTargets.get(random.nextInt(possibleTargets.size())); // We get a random value that fits within possibleTargets size and save it in targetTile

        // Ziel zufällig auswählen
        Target newTarget = chooseTarget(targetTile.getX(), targetTile.getY(), 0);
        targetTiles[newTarget.getY()][newTarget.getX()] = newTarget; // - Position wird zugewiesen für das neueTarget, jeweils der X und Y Wert
        targets.add(newTarget); // - Targetobjekt zur Arrayliste hinzufügen
    }

    /**
     * Sucht das neue Ziel aus
     *
     * @param x            x-Koordinate
     * @param y            y-Koordinate
     * @param targetNumber 0 für zufällige Auswahl, 1-... für explizite Auswahl des Ziels
     * @return Das Ziel
     */

    // Wir haben eine chooseTarget Methode
    @NonNull
    private Target chooseTarget(int x, int y, int targetNumber) {
        int targetScores[] = {1, 2, 4, 8};    // TODO // Score of the target in an array 1,2,4
        double targetProps[] = {0.5, 0.6, 0.8, 0.95}; // TODO // Probability of targets appearing

        int targetIndex;

        // zufällige Auswahl des Targets nach Wahrscheinlichkeiten in targetProps
        if (targetNumber == 0) { // Auswahlzufällig
            double dice = random.nextDouble(); // Have Dice
            targetIndex = targetProps.length -1;

            while (targetIndex > 0 && dice < targetProps[targetIndex]) {
                targetIndex--;
            }
            targetNumber = targetIndex + 1;

        } else  // explizite Wahl der Nummer des Targets
        {
            if (targetNumber < 1 || targetNumber > targetScores.length)
                targetNumber = 1;
            targetIndex = targetNumber - 1;
        }

        Log.d("ls", "Score = " +  targetScores[targetIndex] + " GrafikNumber = " + targetNumber);
        return new Target(x, y, getGraphicsStream(levelName, "ls" + targetNumber), targetScores[targetIndex]);
    }

    /**
     * Prüft ob zwei Kacheln auf den gleichen Koordinaten liegen
     * @param a erste Kachel
     * @param b zweite Kachel
     * @return true wenn Position gleich, andernfalls false
     */

    private boolean samePosition(TileGraphics a, TileGraphics b) {
        if(a.getX() == b.getX() && a.getY() == b.getY()) // Wenn a = b und b = a return true kachel selbse Position da TileGraphics [] [] a [] [] b
            return true;
        return false;
    }

    /**
     * Besorgt Inputstream einer Grafikdatei eines bestimmten Levels aus den Assets
     * @param levelName     Levelname
     * @param graphicsName  Grafikname
     * @return Inputstream
     */

    public InputStream getGraphicsStream(String levelName, String graphicsName) {
        try { // Lädt Leveldatei.txt d.h W,F,P
            return assetManager.open("levels/" + levelName + "/" + graphicsName + ".png");
        }catch(IOException e){
            try {
                return assetManager.open("levels/default/" + graphicsName + ".png");
            }catch(IOException e2){
                return null;
            }
        }
    }


    @Nullable
    private TileGraphics getTileByCharacter(char c, int xIndex, int yIndex) { // Zu TileGrapihcs gehören Floor und Player
        switch(c) {
            case 'w':
            case 'W': return new Wall(xIndex, yIndex, getGraphicsStream(levelName, "wall"));    // TODO Done
            case 'f':
            case 'F': return new Floor(xIndex, yIndex, getGraphicsStream(levelName, "floor"));
            case 'p':
            case 'P': return new Player(xIndex, yIndex, getGraphicsStream(levelName, "luke5Big"));
        }
        return null;
    }
}
