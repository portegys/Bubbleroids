/************************************************************************************************
*
*  Bubbleroids.java
*
*  Usage:
*
*  <applet code="Bubbleroids" archive="Bubbleroids.jar" width=500 height=400></applet>
*
*  Keyboard Controls:
*
*  S            - Start Game    P           - Pause Game
*  Cursor Left  - Rotate Left   Cursor Up   - Fire Thrusters
*  Cursor Right - Rotate Right  Cursor Down - Fire Retro Thrusters
*  Spacebar     - Fire Cannon   H           - Hyperspace
*  M            - Toggle Sound  D           - Toggle Graphics Detail
*  U            - Launch UFO
*
************************************************************************************************/

import java.awt.*;
import java.net.*;
import java.util.*;
import java.applet.Applet;
import java.applet.AudioClip;
import javax.swing.*;

/************************************************************************************************
*  The BubbleroidsSprite class defines a game object, including it's shape, position, movement and
*  rotation. It also can determine if two objects collide.
************************************************************************************************/

class BubbleroidsSprite {
   // Fields:

   static int width, width2;     // Dimensions of the graphics area.
   static int height, height2;

   Polygon shape;                // Initial sprite shape, centered at the origin (0,0).
   double  radius;               // Radius of bubbleroid sprites.
   boolean active;               // Active flag.
   double  angle;                // Current angle of rotation.
   double  deltaAngle;           // Amount to change the rotation angle.
   double  currentX, currentY;   // Current position on screen.
   double  deltaX, deltaY;       // Amount to change the screen position.
   Color   color;                // Color.
   Polygon sprite;               // Final location and shape of sprite after applying rotation and
                                 // moving to screen position. Used for drawing on the screen and
                                 // in detecting collisions.

   // Constructors:

   public BubbleroidsSprite()
   {
      this.shape      = new Polygon();
      this.radius     = -1.0;
      this.active     = false;
      this.angle      = 0.0;
      this.deltaAngle = 0.0;
      this.currentX   = 0.0;
      this.currentY   = 0.0;
      this.deltaX     = 0.0;
      this.deltaY     = 0.0;
      this.color      = Color.white;
      this.sprite     = new Polygon();
   }


   // Methods:

   public void advance()
   {
      // Update the rotation and position of the sprite based on the delta values. If the sprite
      // moves off the edge of the screen, it is wrapped around to the other side.

      this.angle += this.deltaAngle;
      if (this.angle < 0)
      {
         this.angle += 2 * Math.PI;
      }
      if (this.angle > 2 * Math.PI)
      {
         this.angle -= 2 * Math.PI;
      }
      this.currentX += this.deltaX;
      if (this.currentX < -width2)
      {
         this.currentX += width;
      }
      if (this.currentX > width2)
      {
         this.currentX -= width;
      }
      this.currentY -= this.deltaY;
      if (this.currentY < -height2)
      {
         this.currentY += height;
      }
      if (this.currentY > height2)
      {
         this.currentY -= height;
      }
   }


   public void render()
   {
      int i;

      // Render the sprite's shape and location by rotating it's base shape and moving it to
      // it's proper screen position.

      this.sprite = new Polygon();
      for (i = 0; i < this.shape.npoints; i++)
      {
         this.sprite.addPoint((int)Math.round(this.shape.xpoints[i] * Math.cos(this.angle) + this.shape.ypoints[i] * Math.sin(this.angle)) + (int)Math.round(this.currentX) + width2,
                              (int)Math.round(this.shape.ypoints[i] * Math.cos(this.angle) - this.shape.xpoints[i] * Math.sin(this.angle)) + (int)Math.round(this.currentY) + height2);
      }
   }


   public boolean isColliding(BubbleroidsSprite s)
   {
      int i;

      // Determine if one sprite overlaps with another, i.e., if any vertice
      // of one sprite lands inside the other.

      for (i = 0; i < s.sprite.npoints; i++)
      {
         if (this.sprite.inside(s.sprite.xpoints[i], s.sprite.ypoints[i]))
         {
            return(true);
         }
      }
      for (i = 0; i < this.sprite.npoints; i++)
      {
         if (s.sprite.inside(this.sprite.xpoints[i], this.sprite.ypoints[i]))
         {
            return(true);
         }
      }
      return(false);
   }
}

/************************************************************************************************
*  Main applet code.
************************************************************************************************/

public class Bubbleroids extends Applet implements Runnable {
   // Thread control variables.

   Thread loadThread;
   Thread loopThread;

   // Constants

   static final int DELAY = 50;            // Milliseconds between screen updates.

   static final int MAX_SHIPS = 3;         // Starting number of ships per game.

   static final int MAX_SHOTS   = 6;       // Maximum number of sprites for photons,
   static final int MAX_BUBBLES = 8;       // bubbleroids and explosions.
   static final int MAX_SCRAP   = 20;

   static final int SCRAP_COUNT = 30;       // Counter starting values.
   static final int HYPER_COUNT = 60;
   static final int STORM_PAUSE = 30;
   static final int UFO_PASSES  = 3;

   static final int NUM_BUBBLE_SIDES = 12;    // Bubbleroid shape and size ranges.
   static final int MIN_BUBBLE_SIZE  = 20;
   static final int MAX_BUBBLE_SIZE  = 40;
   static final int MIN_BUBBLE_SPEED = 2;
   static final int MAX_BUBBLE_SPEED = 12;

   static final int BIG_POINTS    = 25;     // Points for shooting different objects.
   static final int SMALL_POINTS  = 50;
   static final int UFO_POINTS    = 250;
   static final int MISSLE_POINTS = 500;

   static final int NEW_SHIP_POINTS = 5000; // Number of points needed to earn a new ship.
   static final int NEW_UFO_POINTS  = 2750; // Number of points between flying saucers.

   // Space (double buffered).

   Canvas    space;
   Dimension spaceDimension;
   Graphics  spaceGraphics;
   Image     spaceImage;
   Graphics  spaceImageGraphics;

