package com.example.projectapplication;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.time.LocalTime;
import java.util.ArrayList;

public class SpaceGameView extends SurfaceView implements Runnable {

    long enemiesDiedTime = 0;
    long meteorsVanishedTime = 0;

    private int numEnemies = 0;
    private ArrayList<Enemy> enemies = new ArrayList<Enemy>();
    private ArrayList<Bullet> enemyBullets = new ArrayList<>();
    Handler handler;
    private Context context;

    // This is our thread
    private Thread gameThread = null;

    // Our SurfaceHolder to lock the surface before we draw our graphics
    private SurfaceHolder ourHolder;

    // A boolean which we will set and unset
    // when the game is running- or not.
    private volatile boolean playing;

    // Game is paused at the start
    private boolean paused = true;

    // A Canvas and a Paint object
    private Canvas canvas;
    private Paint paint;

    // This variable tracks the game frame rate
    private long fps;

    // This is used to help calculate the fps
    private long timeThisFrame;
    private long lastCollision;
    // The size of the screen in pixels
    private int screenX;
    private int screenY;

    // The score
    public int score = 0;

    // Lives
    private int lives = 200;

    long lastTime = 0;
    private Spaceship spaceShip;
    private ArrayList<Bullet> bulletList = new ArrayList<Bullet>();
    private ArrayList<Bullet> bossBulletList = new ArrayList<Bullet>();
    private Bitmap bitmapback;

    private ArrayList<Explosion> explosionArrayList = new ArrayList<Explosion>();
    private Boss boss;

    int level = 0;

//Finns
    private ArrayList<Meteor> meteors;

    static int dWidth, dHeight;

    int totalMeteors;
    boolean meteorIsActive;
    MediaPlayer mediaPlayer;
    MediaPlayer musicPlayer;
    // This special constructor method runs
    public SpaceGameView(Context context, int x, int y) {

        // The next line of code asks the
        // SurfaceView class to set up our object.
        // How kind.
        super(context);

        // Make a globally available copy of the context so we can use it in another method
        this.context = context;
        // Initialize ourHolder and paint objects
        ourHolder = getHolder();
        paint = new Paint();
        screenX = x;
        screenY = y;
        meteors = new ArrayList<>();

        //Finns
        Display display =  ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        dWidth = point.x;
        dHeight = point.y;
        totalMeteors = 0;
        meteorIsActive = false;
        level = 1;
        playMusic();
        initLevel();
    }
    public void playMusic(){
            musicPlayer = MediaPlayer.create(context, R.raw.skyfire);
            musicPlayer.start();
            musicPlayer.setLooping(true);
            musicPlayer.setVolume((float)0.4,(float)0.4);
    }
public void playExplosion(){
        mediaPlayer = MediaPlayer.create(context, R.raw.explosion);
        mediaPlayer.start();
}
public void playShoot(){
    mediaPlayer = MediaPlayer.create(context, R.raw.alienshoot1);
    mediaPlayer.start();
}
    private void initLevel() {

        spaceShip = new Spaceship(context, screenX, screenY);
        boss = new Boss(context, screenX, screenY);
        //bulletList.add(new Bullet(context, screenY, screenX));
        //finns
        for (int i=0;i<5;i++) {
            Meteor meteor = new Meteor(context);
            meteors.add(meteor);
        }

        enemies = new ArrayList<>();
        // screenX / length of two enemies
        int numColumns = screenX / (2 * screenX / 10);
        // Create the enemies and add them to the list
        for (int row = 0; row < 1; row++) {
            for (int column = 0; column < numColumns; column++) {
                Enemy enemy = new Enemy(getContext(), row, column, screenX, screenY);
                enemies.add(enemy);
            }
        }

    }

