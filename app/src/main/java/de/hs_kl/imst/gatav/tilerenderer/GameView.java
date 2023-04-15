package de.hs_kl.imst.gatav.tilerenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.hs_kl.imst.gatav.tilerenderer.drawable.GameContent;
import de.hs_kl.imst.gatav.tilerenderer.drawable.Lukesprite;
import de.hs_kl.imst.gatav.tilerenderer.drawable.TileGraphics;
import de.hs_kl.imst.gatav.tilerenderer.util.Direction;
import de.hs_kl.imst.gatav.tilerenderer.util.LevelHelper;


/**
 * {@link SurfaceView} welches sich um die Darstellung des Spiels und Interaktion mit diesem kümmert.
 * Erzeugt eine Gameloop ({@link GameView#gameThread}), welcher die Aktualisierung von Spielzustand
 * und -darstellung regelt.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, GestureDetector.OnGestureListener {

    private SurfaceHolder surfaceHolder;


    private Thread gameThread;
    private boolean runningRenderLoop = false;
    public boolean gameOver=false;
    private String levelName;
    private Thread timeThread;
    private volatile boolean runningTimeThread=false;    // access to elementary data types (not double or long) are atomic and should be volatile to synchronize content
    private volatile double elapsedTime = 0.0;


    synchronized private void resetElapsedTime() { elapsedTime = 0.0;}
    synchronized private double getElapsedTime() { return elapsedTime; }
    synchronized private void increaseElapsedTime(double increment) { elapsedTime += increment; }

    private double maxCollectedTargets = 30;

    private int gameMode=0; // 0 game not startet, 1 game started by first fling gesture, 2 game over

    private float gameWidth = -1;
    private float gameHeight = -1;

    private GestureDetectorCompat gestureDetector;

    // - Konstruktor in GameContent liefert uns GameContent Variable

    private GameContent gameContent;

    // Paint Objekt erstellt
    private Paint scoreAndTimePaint = new Paint();
    {   scoreAndTimePaint.setColor(Color.GRAY); // Setze Farbe
        scoreAndTimePaint.setTextSize(20); // Setze Textgröße
    }

    /**
     * Konstruktor, initialisiert surfaceHolder und setzt damit den Lifecycle des SurfaceViews in Gang
     * @param context Kontext
     */

    //GameView Konstruktor -
    public GameView(Context context, String level) {
        super(context);
        levelName=level;

        surfaceHolder = getHolder();

        surfaceHolder.addCallback(this);

        // GameView enthält Gestendetection
        gestureDetector = new GestureDetectorCompat(context, this);

        // - Erstellen Textgröße irgendwie
        scoreAndTimePaint.setTextSize(20f * context.getResources().getDisplayMetrics().density);
    }



    /**
     * Aktualisiert die grafische Darstellung; wird von Gameloop aufgerufen
     * @param canvas Zeichenfläche
     */

    void updateGraphics(Canvas canvas) {
        // Layer 0 (clear background)
        canvas.drawColor(Color.parseColor("#555555"));

        // Layer 1 (Game content)
        if(gameContent == null) return;
        canvas.save();
        canvas.translate((canvas.getWidth() - gameContent.getGameWidth()) / 2,
                (canvas.getHeight() - gameContent.getGameHeight()) / 2);
        gameContent.draw(canvas);
        canvas.restore();

        // Layer 2 (Collected Targets, Score and Elapsed Time)
        String collectedText = String.format("%d gesammelt", gameContent.getCollectedTargets()); // - We are getting / the collectedTargets
        String scoreText = String.format("Punkte: %d", gameContent.getCollectedScore()); // - We are getting - scoreText / Punkteanzahl
        String timeText = "Zeit: " + String.format("%.2f", getElapsedTime()) + " Sekunden"; // - Verstrichene Zeit
        String timeTextFake = "Zeit: " + String.format("%.2f", 200.0) + " Sekunden"; // - We are getting - some fake time
        String gameOverMsg = "GameOver";
        Rect collectedTextBounds = new Rect(); // - Create Rectangle collectedTextBounds
        scoreAndTimePaint.getTextBounds(collectedText, 0, collectedText.length(), collectedTextBounds);
        Rect scoreTextBounds = new Rect(); // - Create Rectangle scoreTextBounds
        scoreAndTimePaint.getTextBounds(scoreText, 0, scoreText.length(), scoreTextBounds);
        Rect timeTextBounds = new Rect(); // - Create Rectangle timeTextBounds
        scoreAndTimePaint.getTextBounds(timeText, 0, timeText.length(), timeTextBounds); // - Anfangs- und Endpunkt des Textes in Rechteck
        Rect gameOverTextBounds = new Rect(); // - Create Rectangle gameOverTextBounds
        scoreAndTimePaint.getTextBounds(gameOverMsg, 0 , gameOverMsg.length(), gameOverTextBounds); // - Anfangs- und Endpunkt des Textes in Rechteck
        float textWidth = Math.max(scoreAndTimePaint.measureText(timeText), scoreAndTimePaint.measureText(timeTextFake))+10; // -
        textWidth = Math.max(scoreAndTimePaint.measureText(collectedText), textWidth); // - Textgröße vergleichen
        textWidth = Math.max(scoreAndTimePaint.measureText(scoreText), textWidth); // - Textgröße vergleichen
        textWidth = Math.max(scoreAndTimePaint.measureText(gameOverMsg), textWidth);
        canvas.save(); // Canvas wird gespeichert
        canvas.translate(gameWidth - textWidth, scoreTextBounds.height()); // -
        canvas.drawText(collectedText, 0, 0, scoreAndTimePaint); // - Text für collectedTargets wird gezeichnet
        canvas.translate(0, (int) (timeTextBounds.height() * 1.5)); // - Text für
        canvas.drawText(scoreText, 0, 0, scoreAndTimePaint);


        if(gameMode==1) {   // game running
            canvas.translate(0, (int)(timeTextBounds.height()*1.5));
            canvas.drawText(timeText, 0, 0, scoreAndTimePaint);

        }
        if(gameMode == 2){
            canvas.translate(0, (int)(gameOverTextBounds.height()*1.5));
            canvas.drawText(gameOverMsg, 0 , 0, scoreAndTimePaint);
        }
        // TODO
        canvas.restore();
    }

    /**
     * Aktualisiert den Spielzustand; wird von Gameloop aufgerufen
     * @param fracsec Teil einer Sekunde, der seit dem letzten Update vergangen ist
     */

    void updateContent(float fracsec) {
        // -  Sobald Inhalt nicht Leer
        if(gameContent != null)
            // gameContent Referenzvariable wird geupdated -> update erhält fracsec
            gameContent.update(fracsec);
        }

    /**
     * Wird aufgerufen, wenn die Zeichenfläche erzeugt wird
     * @param holder
     */

    // Run Methode wird aufgerufen wenn surface erstellt wird
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Gameloop anwerfen
        // GGf. hier Animationen eibauen und Bitmaps
        gameThread = new Thread(this); // Create Thread
        gameThread.start(); // Start Thread
    }

    /**
     * Wird aufgerufen, wenn sich die Größe der Zeichenfläche ändert
     * Das initiale Festlegen der Größe bewirkt ebenfalls den Aufruf dieser Funktion
     * @param holder Surface Holder
     * @param format Pixelformat
     * @param width Breite in Pixeln
     * @param height Höhe in Pixeln
     */

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        int border = 0;                                                     // darf's ein wenig Rand sein?
        gameWidth = width - border;                                         // hinzufügen
        gameHeight = (int)((float)gameWidth / ((float) width / height));    // Höhe entsprechend anpassen

        // Ermitteln der Größe der einzelnen Elemente
        Pair<Integer, Integer> maxLevelSize = LevelHelper.getLargestLevelDimensions(getContext());
        // minimale Breite hält alle quadratischen Kacheln sichbar im Spielfeld
        TileGraphics.setTileSize(Math.min(gameWidth / maxLevelSize.first,
                gameHeight / maxLevelSize.second));

        gameContent = new GameContent(getContext(), levelName);

        // Reset der Zustände bei "onResume"
        gameOver=false;
        gameMode=0;
    }


    /**
     * Wird am Ende des Lifecycles der Zeichenfläche aufgerufen
     * Ein guter Ort um ggf. Ressourcen freizugeben, Verbindungen
     * zu schließen und die Gameloop und den Time Thread zu beenden
     * @param holder SurfaceHolder
     */

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Gameloop and Time Thread beenden
        runningRenderLoop = false; // Kill thread 1
        runningTimeThread = false; // Kill thread 2
        gameMode=0; // Set gameMode 0
        gameOver=false; //
        gameContent.resetPlayerDirection();

        try {
            gameThread.join();
            if(timeThread != null)  // überhaupt gestartet?
                timeThread.join();
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gameloop, ruft {@link #updateContent(float)} und {@link #updateGraphics(Canvas)} auf
     * und ermittelt die seit dem letzten Schleifendurchlauf vergangene Zeit (wird zum zeitlich
     * korrekten Aktualisieren des Spielzustandes benötigt)
     */

    // Wird gestartet bei GameThread - aktuallisiert Spielzustand - Lädt Spieleinhalte hoch -
    @Override
    public void run() {

        // - while(true) Beendigung aktiviert
        runningRenderLoop = true;

        // - Erster Zeitvermerk - Bei Beginn von Run
        long lastTime = System.currentTimeMillis();

        while(runningRenderLoop) {
            // - Zweiter Zeitvermerk - Bei Beginn von "while true".
            long currentTime = System.currentTimeMillis();
            // - Dritter Zeitvermerk - Veränderung des Zeitvermerk
            long delta = currentTime - lastTime;
            //SekundenRechnung
            float fracsec = (float)delta / 1000f;

            lastTime = currentTime;



            // lockCanvas is especially important for Threads !
            Canvas canvas = surfaceHolder.lockCanvas();
            // Editing pixels on the surface area now possible


            // Has to be empty apparently
            if(canvas == null) continue;

            if(!gameOver)
                // Inhalte hochladen !
                updateContent(fracsec); // kompletten Spielzustand aktualisieren - Pro Sekunde !

            // - Spiel ist beendet unter dieser Bedingung
            if(gameContent!=null && gameContent.getCollectedTargets() >= maxCollectedTargets) {
                gameMode = 2;
                gameOver = true; // Game over
            }


            updateGraphics(canvas); // Zeichne das Spiel bzw. canvas
            // - Surface Pixels will be shown on the screen !
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }




    // - Methode wird bei erster Bewegung onFling also gestartet (Thread wird in der Methode gestartet)
    public void startTimeThread() {

        // - If runningTimeThread false;
        if(runningTimeThread) return;
        runningTimeThread = true; // Set runningTimeThread True
        resetElapsedTime(); // - Verstrichene Zeit zurücksetzen

        // - Starte Thread
        timeThread = new Thread(new Runnable() {
            public void run() { //
                while (runningTimeThread) { // while (true)
                    increaseElapsedTime(0.01); // Erhöhe verstrichene Zeit um 0.01;
                    try {
                        Thread.sleep(10); // Lege den Thread für 10 Milisekunden schalfen
                    } catch (InterruptedException ex) { //
                        runningTimeThread=false;
                    }
                }
            }});
        timeThread.start();
    }




    /**
     * Um den GestureDetector verwenden zu können, müssen die Touch-Events an diesen weitergeleitet werden
     * Hier wäre evtl. eine geeignete Stelle, um Eingaben vorrübergehend
     * (bspw. während Animationen) zu deaktivieren, indem eben dieses Weiterleiten deaktiviert wird
     * @param event Aktuelles {@link MotionEvent}
     * @return true wenn das Event verarbeitet wurde, andernfalls false
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
           if (gestureDetector.onTouchEvent(event))
                return true;
            else
                return super.onTouchEvent(event);
    }

    /**
     * Die Fling-Geste wird genutzt, um die Spielfigur durch den Level zu bewegen.
     * Der eigentliche Move wird dem Gameloop synchron signalisiert und von diesem ausgeführt.
     * @param e1 {@link MotionEvent} welches die Geste gestartet hat (Ursprung)
     * @param e2 {@link MotionEvent} am Ende der Geste (aktuelle Position)
     * @param velocityX Geschwindigkeit der Geste auf der X-Achse (Pixel / Sekunde)
     * @param velocityY Geschwindigkeit der Geste auf der Y-Achse (Pixel / Sekunde)
     * @return true wenn das Event verarbeitet wurde, andernfalls false
     */

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        // Wird der Player aktuell noch animiert, wird der Fling wegkonsumiert
        // (bei uns nur konzeptionell notwendig, da die laufende Animation in GameContent
        // neue Animationen eh abblockt)
        if(!gameContent.isPlayerDirectionIDLE())
            return true;

        // Umrechnung Radian in Grad aufgrund von arc cosine
        float deg = (float) Math.toDegrees(
                Math.acos(velocityX/Math.sqrt(velocityX * velocityX + velocityY * velocityY))
        );
        System.out.println(deg); // Out bei Debugger ist für Bewegung von Player
        if(velocityY > 0)
            deg = 180f + (180f - deg);

        if(deg > 315 || deg < 45)
            gameContent.setPlayerDirection(Direction.RIGHT);
        else if(deg >= 45 && deg <= 135)
            gameContent.setPlayerDirection(Direction.UP);
        else if(deg > 135 && deg < 225)
            gameContent.setPlayerDirection(Direction.LEFT);
        else if(deg >= 225 && deg < 315)
            gameContent.setPlayerDirection(Direction.DOWN);

        // erster Fling startet den Zeitzähler
        gameMode=1;
        // Erste Bewegung start zweiten Thread für die Zeit
        startTimeThread();
        return true;
    }

    /**
     * Wird hier nicht true zurück gegeben, erfolgen keine onFling events
     * @param e {@link MotionEvent} aktuelles Event
     * @return true.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    // Nicht genutzte Gesten
    @Override public void onShowPress(MotionEvent e) {}
    @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
    @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
    @Override public void onLongPress(MotionEvent e) {}
}