   // Background stars.

   int numStars;
   Point[] stars;

   // Game data.

   int score;
   int highScore;
   int newShipScore;
   int newUfoScore;

   boolean loaded = false;
   boolean paused;
   boolean playing;
   boolean sound;
   boolean detail;

   // Control panel.

   Panel    controls;
   Checkbox startQuit;
   Checkbox pauseCheck;
   Checkbox muteCheck;

   // Key flags.

   boolean left  = false;
   boolean right = false;
   boolean up    = false;
   boolean down  = false;

   // Sprite objects.

   BubbleroidsSprite ship;
   BubbleroidsSprite ufo;
   BubbleroidsSprite missile;
   BubbleroidsSprite[] photons     = new BubbleroidsSprite[MAX_SHOTS];
   BubbleroidsSprite[] bubbleroids = new BubbleroidsSprite[MAX_BUBBLES];
   BubbleroidsSprite[] explosions  = new BubbleroidsSprite[MAX_SCRAP];

   // Ship data.

   int shipsLeft;      // Number of ships left to play, including current one.
   int shipCounter;    // Time counter for ship explosion.
   int hyperCounter;   // Time counter for hyperspace.

   // Photon data.

   int[] photonCounter = new int[MAX_SHOTS];   // Time counter for life of a photon.
   int photonIndex;                            // Next available photon sprite.

   // Flying saucer data.

   int ufoPassesLeft;   // Number of flying saucer passes.
   int ufoCounter;      // Time counter for each pass.

   // Missile data.

   int missileCounter;   // Counter for life of missile.

   // Bubbleroid data.

   boolean[] bubbleroidIsSmall = new boolean[MAX_BUBBLES]; // Bubbleroid size flag.
   int bubbleroidsCounter;                                 // Break-time counter.
   int bubbleroidsSpeed;                                   // Bubbleroid speed.
   int bubbleroidsLeft;                                    // Number of active bubbleroids.

   // Explosion data.

   int[] explosionCounter = new int[MAX_SCRAP]; // Time counters for explosions.
   int explosionIndex;                          // Next available explosion sprite.

   // Sound clips.

   AudioClip crashSound;
   AudioClip explosionSound;
   AudioClip fireSound;
   AudioClip missileSound;
   AudioClip saucerSound;
   AudioClip thrustersSound;
   AudioClip warpSound;

   // Flags for looping sound clips.

   boolean thrustersPlaying;
   boolean saucerPlaying;
   boolean missilePlaying;

   // Font data.

   Font        font = new Font("Helvetica", Font.BOLD, 12);
   FontMetrics fm;
   int         fontWidth;
   int         fontHeight;

   // Applet information.

   public String getAppletInfo()
   {
      return("Bubbleroids by Tom Portegys");
   }


   public void init()
   {
      Graphics  g;
      Dimension d;
      int       i;

      // Find the size of the screen and set the values for sprites.

      g        = getGraphics();
      d        = size();
      d.height = (int)((double)d.height * .9);
      BubbleroidsSprite.width   = d.width;
      BubbleroidsSprite.width2  = d.width / 2;
      BubbleroidsSprite.height  = d.height;
      BubbleroidsSprite.height2 = d.height / 2;

      // Create space.

      space          = new Canvas();
      spaceDimension = d;
      space.setBounds(0, 0, d.width, d.height);
      add(space);
      spaceGraphics      = space.getGraphics();
      spaceImage         = createImage(d.width, d.height);
      spaceImageGraphics = spaceImage.getGraphics();

      // Control panel.

      controls = new Panel();
      add(controls);
      startQuit = new Checkbox("Start");
      controls.add(startQuit);
      pauseCheck = new Checkbox("Pause");
      controls.add(pauseCheck);
      muteCheck = new Checkbox("Mute");
      controls.add(muteCheck);

      // Generate starry background.

      numStars = BubbleroidsSprite.width * BubbleroidsSprite.height / 5000;
      stars    = new Point[numStars];
      for (i = 0; i < numStars; i++)
      {
         stars[i] = new Point((int)(Math.random() * BubbleroidsSprite.width), (int)(Math.random() * BubbleroidsSprite.height));
      }

      // Create shape for the ship sprite.

      ship = new BubbleroidsSprite();
      ship.shape.addPoint(0, -10);
      ship.shape.addPoint(7, 10);
      ship.shape.addPoint(-7, 10);

      // Create shape for the photon sprites.

      for (i = 0; i < MAX_SHOTS; i++)
      {
         photons[i] = new BubbleroidsSprite();
         photons[i].shape.addPoint(1, 1);
         photons[i].shape.addPoint(1, -1);
         photons[i].shape.addPoint(-1, 1);
         photons[i].shape.addPoint(-1, -1);
      }

      // Create shape for the flying saucer.

      ufo = new BubbleroidsSprite();
      ufo.shape.addPoint(-15, 0);
      ufo.shape.addPoint(-10, -5);
      ufo.shape.addPoint(-5, -5);
      ufo.shape.addPoint(-5, -9);
      ufo.shape.addPoint(5, -9);
      ufo.shape.addPoint(5, -5);
      ufo.shape.addPoint(10, -5);
      ufo.shape.addPoint(15, 0);
      ufo.shape.addPoint(10, 5);
      ufo.shape.addPoint(-10, 5);

      // Create shape for the guided missile.

      missile = new BubbleroidsSprite();
      missile.shape.addPoint(0, -4);
      missile.shape.addPoint(1, -3);
      missile.shape.addPoint(1, 3);
      missile.shape.addPoint(2, 4);
      missile.shape.addPoint(-2, 4);
      missile.shape.addPoint(-1, 3);
      missile.shape.addPoint(-1, -3);

      // Create bubbleroid sprites.

      for (i = 0; i < MAX_BUBBLES; i++)
      {
         bubbleroids[i] = new BubbleroidsSprite();
      }

      // Create explosion sprites.

      for (i = 0; i < MAX_SCRAP; i++)
      {
         explosions[i] = new BubbleroidsSprite();
      }

      // Set font data.

      g.setFont(font);
      fm         = g.getFontMetrics();
      fontWidth  = fm.getMaxAdvance();
      fontHeight = fm.getHeight();

      // Initialize game data and put us in 'game over' mode.

      highScore = 0;
      sound     = true;
      detail    = true;
      initGame();
      endGame();
   }