    @Override
    public void run() {
        while (playing) {
            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();
            // Update the frame
            if (!paused) {
                if (level != 2){
                    shoot();
                }
                update();

                if (level == 2){
                    if (enemies.isEmpty() && System.currentTimeMillis() > enemiesDiedTime + 5000) {
                        startMeteorShower(10);
                    }
                }
                if (level ==3){
                    if (meteors.isEmpty() && System.currentTimeMillis() > meteorsVanishedTime + 10000) {
                        boss();
                        if (boss != null) {
                            boss.shoot(bossBulletList, context, spaceShip);
                            boss.move(spaceShip);
                        }
                        else {
                            win();
                        }
                    }
                }
            }
            // Draw the frame
            draw();

            // Calculate the fps this frame
            // We can then use the result to
            // time animations and more.
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    private void update() {

        spaceShip.update(fps);


        for (int i = 0; i < bossBulletList.size(); i++) {
            if (bossBulletList.get(i).getStatus())
                bossBulletList.get(i).update(fps);
        }

        for (int i = 0; i < bulletList.size(); i++) {
            if (bulletList.get(i).getStatus())
                bulletList.get(i).update(fps);
        }


        //Update enemybulletList
        for (int i = 0; i < enemyBullets.size(); i++) {
            if (enemyBullets.get(i).getStatus())
                enemyBullets.get(i).update(fps);
        }

        // update enemy movement
        boolean hit = false;


        for (int i = 0; i < enemies.size(); i++){
            Enemy enemy = enemies.get(i);
            if (enemies.size() <= 5){
                enemy.enterRageMode();
            }

            enemy.dropBullet(enemyBullets, context, spaceShip , fps);
            enemy.move(fps);
            if (enemy.hitsBorder(fps)){
                hit = true;
            };
        }
        // System.out.print(hit);
        // change direction if screenHit
       if (hit) {
            Enemy.reduceBulletFrequency();
            for (int i = 0; i < enemies.size(); i++){
                Enemy enemy = enemies.get(i);
                enemy.changeDirection();
                enemy.moveDown();
            }
            hit = false;
        }

        if (boss != null) {
            boss.update();
        }

        checkCollisions();
        int i=0;
        while( i<explosionArrayList.size()){
            explosionArrayList.get(i).update();
            if (explosionArrayList.get(i).getCurrentFrame()>63) {
                explosionArrayList.remove(i);
                i--;
            }
            i++;
        }
    }

    private void checkCollisions() {

        if (boss != null && boss.isActive()) {
            if (LocalTime.now().toNanoOfDay() / 1000000 - lastCollision >= 1000) {
                if (RectangleCollison(spaceShip.getActualRect(), boss.getActualRect())) {
                    lives--;
                    if (lives == 0) {
                        // Game over
                        gameOver();
                    }
                    lastCollision = LocalTime.now().toNanoOfDay() / 1000000;
                }
            }
        }


        if (level == 1) {
            if (System.currentTimeMillis() - lastCollision >= 1000) {
                for (int i = 0; i < enemies.size(); i++){
                    Enemy enemy = enemies.get(i);
                    if (spaceShip.getActualRect().intersect(enemy.getActualRect())){
                        lives--;
                        if (lives == 0) {
                            // Game over
                            gameOver();
                        }
                        enemies.remove(enemy);
                        if (enemies.isEmpty()){
                            enemiesDiedTime = System.currentTimeMillis();
                            level = 2;
                        }
                        lastCollision = LocalTime.now().toNanoOfDay() / 1000000;
                    }
                }
            }
        }

        int i = 0;
        while (i < bossBulletList.size()) {
            if (RectangleCollison(bossBulletList.get(i).getActualRect(), spaceShip.getActualRect())) {
                lives--;
                if (lives == 0) {
                    // Game over
                    gameOver();
                }
                bossBulletList.remove(i);
                continue;
            }
            if (bossBulletList.get(i).getImpactPointY() < 0) {
                bossBulletList.remove(i);
                continue;
            }
            if (bossBulletList.get(i).getImpactPointY() > screenY) {
                bossBulletList.remove(i);
                continue;
            }
            if (bossBulletList.get(i).getImpactPointX() < 0) {
                bossBulletList.remove(i);
                continue;
            }
            if (bossBulletList.get(i).getImpactPointX() > screenX) {
                bossBulletList.remove(i);
                continue;
            }
            i++;
        }
        i = 0;

        /*
        while (i < bulletList.size()) {
            if (boss != null) {
                if (RectangleCollison(bulletList.get(i).getActualRect(), boss.getActualRect()) && boss.isActive()) {
                    boss.setHp(boss.getHp() - 20);
                    bulletList.remove(i);
                    continue;
                }
            }

            if (bulletList.get(i).getImpactPointY() < 0) {
                bulletList.remove(i);
                continue;
            }
            if (bulletList.get(i).getImpactPointY() > screenY) {
                bulletList.remove(i);
                continue;
            }
            if (bulletList.get(i).getImpactPointX() < 0) {
                bulletList.remove(i);
                continue;
            }
            if (bulletList.get(i).getImpactPointX() > screenX) {
                bulletList.remove(i);
                continue;
            }
        }

        i++; */
        i = 0;
        while ( i < bulletList.size()) {
            if (boss != null) {
                if (RectangleCollison(bulletList.get(i).getActualRect(), boss.getActualRect()) && boss.isActive()) {
                    boss.setHp(boss.getHp() - 20);
                    if (boss.getHp()<0.1* boss.getMaxhp()){
                        explosionArrayList.add(new Explosion(context, (int)(bulletList.get(i).getActualRect().left-64),(int)(bulletList.get(i).getActualRect().top-64)));
                        playExplosion();
                    }
                    bulletList.remove(i);
                    continue;
                }
            }
               Bullet bullet = bulletList.get(i);
                if (bulletList.get(i).getStatus()) {
                    RectF bulletRect = bulletList.get(i).getActualRect();
                    int j = 0;
                    while (j < enemies.size()) {
                        Enemy enemy = enemies.get(j);
                        if (enemy.getStatus()) {
                            RectF enemyRect = enemy.getActualRect();
                            if (RectF.intersects(bullet.getActualRect(), enemyRect)) {
                                explosionArrayList.add(new Explosion(context, (int)(enemyRect.left),(int)(enemyRect.top)));
                                playExplosion();
                                // Bullet and enemy have collided
                                bullet.setInactive();
                                enemies.get(j).setInactive();
                                bulletList.remove(bullet);
                                enemies.remove(enemy);
                                if (enemies.isEmpty()){
                                    enemiesDiedTime = System.currentTimeMillis();
                                    level = 2;
                                }
                                score += 10;
                                continue;
                            }
                        }
                        j++;
                    }
                    // Check if bullet has gone out of the screen
                    if (bullet.getActualRect().bottom < 0) {
                        bullet.setInactive();
                        bulletList.remove(bullet);
                        continue;
                    }
                    if (bullet.getImpactPointY() < 0) {
                        bullet.setInactive();
                        bulletList.remove(bullet);
                        continue;
                    }
                    if (bullet.getImpactPointX() > screenX) {
                        bullet.setInactive();
                        bulletList.remove(bullet);
                        continue;
                    }
                    if (bullet.getImpactPointX() < 0) {
                        bullet.setInactive();
                        bulletList.remove(bullet);
                        continue;
                    }
                }
                i++;
            }

        i =0;
        // Check for enemy bullet collisions with spaceship
        while( i < enemyBullets.size()) {
            Bullet enemyBullet = enemyBullets.get(i);
            if (enemyBullet.getStatus()) {
                RectF enemyBulletRect = enemyBullet.getActualRect();
                if (RectF.intersects(enemyBulletRect, spaceShip.getActualRect())) {
                    // Enemy bullet has hit the spaceship
                    enemyBullet.setInactive();
                    enemyBullets.remove(enemyBullet);
                    lives--;
                    if (lives == 0) {
                        // Game over
                        gameOver();
                    }
                }
                // Check if enemy bullet has gone out of the screen
                if (enemyBulletRect.top > screenY) {
                    enemyBullet.setInactive();
                    enemyBullets.remove(enemyBullet);
                }
            }
            i++;
        }
    }
private void boss(){
        if (boss != null) {
            boss.setActive(true);
            if (boss.getHp() < 0) {
                boss = null;
                winGame();
            }
        }
}
private void drawdebug() {
    paint.setColor(Color.WHITE);
    for (int i = 0; i < bulletList.size(); i++) {
        if (bulletList.get(i).getStatus())
            canvas.drawRect(bulletList.get(i).getActualRect(), paint);
        canvas.drawBitmap(bulletList.get(i).getBitmapBullet(), bulletList.get(i).getRect().left, bulletList.get(i).getRect().top, paint);

    }
    for (int i = 0; i < bossBulletList.size(); i++) {
        if (bossBulletList.get(i).getStatus())
            canvas.drawRect(bossBulletList.get(i).getActualRect(), paint);
        canvas.drawBitmap(bossBulletList.get(i).getBitmapBullet(), bossBulletList.get(i).getRect().left, bossBulletList.get(i).getRect().top, paint);
    }
    if (boss != null) {
        canvas.drawRect(boss.getActualRect(), paint);
        canvas.drawBitmap(boss.getCurrentBitmap(), boss.getX(), boss.getY(), paint);
    }


    for (int i = 0; i < enemies.size(); i++) {
        if (enemies.get(i).getStatus())
            canvas.drawRect(enemies.get(i).getActualRect(), paint);
        canvas.drawBitmap(enemies.get(i).getCurrentBitmap(), enemies.get(i).getX(), enemies.get(i).getY(), paint);
    }

    canvas.drawRect(spaceShip.getActualRect(), paint);
    canvas.drawBitmap(spaceShip.getBitmap(), spaceShip.getX(), spaceShip.getY(), paint);

}
    public void gameOver(){
        paused = true;
        handler = null;
        Intent intent = new Intent(context, GameOverScene.class);
        intent.putExtra("score", score);
        context.startActivity(intent);
        ((Activity) context).finish();
        System.out.print("Game Over");
    }

    public void win(){
        Log.d("App", "You win");
        playing = false;
    };


    public void winGame(){
        paused = true;
        handler = null;
        Intent intent = new Intent(context, WinnerScene.class);
        intent.putExtra("score", score);
        context.startActivity(intent);
        ((Activity) context).finish();
        System.out.print("Winner!");
    }

    private void draw() {
        // Make sure our drawing surface is valid or we crash
        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            // Draw the background color
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            // Choose the brush color for drawing
            paint.setColor(Color.argb(255, 255, 255, 255));

            bitmapback = BitmapFactory.decodeResource(context.getResources(), R.drawable.sprite);
            bitmapback = Bitmap.createScaledBitmap(bitmapback, (int) (screenX), (int) (screenY), false);

            //  canvas.drawBitmap(background.getBitmap(), spaceShip.getX(), spaceShip.getY() , paint);
            //  draw the defender bullets
            canvas.drawBitmap(bitmapback, 0,  0, paint);

            for (int i = 0; i < bulletList.size(); i++) {
                if (bulletList.get(i).getStatus())
                    canvas.drawBitmap(bulletList.get(i).getBitmapBullet(), bulletList.get(i).getRect().left, bulletList.get(i).getRect().top, paint);
            }

            for (int i = 0; i < enemyBullets.size(); i++) {
                if (enemyBullets.get(i).getStatus())
                    canvas.drawBitmap(enemyBullets.get(i).getBitmapBullet(), enemyBullets.get(i).getRect().left, enemyBullets.get(i).getRect().top, paint);
            }
            if (!meteors.isEmpty() && meteorIsActive == true) {
                for (int i=0;i<meteors.size();i++) {
                    canvas.drawBitmap(meteors.get(i).getMeteor(), meteors.get(i).meteorX, meteors.get(i).meteorY, null);
                }
            }

            canvas.drawBitmap(spaceShip.getBitmap(), spaceShip.getX(), spaceShip.getY(), paint);

            // draw all enemies
            for (Enemy enemy : enemies) {
                if (enemy.isActive()) {
                    canvas.drawBitmap(enemy.getCurrentBitmap(), enemy.getX(), enemy.getY(), paint);
                }
            }
            for (int i =0; i< explosionArrayList.size(); i++){
                canvas.drawBitmap(explosionArrayList.get(i).getCurrentBitmap(),explosionArrayList.get(i).getX(),explosionArrayList.get(i).getY(), paint);
            }
            for (int i = 0; i < bossBulletList.size(); i++) {
                if (bossBulletList.get(i).getStatus())
                    canvas.drawBitmap(bossBulletList.get(i).getBitmapBullet(), bossBulletList.get(i).getRect().left, bossBulletList.get(i).getRect().top, paint);
            }
            if (boss != null && boss.isActive()) {
                canvas.drawBitmap(boss.getCurrentBitmap(), boss.getX(), boss.getY(), paint);
                boss.drawHealthBar(canvas,paint);
            }

            canvas.drawBitmap(spaceShip.getBitmap(), spaceShip.getX(), spaceShip.getY(), paint);

            //drawdebug();
            // Draw the score and remaining lives
            // Change the brush color
            paint.setColor(Color.argb(255, 249, 129, 0));
            paint.setTextSize(40);
            canvas.drawText("Score: " + score + "   Lives: " + lives + "    FPS: " + fps, 10, 50, paint);

            // Draw everything to the screen
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void startMeteorShower (int maxMeteors) {

        meteorIsActive = true;
        for (int i = 0; i < meteors.size(); i++) {

            meteors.get(i).meteorY += meteors.get(i).meteorSpeed;
            meteors.get(i).meteorX += meteors.get(i).meteorOffset;
            if (meteors.get(i).meteorY >= screenY ||
                    meteors.get(i).meteorX > 1000 ||
                    meteors.get(i).meteorX < -200) {
                meteors.get(i).resetPosition();
                score+=10;
                if(totalMeteors > maxMeteors - meteors.size() -1) {
                    meteors.remove(i);
                }

                totalMeteors++;
            }
        }

        for (int i=0;i< meteors.size();i++) {
            if (meteors.get(i).meteorSpaceshipDistance(meteors.get(i), spaceShip) <= meteors.get(i).getMeteorWidth() / 2) {
                lives--;
                meteors.get(i).resetPosition();
            }
        }

        if (lives == 0) {
            gameOver();
        }

        if (meteors.isEmpty()) {
            meteorIsActive = false;
            meteorsVanishedTime = System.currentTimeMillis();
            level = 3;
        }
    }

    public void shoot() {
        if (LocalTime.now().toNanoOfDay() / 1000000 - lastTime >= 1000) {
            bulletList.add(new Bullet(context, screenY, screenX,0,spaceShip.getX()+( spaceShip.getLength() /6)
                    , spaceShip.getY() + spaceShip.getHeight() / 2));
            bulletList.get(bulletList.size() - 1).shoot();
            playShoot();
            lastTime = LocalTime.now().toNanoOfDay() / 1000000;
        }
    }

    // If SpaceGameActivity is paused/stopped
    // shutdown our thread.
    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }
    }

    // If SpaceInvadersActivity is started then
    // start our thread.
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            // Player has touched the screen
            case MotionEvent.ACTION_MOVE:
                paused = false;
                //Spaceship follows touch input

                spaceShip.setX((int)
                        (motionEvent.getX() - spaceShip.getLength() /2));
                spaceShip.setY((int)
                        (motionEvent.getY() - spaceShip.getHeight()/2));

                break;
            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:

                break;
        }
        return true;
    }

public boolean RectangleCollison(RectF rect1, RectF rect2){
        if (rect1.left < rect2.right
                && rect1.right > rect2.left &&
        rect1.top < rect2.bottom && rect1.bottom> rect2.top){
            return true;
        }
        else return false;
}
        }  // end class