   public void initGame()
   {
      // Initialize game data and sprites.

      score            = 0;
      shipsLeft        = MAX_SHIPS;
      bubbleroidsSpeed = MIN_BUBBLE_SPEED;
      newShipScore     = NEW_SHIP_POINTS;
      newUfoScore      = NEW_UFO_POINTS;
      initShip();
      initPhotons();
      stopUfo();
      stopMissile();
      initBubbleroids();
      initExplosions();
      playing = true;
      paused  = false;
      pauseCheck.setState(false);
   }


   public void endGame()
   {
      // Stop ship, flying saucer, guided missile and associated sounds.

      playing = false;
      stopShip();
      stopUfo();
      stopMissile();
   }


   public void start()
   {
      if (loopThread == null)
      {
         loopThread = new Thread(this);
         loopThread.start();
      }
      if (!loaded && (loadThread == null))
      {
         loadThread = new Thread(this);
         loadThread.start();
      }
   }


   public void stop()
   {
      if (loopThread != null)
      {
         loopThread.stop();
         loopThread = null;
      }
      if (loadThread != null)
      {
         loadThread.stop();
         loadThread = null;
      }
   }


   public void run()
   {
      int  i, j;
      long startTime;

      // Lower this thread's priority and get the current time.

      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      startTime = System.currentTimeMillis();

      // Run thread for loading sounds.

      if (!loaded && (Thread.currentThread() == loadThread))
      {
         loadSounds();
         loaded = true;
         loadThread.stop();
      }

      // This is the main loop.

      while (Thread.currentThread() == loopThread)
      {
         if (!paused)
         {
            // Move and process all sprites.

            updateShip();
            updatePhotons();
            updateUfo();
            updateMissile();
            updateBubbleroids();
            updateExplosions();

            // Check the score and advance high score, add a new ship or start the flying
            // saucer as necessary.

            if (score > highScore)
            {
               highScore = score;
            }
            if (score > newShipScore)
            {
               newShipScore += NEW_SHIP_POINTS;
               shipsLeft++;
            }
            if (playing && (score > newUfoScore) && !ufo.active)
            {
               newUfoScore  += NEW_UFO_POINTS;
               ufoPassesLeft = UFO_PASSES;
               initUfo();
            }

            // If all bubbleroids have been destroyed create a new batch.

            if (bubbleroidsLeft <= 0)
            {
               if (--bubbleroidsCounter <= 0)
               {
                  initBubbleroids();
               }
            }
         }

         // Update the screen and set the timer for the next loop.

         repaint();
         try {
            startTime += DELAY;
            Thread.sleep(Math.max(0, startTime - System.currentTimeMillis()));
         }
         catch (InterruptedException e) {
            break;
         }
      }
   }


   public void loadSounds()
   {
      crashSound     = loadSound("crash.au");
      explosionSound = loadSound("explosion.au");
      fireSound      = loadSound("fire.au");
      missileSound   = loadSound("missile.au");
      saucerSound    = loadSound("saucer.au");
      thrustersSound = loadSound("thrusters.au");
      warpSound      = loadSound("warp.au");
   }


   // Load sound clip by playing and immediately stopping it.
   public AudioClip loadSound(String soundFile)
   {
      AudioClip audioClip = null;

      try
      {
         URL url = Bubbleroids.class .getResource(soundFile);

         if (url != null)
         {
            audioClip = Applet.newAudioClip(url);
         }

         if (audioClip == null)
         {
            showStatus("Cannot load sound file " + soundFile);
         }
         else
         {
            audioClip.play();
            audioClip.stop();
         }
      }
      catch (Exception e) {
         showStatus("Cannot load sound file " + soundFile + ": " + e.toString());
      }

      return(audioClip);
   }


   public void initShip()
   {
      ship.active     = true;
      ship.angle      = 0.0;
      ship.deltaAngle = 0.0;
      ship.currentX   = 0.0;
      ship.currentY   = 0.0;
      ship.deltaX     = 0.0;
      ship.deltaY     = 0.0;
      ship.render();
      if (loaded)
      {
         thrustersSound.stop();
      }
      thrustersPlaying = false;

      hyperCounter = 0;
   }


   public void updateShip()
   {
      double dx, dy, limit;

      if (!playing)
      {
         return;
      }

      // Rotate the ship if left or right cursor key is down.

      if (left)
      {
         ship.angle += Math.PI / 16.0;
         if (ship.angle > 2 * Math.PI)
         {
            ship.angle -= 2 * Math.PI;
         }
      }
      if (right)
      {
         ship.angle -= Math.PI / 16.0;
         if (ship.angle < 0)
         {
            ship.angle += 2 * Math.PI;
         }
      }

      // Fire thrusters if up or down cursor key is down. Don't let ship go past
      // the speed limit.

      dx    = -Math.sin(ship.angle);
      dy    = Math.cos(ship.angle);
      limit = 0.8 * MIN_BUBBLE_SIZE;
      if (up)
      {
         if ((ship.deltaX + dx > -limit) && (ship.deltaX + dx < limit))
         {
            ship.deltaX += dx;
         }
         if ((ship.deltaY + dy > -limit) && (ship.deltaY + dy < limit))
         {
            ship.deltaY += dy;
         }
      }
      if (down)
      {
         if ((ship.deltaX - dx > -limit) && (ship.deltaX - dx < limit))
         {
            ship.deltaX -= dx;
         }
         if ((ship.deltaY - dy > -limit) && (ship.deltaY - dy < limit))
         {
            ship.deltaY -= dy;
         }
      }

      // Move the ship. If it is currently in hyperspace, advance the countdown.

      if (ship.active)
      {
         ship.advance();
         ship.render();
         if (hyperCounter > 0)
         {
            hyperCounter--;
         }
      }

      // Ship is exploding, advance the countdown or create a new ship if it is
      // done exploding. The new ship is added as though it were in hyperspace.
      // (This gives the player time to move the ship if it is in imminent danger.)
      // If that was the last ship, end the game.

      else
      if (--shipCounter <= 0)
      {
         if (shipsLeft > 0)
         {
            initShip();
            hyperCounter = HYPER_COUNT;
         }
         else
         {
            endGame();
            startQuit.setLabel("Start");
         }
      }
   }


   public void stopShip()
   {
      ship.active = false;
      shipCounter = SCRAP_COUNT;
      if (shipsLeft > 0)
      {
         shipsLeft--;
      }
      if (loaded)
      {
         thrustersSound.stop();
      }
      thrustersPlaying = false;
   }


   public void initPhotons()
   {
      int i;

      for (i = 0; i < MAX_SHOTS; i++)
      {
         photons[i].active = false;
         photonCounter[i]  = 0;
      }
      photonIndex = 0;
   }


   public void updatePhotons()
   {
      int i;

      // Move any active photons. Stop it when its counter has expired.

      for (i = 0; i < MAX_SHOTS; i++)
      {
         if (photons[i].active)
         {
            photons[i].advance();
            photons[i].render();
            if (--photonCounter[i] < 0)
            {
               photons[i].active = false;
            }
         }
      }
   }


   public void initUfo()
   {
      double temp;

      // Randomly set flying saucer at left or right edge of the screen.

      ufo.active   = true;
      ufo.currentX = -BubbleroidsSprite.width / 2;
      ufo.currentY = Math.random() * BubbleroidsSprite.height;
      ufo.deltaX   = MIN_BUBBLE_SPEED + Math.random() * (MAX_BUBBLE_SPEED - MIN_BUBBLE_SPEED);
      if (Math.random() < 0.5)
      {
         ufo.deltaX   = -ufo.deltaX;
         ufo.currentX = BubbleroidsSprite.width / 2;
      }
      ufo.deltaY = MIN_BUBBLE_SPEED + Math.random() * (MAX_BUBBLE_SPEED - MIN_BUBBLE_SPEED);
      if (Math.random() < 0.5)
      {
         ufo.deltaY = -ufo.deltaY;
      }
      ufo.render();
      saucerPlaying = true;
      if (sound)
      {
         saucerSound.loop();
      }

      // Set counter for this pass.

      ufoCounter = (int)Math.floor(BubbleroidsSprite.width / Math.abs(ufo.deltaX));
   }


   public void updateUfo()
   {
      int i, d;

      // Move the flying saucer and check for collision with a photon. Stop it when its
      // counter has expired.

      if (ufo.active)
      {
         ufo.advance();
         ufo.render();
         if (--ufoCounter <= 0)
         {
            if (--ufoPassesLeft > 0)
            {
               initUfo();
            }
            else
            {
               stopUfo();
            }
         }
         else
         {
            for (i = 0; i < MAX_SHOTS; i++)
            {
               if (photons[i].active && ufo.isColliding(photons[i]))
               {
                  if (sound)
                  {
                     crashSound.play();
                  }
                  explode(ufo);
                  stopUfo();
                  score += UFO_POINTS;
               }
            }

            // On occassion, fire a missile at the ship if the saucer is not
            // too close to it.

            d = (int)Math.max(Math.abs(ufo.currentX - ship.currentX), Math.abs(ufo.currentY - ship.currentY));
            if (ship.active && (hyperCounter <= 0) && ufo.active && !missile.active &&
                (d > 4 * MAX_BUBBLE_SIZE) && (Math.random() < .03))
            {
               initMissile();
            }
         }
      }
   }


   public void stopUfo()
   {
      ufo.active    = false;
      ufoCounter    = 0;
      ufoPassesLeft = 0;
      if (loaded)
      {
         saucerSound.stop();
      }
      saucerPlaying = false;
   }


   public void initMissile()
   {
      missile.active     = true;
      missile.angle      = 0.0;
      missile.deltaAngle = 0.0;
      missile.currentX   = ufo.currentX;
      missile.currentY   = ufo.currentY;
      missile.deltaX     = 0.0;
      missile.deltaY     = 0.0;
      missile.render();
      missileCounter = 3 * Math.max(BubbleroidsSprite.width, BubbleroidsSprite.height) / MIN_BUBBLE_SIZE;
      if (sound)
      {
         missileSound.loop();
      }
      missilePlaying = true;
   }


   public void updateMissile()
   {
      int i;

      // Move the guided missile and check for collision with ship or photon. Stop it when its
      // counter has expired.

      if (missile.active)
      {
         if (--missileCounter <= 0)
         {
            stopMissile();
         }
         else
         {
            guideMissile();
            missile.advance();
            missile.render();
            for (i = 0; i < MAX_SHOTS; i++)
            {
               if (photons[i].active && missile.isColliding(photons[i]))
               {
                  if (sound)
                  {
                     crashSound.play();
                  }
                  explode(missile);
                  stopMissile();
                  score += MISSLE_POINTS;
               }
            }
            if (missile.active && ship.active && (hyperCounter <= 0) && ship.isColliding(missile))
            {
               if (sound)
               {
                  crashSound.play();
               }
               explode(ship);
               stopShip();
               stopUfo();
               stopMissile();
            }
         }
      }
   }


   public void guideMissile()
   {
      double dx, dy, angle;

      if (!ship.active || (hyperCounter > 0))
      {
         return;
      }

      // Find the angle needed to hit the ship.

      dx = ship.currentX - missile.currentX;
      dy = ship.currentY - missile.currentY;
      if ((dx == 0) && (dy == 0))
      {
         angle = 0;
      }
      if (dx == 0)
      {
         if (dy < 0)
         {
            angle = -Math.PI / 2;
         }
         else
         {
            angle = Math.PI / 2;
         }
      }
      else
      {
         angle = Math.atan(Math.abs(dy / dx));
         if (dy > 0)
         {
            angle = -angle;
         }
         if (dx < 0)
         {
            angle = Math.PI - angle;
         }
      }

      // Adjust angle for screen coordinates.

      missile.angle = angle - Math.PI / 2;

      // Change the missile's angle so that it points toward the ship.

      missile.deltaX = MIN_BUBBLE_SIZE / 3 * -Math.sin(missile.angle);
      missile.deltaY = MIN_BUBBLE_SIZE / 3 * Math.cos(missile.angle);
   }


   public void stopMissile()
   {
      missile.active = false;
      missileCounter = 0;
      if (loaded)
      {
         missileSound.stop();
      }
      missilePlaying = false;
   }


   public void initBubbleroids()
   {
      int    i, j;
      double theta, r, dx, dy;
      int    x, y;

      // Create shapes, positions and movements for each bubbleroid.

      bubbleroidsLeft = 0;
      for (i = 0; i < MAX_BUBBLES; i++)
      {
         // Create a circular shape for the bubbleroid.

         bubbleroids[i].shape = new Polygon();
         r = MIN_BUBBLE_SIZE + (int)(Math.random() * (MAX_BUBBLE_SIZE - MIN_BUBBLE_SIZE));
         bubbleroids[i].radius = r;
         for (j = 0; j < NUM_BUBBLE_SIDES; j++)
         {
            theta = 2 * Math.PI / NUM_BUBBLE_SIDES * j;
            x     = (int)-Math.round(r * Math.sin(theta));
            y     = (int)Math.round(r * Math.cos(theta));
            bubbleroids[i].shape.addPoint(x, y);
         }
         bubbleroids[i].active     = true;
         bubbleroids[i].angle      = 0.0;
         bubbleroids[i].deltaAngle = 0.0;

         // Place the bubbleroid at one edge of the screen.

         if (Math.random() < 0.5)
         {
            bubbleroids[i].currentX = -BubbleroidsSprite.width / 2;
            if (Math.random() < 0.5)
            {
               bubbleroids[i].currentX = BubbleroidsSprite.width / 2;
            }
            bubbleroids[i].currentY = Math.random() * BubbleroidsSprite.height;
         }
         else
         {
            bubbleroids[i].currentX = Math.random() * BubbleroidsSprite.width;
            bubbleroids[i].currentY = -BubbleroidsSprite.height / 2;
            if (Math.random() < 0.5)
            {
               bubbleroids[i].currentY = BubbleroidsSprite.height / 2;
            }
         }


         // Do not allow bubbleroids to overlap.

         for (j = 0; j < i; j++)
         {
            if (bubbleroids[j].active)
            {
               dx = bubbleroids[i].currentX - bubbleroids[j].currentX;
               dy = bubbleroids[i].currentY - bubbleroids[j].currentY;
               if (Math.sqrt((dx * dx) + (dy * dy)) <= (bubbleroids[i].radius + bubbleroids[j].radius))
               {
                  bubbleroids[i].active = false;
                  break;
               }
            }
         }
         if (!bubbleroids[i].active) { continue; }

         // Choose bubble color.

         bubbleroids[i].color = new Color((int)(Math.random() * 255), (int)(Math.random() * 255), 255);

         // Set a random motion for the bubbleroid.

         bubbleroids[i].deltaX = Math.random() * bubbleroidsSpeed;
         if (Math.random() < 0.5)
         {
            bubbleroids[i].deltaX = -bubbleroids[i].deltaX;
         }
         bubbleroids[i].deltaY = Math.random() * bubbleroidsSpeed;
         if (Math.random() < 0.5)
         {
            bubbleroids[i].deltaY = -bubbleroids[i].deltaY;
         }

         bubbleroids[i].render();
         bubbleroidIsSmall[i] = false;

         bubbleroidsLeft++;
      }

      bubbleroidsCounter = STORM_PAUSE;
      if (bubbleroidsSpeed < MAX_BUBBLE_SPEED)
      {
         bubbleroidsSpeed++;
      }
   }


   public void initSmallBubbleroids(int n)
   {
      int    count;
      int    i, j;
      double tempX, tempY;
      double theta, r;
      int    x, y;

      // Create one or two smaller bubbleroids from a larger one using inactive bubbleroids. The new
      // bubbleroids will be placed in the same position as the old one but will have a new, smaller
      // shape and new, randomly generated movements.

      count = 0;
      i     = 0;
      tempX = bubbleroids[n].currentX;
      tempY = bubbleroids[n].currentY;
      do
      {
         if (!bubbleroids[i].active)
         {
            bubbleroids[i].shape = new Polygon();
            r = (MIN_BUBBLE_SIZE + (int)(Math.random() * (MAX_BUBBLE_SIZE - MIN_BUBBLE_SIZE))) / 2;
            bubbleroids[i].radius = r;
            for (j = 0; j < NUM_BUBBLE_SIDES; j++)
            {
               theta = 2 * Math.PI / NUM_BUBBLE_SIDES * j;
               x     = (int)-Math.round(r * Math.sin(theta));
               y     = (int)Math.round(r * Math.cos(theta));
               bubbleroids[i].shape.addPoint(x, y);
            }
            bubbleroids[i].active     = true;
            bubbleroids[i].angle      = 0.0;
            bubbleroids[i].deltaAngle = 0.0;
            bubbleroids[i].currentX   = tempX;
            bubbleroids[i].currentY   = tempY;
            bubbleroids[i].deltaX     = Math.random() * 2 * bubbleroidsSpeed - bubbleroidsSpeed;
            bubbleroids[i].deltaY     = Math.random() * 2 * bubbleroidsSpeed - bubbleroidsSpeed;
            bubbleroids[i].color      = bubbleroids[n].color;
            bubbleroidIsSmall[i]      = true;
            count++;
            bubbleroidsLeft++;
         }
         i++;
      } while (i < MAX_BUBBLES && count < 2);
   }


   public void updateBubbleroids()
   {
      int    i, j;
      double dx, dy, d1, d2;

      // Bounce colliding bubbleroids which are also moving toward each other.

      for (i = 0; i < MAX_BUBBLES; i++)
      {
         if (bubbleroids[i].active)
         {
            for (j = i + 1; j < MAX_BUBBLES; j++)
            {
               if (bubbleroids[j].active)
               {
                  d2 = bubbleroids[i].radius + bubbleroids[j].radius;
                  dx = (bubbleroids[i].currentX + bubbleroids[i].deltaX) -
                       (bubbleroids[j].currentX + bubbleroids[j].deltaX);
                  if (Math.abs(dx) > d2) { continue; }
                  dy = (bubbleroids[i].currentY - bubbleroids[i].deltaY) -
                       (bubbleroids[j].currentY - bubbleroids[j].deltaY);
                  if (Math.abs(dy) > d2) { continue; }
                  d1 = Math.sqrt((dx * dx) + (dy * dy));
                  if (d1 <= d2)
                  {
                     dx = bubbleroids[i].currentX - bubbleroids[j].currentX;
                     dy = bubbleroids[i].currentY - bubbleroids[j].currentY;
                     d2 = Math.sqrt((dx * dx) + (dy * dy));
                     if (d1 < d2)
                     {
                        dx = bubbleroids[i].deltaX;
                        dy = bubbleroids[i].deltaY;
                        bubbleroids[i].deltaX = bubbleroids[j].deltaX;
                        bubbleroids[i].deltaY = bubbleroids[j].deltaY;
                        bubbleroids[j].deltaX = dx;
                        bubbleroids[j].deltaY = dy;
                     }
                  }
               }
            }
         }
      }

      // Bounce bubbleroids off of walls.

      for (i = 0; i < MAX_BUBBLES; i++)
      {
         if (bubbleroids[i].active)
         {
            dx = bubbleroids[i].currentX + bubbleroids[i].deltaX;
            if (((dx + bubbleroids[i].radius) >= (double)(BubbleroidsSprite.width2)) && (bubbleroids[i].deltaX > 0.0))
            {
               bubbleroids[i].deltaX = -bubbleroids[i].deltaX;
            }
            else if (((dx - bubbleroids[i].radius) <= (double)(-BubbleroidsSprite.width2)) && (bubbleroids[i].deltaX < 0.0))
            {
               bubbleroids[i].deltaX = -bubbleroids[i].deltaX;
            }
            dy = bubbleroids[i].currentY - bubbleroids[i].deltaY;
            if (((dy + bubbleroids[i].radius) >= (double)(BubbleroidsSprite.height2)) && (bubbleroids[i].deltaY < 0.0))
            {
               bubbleroids[i].deltaY = -bubbleroids[i].deltaY;
            }
            else if (((dy - bubbleroids[i].radius) <= (double)(-BubbleroidsSprite.height2)) && (bubbleroids[i].deltaY > 0.0))
            {
               bubbleroids[i].deltaY = -bubbleroids[i].deltaY;
            }
         }
      }

      // Move any active bubbleroids and check for other collisions.

      for (i = 0; i < MAX_BUBBLES; i++)
      {
         if (bubbleroids[i].active)
         {
            bubbleroids[i].advance();
            bubbleroids[i].render();

            // If hit by photon, kill bubbleroid and advance score. If bubbleroid is large,
            // make some smaller ones to replace it.

            for (j = 0; j < MAX_SHOTS; j++)
            {
               if (photons[j].active && bubbleroids[i].active && bubbleroids[i].isColliding(photons[j]))
               {
                  bubbleroidsLeft--;
                  bubbleroids[i].active = false;
                  photons[j].active     = false;
                  if (sound)
                  {
                     explosionSound.play();
                  }
                  explode(bubbleroids[i]);
                  if (!bubbleroidIsSmall[i])
                  {
                     score += BIG_POINTS;
                     initSmallBubbleroids(i);
                  }
                  else
                  {
                     score += SMALL_POINTS;
                  }
               }
            }

            // If the ship is not in hyperspace, see if it is hit.

            if (ship.active && (hyperCounter <= 0) && bubbleroids[i].active && bubbleroids[i].isColliding(ship))
            {
               if (sound)
               {
                  crashSound.play();
               }
               explode(ship);
               stopShip();
               stopUfo();
               stopMissile();
            }
         }
      }
   }


   public void initExplosions()
   {
      int i;

      for (i = 0; i < MAX_SCRAP; i++)
      {
         explosions[i].shape  = new Polygon();
         explosions[i].active = false;
         explosionCounter[i]  = 0;
      }
      explosionIndex = 0;
   }


   public void explode(BubbleroidsSprite s)
   {
      int c, i, j;

      // Create sprites for explosion animation. The each individual line segment of the given sprite
      // is used to create a new sprite that will move outward  from the sprite's original position
      // with a random rotation.

      s.render();
      c = 2;
      if (detail || (s.sprite.npoints < 6))
      {
         c = 1;
      }
      for (i = 0; i < s.sprite.npoints; i += c)
      {
         explosionIndex++;
         if (explosionIndex >= MAX_SCRAP)
         {
            explosionIndex = 0;
         }
         explosions[explosionIndex].active = true;
         explosions[explosionIndex].shape  = new Polygon();
         explosions[explosionIndex].shape.addPoint(s.shape.xpoints[i], s.shape.ypoints[i]);
         j = i + 1;
         if (j >= s.sprite.npoints)
         {
            j -= s.sprite.npoints;
         }
         explosions[explosionIndex].shape.addPoint(s.shape.xpoints[j], s.shape.ypoints[j]);
         explosions[explosionIndex].angle      = s.angle;
         explosions[explosionIndex].deltaAngle = (Math.random() * 2 * Math.PI - Math.PI) / 15;
         explosions[explosionIndex].currentX   = s.currentX;
         explosions[explosionIndex].currentY   = s.currentY;
         explosions[explosionIndex].deltaX     = -s.shape.xpoints[i] / 5;
         explosions[explosionIndex].deltaY     = -s.shape.ypoints[i] / 5;
         explosions[explosionIndex].color      = s.color;
         explosionCounter[explosionIndex]      = SCRAP_COUNT;
      }
   }


   public void updateExplosions()
   {
      int i;

      // Move any active explosion debris. Stop explosion when its counter has expired.

      for (i = 0; i < MAX_SCRAP; i++)
      {
         if (explosions[i].active)
         {
            explosions[i].advance();
            explosions[i].render();
            if (--explosionCounter[i] < 0)
            {
               explosions[i].active = false;
            }
         }
      }
   }


   public boolean keyDown(Event e, int key)
   {
      // Check if any cursor keys have been pressed and set flags.

      if (key == Event.LEFT)
      {
         left = true;
      }
      if (key == Event.RIGHT)
      {
         right = true;
      }
      if (key == Event.UP)
      {
         up = true;
      }
      if (key == Event.DOWN)
      {
         down = true;
      }

      if ((up || down) && ship.active && !thrustersPlaying && !paused)
      {
         if (sound)
         {
            thrustersSound.loop();
         }
         thrustersPlaying = true;
      }

      // Spacebar: fire a photon and start its counter.

      if ((key == 32) && ship.active && !paused)
      {
         if (sound)
         {
            fireSound.play();
         }
         photonIndex++;
         if (photonIndex >= MAX_SHOTS)
         {
            photonIndex = 0;
         }
         photons[photonIndex].active   = true;
         photons[photonIndex].currentX = ship.currentX;
         photons[photonIndex].currentY = ship.currentY;
         photons[photonIndex].deltaX   = MIN_BUBBLE_SIZE * -Math.sin(ship.angle);
         photons[photonIndex].deltaY   = MIN_BUBBLE_SIZE * Math.cos(ship.angle);
         photonCounter[photonIndex]    = Math.min(BubbleroidsSprite.width, BubbleroidsSprite.height) / MIN_BUBBLE_SIZE;
      }

      // 'H' key: warp ship into hyperspace by moving to a random location and starting counter.

      if ((key == 104) && ship.active && (hyperCounter <= 0) && !paused)
      {
         ship.currentX = (Math.random() * BubbleroidsSprite.width) - BubbleroidsSprite.width2;
         ship.currentY = (Math.random() * BubbleroidsSprite.height) - BubbleroidsSprite.height2;
         hyperCounter  = HYPER_COUNT;
         if (sound)
         {
            warpSound.play();
         }
      }

      // 'D' key: toggle graphics detail on or off.

      if (key == 100)
      {
         detail = !detail;
      }

      // 'U' key: launch the UFO.

      if ((key == 117) && ship.active && !paused && !ufo.active)
      {
         newUfoScore  += NEW_UFO_POINTS;
         ufoPassesLeft = UFO_PASSES;
         initUfo();
      }

      return(true);
   }


   public boolean keyUp(Event e, int key)
   {
      // Check if any cursor keys where released and set flags.

      if (key == Event.LEFT)
      {
         left = false;
      }
      if (key == Event.RIGHT)
      {
         right = false;
      }
      if (key == Event.UP)
      {
         up = false;
      }
      if (key == Event.DOWN)
      {
         down = false;
      }

      if (!up && !down && thrustersPlaying)
      {
         thrustersSound.stop();
         thrustersPlaying = false;
      }


      return(true);
   }


   public boolean action(Event evt, Object arg)
   {
      // Start/Quit.

      if (evt.target.equals(startQuit) && startQuit.getState() &&
          loaded && !playing)
      {
         initGame();
         startQuit.setLabel("Quit");
      }
      else if (evt.target.equals(startQuit) && !startQuit.getState() &&
               loaded && playing)
      {
         initGame();
         endGame();
         startQuit.setLabel("Start");

         // Pause.
      }
      else if (evt.target.equals(pauseCheck))
      {
         if (!loaded || !playing)
         {
            pauseCheck.setState(false);
         }
         else if (pauseCheck.getState())
         {
            if (missilePlaying)
            {
               missileSound.stop();
            }
            if (saucerPlaying)
            {
               saucerSound.stop();
            }
            if (thrustersPlaying)
            {
               thrustersSound.stop();
            }
            paused = true;
         }
         else if (!pauseCheck.getState())
         {
            if (sound && missilePlaying)
            {
               missileSound.loop();
            }
            if (sound && saucerPlaying)
            {
               saucerSound.loop();
            }
            if (sound && thrustersPlaying)
            {
               thrustersSound.loop();
            }
            paused = false;
         }

         // Mute.
      }
      else if (evt.target.equals(muteCheck))
      {
         if (!loaded)
         {
            muteCheck.setState(false);
         }
         else if (muteCheck.getState())
         {
            crashSound.stop();
            explosionSound.stop();
            fireSound.stop();
            missileSound.stop();
            saucerSound.stop();
            thrustersSound.stop();
            warpSound.stop();
            sound = false;
         }
         else if (!muteCheck.getState())
         {
            if (missilePlaying)
            {
               missileSound.loop();
            }
            if (saucerPlaying)
            {
               saucerSound.loop();
            }
            if (thrustersPlaying)
            {
               thrustersSound.loop();
            }
            sound = true;
         }
      }
      else{ return(super.action(evt, arg)); }
      return(true);
   }


   public void paint(Graphics g)
   {
      update(g);
   }


   public void update(Graphics g)
   {
      Dimension d = spaceDimension;
      int       i;
      int       c, cr, cg, cb;
      int       x, y, r2;
      String    s;

      // Fill in background and stars.

      spaceImageGraphics.setColor(Color.black);
      spaceImageGraphics.fillRect(0, 0, d.width, d.height);
      if (detail)
      {
         spaceImageGraphics.setColor(Color.white);
         for (i = 0; i < numStars; i++)
         {
            spaceImageGraphics.drawLine(stars[i].x, stars[i].y, stars[i].x, stars[i].y);
         }
      }

      // Draw photon bullets.

      spaceImageGraphics.setColor(Color.white);
      for (i = 0; i < MAX_SHOTS; i++)
      {
         if (photons[i].active)
         {
            spaceImageGraphics.drawPolygon(photons[i].sprite);
         }
      }

      // Draw the guided missile, counter is used to quickly fade color to black when near expiration.

      c = Math.min(missileCounter * 24, 255);
      spaceImageGraphics.setColor(new Color(c, c, c));
      if (missile.active)
      {
         spaceImageGraphics.drawPolygon(missile.sprite);
         spaceImageGraphics.drawLine(missile.sprite.xpoints[missile.sprite.npoints - 1], missile.sprite.ypoints[missile.sprite.npoints - 1],
                                     missile.sprite.xpoints[0], missile.sprite.ypoints[0]);
      }

      // Draw the bubbleroids.

      for (i = 0; i < MAX_BUBBLES; i++)
      {
         if (bubbleroids[i].active)
         {
            spaceImageGraphics.setColor(bubbleroids[i].color);
            x  = (int)Math.round(bubbleroids[i].currentX - bubbleroids[i].radius + BubbleroidsSprite.width2);
            y  = (int)Math.round(bubbleroids[i].currentY - bubbleroids[i].radius + BubbleroidsSprite.height2);
            r2 = (int)Math.round(bubbleroids[i].radius * 2.0);
            if (detail)
            {
               spaceImageGraphics.fillOval(x, y, r2, r2);
            }
            else
            {
               spaceImageGraphics.drawOval(x, y, r2, r2);
            }
         }
      }

      // Draw the flying saucer.

      if (ufo.active)
      {
         if (detail)
         {
            spaceImageGraphics.setColor(Color.black);
            spaceImageGraphics.fillPolygon(ufo.sprite);
         }
         spaceImageGraphics.setColor(Color.white);
         spaceImageGraphics.drawPolygon(ufo.sprite);
         spaceImageGraphics.drawLine(ufo.sprite.xpoints[ufo.sprite.npoints - 1], ufo.sprite.ypoints[ufo.sprite.npoints - 1],
                                     ufo.sprite.xpoints[0], ufo.sprite.ypoints[0]);
      }

      // Draw the ship, counter is used to fade color to white on hyperspace.

      c = 255 - (255 / HYPER_COUNT) * hyperCounter;
      if (ship.active)
      {
         if (detail && (hyperCounter == 0))
         {
            spaceImageGraphics.setColor(Color.black);
            spaceImageGraphics.fillPolygon(ship.sprite);
         }
         spaceImageGraphics.setColor(new Color(c, c, c));
         spaceImageGraphics.drawPolygon(ship.sprite);
         spaceImageGraphics.drawLine(ship.sprite.xpoints[ship.sprite.npoints - 1], ship.sprite.ypoints[ship.sprite.npoints - 1],
                                     ship.sprite.xpoints[0], ship.sprite.ypoints[0]);
      }

      // Draw any explosion debris, counters are used to fade color to black.

      for (i = 0; i < MAX_SCRAP; i++)
      {
         if (explosions[i].active)
         {
            cr = (explosions[i].color.getRed() / SCRAP_COUNT) * explosionCounter [i];
            cg = (explosions[i].color.getGreen() / SCRAP_COUNT) * explosionCounter [i];
            cb = (explosions[i].color.getBlue() / SCRAP_COUNT) * explosionCounter [i];
            spaceImageGraphics.setColor(new Color(cr, cg, cb));
            spaceImageGraphics.drawPolygon(explosions[i].sprite);
         }
      }

      // Display status and messages.

      spaceImageGraphics.setFont(font);
      spaceImageGraphics.setColor(Color.white);

      spaceImageGraphics.drawString("Score: " + score, fontWidth, fontHeight);
      spaceImageGraphics.drawString("Ships: " + shipsLeft, fontWidth, d.height - fontHeight);
      s = "High: " + highScore;
      spaceImageGraphics.drawString(s, d.width - (fontWidth + fm.stringWidth(s)), fontHeight);

      if (!playing)
      {
         s = "B U B B L E R O I D S";
         spaceImageGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2);
         if (!loaded)
         {
            s = "Loading sounds...";
            spaceImageGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
         }
         else
         {
            s = "Game Over";
            spaceImageGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
         }
      }

      // Copy the off screen buffer to the screen.

      spaceGraphics.drawImage(spaceImage, 0, 0, this);
   }


   // Main.
   @SuppressWarnings("deprecation")
   public static void main(String[] args)
   {
      // Create game.
      Bubbleroids game = new Bubbleroids();

      // Create frame.
      JFrame frame = new JFrame();

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setTitle("Bubbleroids");
      frame.setBounds(0, 0, 500, 399);
      frame.setLayout(new GridLayout(1, 1));
      frame.add(game);
      frame.setVisible(true);

      // Run applet.
      game.init();
      game.start();
      frame.resize(new Dimension(500, 400));

      // Print instructions.
      System.out.println("   Keyboard Controls:");
      System.out.println();
      System.out.println("     Arrow Left  - Rotate Left   Arrow Up   - Fire Thrusters");
      System.out.println("     Arrow Right - Rotate Right  Arrow Down - Fire Retro Thrusters");
      System.out.println("     Spacebar    - Fire Cannon   H          - Hyperspace");
   }
}
