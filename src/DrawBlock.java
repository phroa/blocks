/*
 * Draw a block assembly puzzle.
 * Author: Chris Reedy (Chris.Reedy@wwu.edu)
 * This work is licensed under the Creative Commons Attribution-NonCommercial 3.0
 * Unported License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-nc/3.0/ or send a letter to Creative
 * Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;

/* Class DrawBlock
 *
 * Draw and update a block assembly puzzle.
 *
 * To use the class:
 *   1. Call the constructor DrawBlock(int w, int h) which initializes the
 *      puzzle with a target rectangle of the given width(w) and height (h).
 *
 *   2. For every initial rectangle in the puzzle, call useRect(int w, int h)
 *      where w and h are the width and height of the given initial rectangle.
 *
 *   3. Once step 2 is complete, call setupComplete(). This will cause the
 *      display to appear after all internal data structures have been
 *      initialized.
 *
 *   4. While searching,
 *     * call placeRect(int w, int h, int c, int r) to indicate that a rectangle
 *       of width w and height h has been with its upper left hand corner at
 *       column c and row r of the target rectangle.
 *     * call clearRect(int w, int h, int c, int r) to clear a previously
 *       placed rectangle from the target rectangle.
 *
 * Here is a rough example of how to use the class:
 *
 *   int width = <<width of grid>>;
 *   int height = <<height of grid>>;
 *
 *   DrawBlock db = new DrawBlock(width, height);
 *
 *   // Assume that the initial rectangles are maintained in an a 2-D array
 *   // int[][] rects which is of size #rectangles by 2
 *   for (int n = 0; n < rects.length; ++n) {
 *     db.useRect(rects[n][0], rects[n][1]);
 *   }
 *   db.setupComplete();
 *
 *   searching for a solution
 *     db.placeRect(w, h, c, r); // Places a w x h rectangle at (c, r)
 *     db.clearRect(w, h, c, r); // Remove the w x h rectangle at (c, r)
 *
 *   to display the current state of the puzzle, call:
 *     db.printRect();
 *
 * All of the calls, above are checked for validity. If the call is invalid in
 * some way, an exception will be thrown indicating the nature of the problem.
 * see the description of each method below for specific exceptions that can
 * be thrown.
 *
 * The following calls are available to control the operation of DrawBlock:
 *   to turn animation on/off:
 *     db.setAnimate(true/false); // true to turn on, false to turn off (default on)
 *   to turn drawing of any final solution on/off:
 *     db.setDrawFinal(true/false); // true to turn on, false to turn off (default on)
 *   to turn printing of any final solution on/off:
 *     db.setPrintFinal(true/false); // true to turn on, false to turn off (default on)
 *   to control length of delay during animation:
 *     db.setDelay(delayTime); // delayTime is in milliseconds (default 50)
 *
 * Note: After a rectangle is placed on the target rectangle, the drawing
 * will delay for a fixed time. A call to g.setDelay(int delayTime) will
 * set the delay time to the given number of milliseconds.
 */
public class DrawBlock {

   // The maximum size for either dimension of the target rectangle.
   public static final int MAX_SIZE = 99;

   // Flags that control behavior
   private boolean animate = true; // Animate the search processArgs
   private boolean drawFinal = true; // Drawing of any final solution
   private boolean printFinal = true; // Print any final solution
   private int drawSleepTime = 50; // Time in milliseconds to wait between steps

   /* Turn animation on and off.
    *
    * The prior value of the animate flag is returned.
    */
   public boolean setAnimate(boolean animateValue) {
      boolean oldValue = animate;
      animate = animateValue;
      if (animate && !oldValue) {
         redraw();
      }
      return oldValue;
   }

   /* Set flag to print solutions.
    *
    * The prior value is returned.
    */
   public boolean setPrintfinal(boolean print) {
      boolean oldValue = printFinal;
      printFinal = print;
      return oldValue;
   }

   /* Set flag to draw solutions.
    *
    * The prior value is returned.
    */
   public boolean setDrawfinal(boolean draw) {
      boolean oldValue = drawFinal;
      drawFinal = draw;
      return oldValue;
   }

   /* Set the delay time for animations.
    *
    * The prior value is returned.
    */
   public int setSleepTime(int sleep) {
      int oldValue = drawSleepTime;
      if (sleep < 0)
         sleep = 0;
      drawSleepTime = sleep;
      return oldValue;
   }

   /* There was a problem placing or clearing a rectangle in the target
    * rectangle. The associated message explains the exact reason the
    * exception was thrown. */
   public static class PlaceRectangleException extends RuntimeException {
      public PlaceRectangleException(String msg) {
         super(msg);
      }
   }

   /* Throw a PlaceRectangleException.
    *
    * When a PlaceRectangleException is thrown, it comes through this method.
    * You can set a break point on the throw statement to break when this
    * exception is about to be thrown.
    */
   private static void throwPlaceException(String msg) {
      throw new PlaceRectangleException(msg);
   }

   // Internal class for a rectangle.
   private static class Rect {
      final int w;
      final int h;
      Rect reversed; // reverse of this rect

      static Rect makeRect(int w, int h) {
         Rect thisRect = new Rect(w, h);
         Rect reversedRect = null;
         if (w != h) {
            reversedRect = new Rect(h, w);
            thisRect.reversed = reversedRect;
            reversedRect.reversed = thisRect;
         } else {
            thisRect.reversed = thisRect;
         }
         if (w >= h) {
            return thisRect;
         } else {
            return reversedRect;
         }
      }

      private Rect(int w, int h) {
         this.w = w;
         this.h = h;
      }

      @Override public boolean equals(Object other) {
         if (!(other instanceof Rect))
            return false;
         Rect otherRect = (Rect)other;
         return this.w == otherRect.w && this.h == otherRect.h;
      }
   }

   // Internal class for a rectangle at a location in the grid.
   private static class RectLoc {
      final int rectNum;
      final int l, r, t, b;

      RectLoc(int rectNum, int l, int r, int t, int b) {
         this.rectNum = rectNum;
         this.l = l;
         this.r = r;
         this.t = t;
         this.b = b;
      }

      RectLoc(int rectNum, Rect rect, int c, int r) {
         this(rectNum, c, c + rect.w, r, r + rect.h);
      }

      @Override public String toString() {
         return String.format("rectangle %d x %d at (%d, %d)",
                 r - l, b - t, l, t);
      }

      boolean hasLoc(int c, int r) {
         return this.l <= c && c < this.r && this.t <= r && r < this.b;
      }

      boolean inside(RectLoc other) {
         return this. l >= other.l && this.r <= other.r &&
                 this.t >= other.t && this.b <= other.b;
      }

      boolean overlap(RectLoc other) {
         return this.r > other.l && this.l < other.r &&
                 this.b > other.r && this.t < other.b;
      }
   }

   private final int targetWidth;
   private final int targetHeight;
   private final RectLoc targetRect;
   private ArrayList<Rect> rectsToUse = new ArrayList<>();
   private boolean setupComplete = false;
   private int minHeight, minWidth; // minimum height and width over all rectangles

   private boolean[] usedRects;
   private RectLoc[] placedRects;
   private int numUsed;

   private int printRectMultiple;

   /* Create a new DrawBlock object.
    *
    * w and h are the width and height of the target rectangle. The target
    * rectangle is the one that is to be filled with the initial rectangles.
    *
    * This method throws an IllegalArgumentException if the width or height
    * are too big or too small.
    */
   public DrawBlock(int w, int h) {
      if (w < 1 || w > MAX_SIZE || h < 1 || h > MAX_SIZE) {
         throw new IllegalArgumentException(
                 String.format("Bad target rectangle size %d x %d", w, h));
      }
      targetWidth = w;
      targetHeight = h;
      targetRect = new RectLoc(-1, 0, w, 0, h);
   }

   /* Add a rectangle to be used to fill the target rectangle.
    *
    * w and h are the width and height of the rectangle.
    *
    * This method throws an IllegalArgumentException is the width or height
    * are too big or too small.
    */
   public void useRect(int w, int h) {
      int maxRectSize = Math.max(targetWidth, targetHeight);
      if (w < 1 || w > maxRectSize || h < 1 || h > maxRectSize) {
         throw new IllegalArgumentException(
                 String.format("Bad rectangle size %d x %d", w, h));
      }
      rectsToUse.add(Rect.makeRect(w, h));
   }

   /* Indicate that the setup process is complete. This means that all the
    * rectangles to be used to fill the target rectangle have been specified.
    *
    * This method will throw an IllegalStateException if the sum of the sizes
    * of the initial rectangles is not equal to the total size of the target
    * rectangle.
    */
   public void setupComplete() {
      int sum = 0;
      for (Rect rect : rectsToUse) {
         sum += rect.w * rect.h;
      }
      int targetSize = targetWidth * targetHeight;
      if (sum != targetSize) {
         throw new IllegalStateException(
                 String.format("Total size of all initial rectangles (%d) is not equal to size of target(%d)",
                         sum, targetSize));
      }

      // Setup structures to track progress
      setupComplete = true;
      usedRects = new boolean[rectsToUse.size()];
      Arrays.fill(usedRects, false); // Not really necessary
      placedRects = new RectLoc[rectsToUse.size()];
      Arrays.fill(placedRects, null); // Again, not really necessary
      numUsed = 0;

      // Setup for drawing and printing the target rectangles
      minHeight = Integer.MAX_VALUE;
      minWidth = Integer.MAX_VALUE;
      for (Rect rect : rectsToUse) {
         minHeight = Math.min(minHeight, rect.h);
         minWidth = Math.min(minWidth, rect.w);
      }
      switch (minHeight) {
         case 1:
            printRectMultiple = 3;
            break;
         case 2:
            printRectMultiple = 2;
            break;
         default:
            printRectMultiple = 1;
      }

      // Now draw the initial target rectangle.
      if (animate) {
         setupGraphics();
      }
   }

   /* Place the initial rectangle with width w and height h at column c and
    * row r in the target rectangle.
    *
    * The code checks to ensure that
    *   (1) the rectangle is an unused initial rectangle,
    *   (2) the rectangle fits in the target rectangle, and
    *   (3) that the rectangle does not overlap any rectangles that have already
    *       been place in the target rectangle.
    *
    * If any of the above conditions fails to hold, a PlaceRectangleException is
    * thrown. The message on the exception indicates the exact nature of the
    * problem.
    *
    * If setupComplete has not been called, an IllegalStateException will be
    * thrown.
    */
   public void placeRect(int w, int h, int c, int r) {
      if (!setupComplete) {
         throw new IllegalStateException("placeRect called before setupComplete has been called");
      }

      // Find the rectangle
      int rectNum = findUnusedRect(w, h);
      Rect theRect = rectsToUse.get(rectNum);
      if (w < h)
         theRect = theRect.reversed;

      RectLoc loc = new RectLoc(rectNum, theRect, c, r);
      if (!loc.inside(targetRect)) {
         throwPlaceException(String.format("%s does not fit inside target rectangle %d x %d",
                 loc, targetWidth, targetHeight));
      }

      for (int n = 0; n < numUsed; ++n) {
         RectLoc rect = placedRects[n];
         if(loc.overlap(rect)) {
            throwPlaceException(String.format("%s overlaps already placed %s", loc, rect));
         }
      }

      // Everything's good
      usedRects[rectNum] = true;
      placedRects[numUsed] = loc;
      ++numUsed;

      // Draw the rectangle it needed
      if (animate) {
         drawRect(loc, true);
      }

      // If the target rectangle has been successfully filled, output the Found
      // solution.
      if (numUsed == rectsToUse.size()) {
         if (printFinal) {
            printRect();
         }
         if (drawFinal && !animate) {
            redraw();
         }
      }
   }

   /* Undo a placeRect
    *
    * w, h, c, r are the same as the corresponding call to place.
    *
    * This version of unplace can only remove the last placed rectangle. A
    * PlaceRectangleException will be thrown if these parameters do not match
    * the last placed Rectangle.
    */
   public void clearRect(int w, int h, int c, int r) {
      if (!setupComplete) {
         throw new IllegalStateException("placeRect called before setupComplete has been called");
      }

      if (numUsed == 0) {
         throwPlaceException("there is no rectangle that can be removed");
      }
      RectLoc lastLoc = placedRects[numUsed - 1];
      if (lastLoc.l != c || lastLoc.t != r ||
              lastLoc.r - lastLoc.l != w || lastLoc.b - lastLoc.t != h) {
         throwPlaceException(String.format("rectangle to be unplaced %d x %d @ (%d, %d) doesn't match last placed %s",
                 w, h, c, r, lastLoc));
      }

      // Everything's good
      --numUsed;
      usedRects[lastLoc.rectNum] = false;
      placedRects[numUsed] = null;

      // Clear the rectangle
      if (animate) {
         clearRect(lastLoc);
      }
   }

   private int findUnusedRect(int w, int h) {
      int searchW, searchH;
      if (w >= h) {
         searchW = w;
         searchH = h;
      } else {
         searchW = h;
         searchH = w;
      }

      int rectNum;
      boolean matched = false;
      for (rectNum = 0; rectNum < rectsToUse.size(); ++rectNum) {
         Rect rect = rectsToUse.get(rectNum);
         if (rect.w == searchW && rect.h == searchH) {
            matched = true;
            if (!usedRects[rectNum])
               return rectNum;
         }
      }

      if (matched) {
         // Found a matching rect but it was already usedRect
         throwPlaceException(
                 String.format("rectangle %d x %d has already been used", w, h));
      } else {
         // There is no such rectangle
         throwPlaceException(
                 String.format("rectangle %d x %d does not exist", w, h));
      }
      throw new AssertionError("Impossible. Report this message to your instructor.");
   }

   /* Print the target rectangle to System.out.
    *
    * This method will throw an IllegalStateException if called before
    * setupComplete has been called.
    */
   public void printRect() {
      if (!setupComplete) {
         throw new IllegalStateException("printRect called before setupComplete has been called");
      }

      // Output a description of the rectangle.
      if (numUsed == rectsToUse.size())
         System.out.println("Solution!");
      else if (numUsed == 0)
         System.out.println("Empty target");
      else
         System.out.println("Partially filled target");

      // Now output the actual rectangle.
      int index = 0;
      for (int pr = 0; pr < targetHeight * printRectMultiple; ++pr) {
         for (int pc = 0; pc < targetWidth * 2 * printRectMultiple; ) {
            int n;
            for (n = 0; n < numUsed; ++n) {
               RectLoc rect = placedRects[n];
               if (rect.hasLoc(pc / (2 * printRectMultiple), pr / printRectMultiple)) {
                  printRectRow(rect, pr);
                  pc = 2 * rect.r * printRectMultiple;
                  break;
               }
            }
            if (n >= numUsed) {
               System.out.print('.');
               ++pc;
            }
         }
         System.out.println();
      }
   }

   private void printRectRow(RectLoc rect, int pr) {
      int printWidth = 2 * (rect.r - rect.l) * printRectMultiple;
      int printTop = rect.t * printRectMultiple;
      int printBottom = rect.b * printRectMultiple;
      if (pr == printTop || pr == printBottom - 1) {
         System.out.print(edgePrintLine(printWidth));
      } else {
         StringBuilder buf = midPrintLine(printWidth);
         if (pr == (printTop + printBottom - 1) / 2) {
            String dims = String.format("%dx%d", rect.r - rect.l, rect.b - rect.t);
            int st = (printWidth - dims.length()) / 2;
            buf.replace(st, st + dims.length(), dims);
         }
         System.out.print(buf);
      }
   }

   // Generate a String of +--- ... ---+ of length n
   private StringBuilder edgePrintLine(int n) {
      StringBuilder buf = new StringBuilder(n);
      buf.append('+');
      for (int k = 1; k < n - 1; ++k) {
         buf.append('-');
      }
      buf.append('+');
      return buf;
   }

   // Generate a String of |    ...    | of length n
   private StringBuilder midPrintLine(int n) {
      StringBuilder buf = new StringBuilder(n);
      buf.append('|');
      for (int k = 1; k < n - 1; ++k) {
         buf.append(' ');
      }
      buf.append('|');
      return buf;
   }

   private static final int MINIMUM_WINDOW = 600;
   private static final int BORDER_WIDTH = 20;
   private static final int MIN_SQUARE_SIZE = 2;
   private int squareSize = 50;
   private static final int SEP_WIDTH = 1;
   private static final int HIGHLIGHT_OFFSET = 7;
   private static final int HIGHLIGHT_ARC = 10;
   private static final int HIGHLIGHT_STROKE_SIZE = 4;
   private static final int LABEL_SPACE = (SEP_WIDTH + HIGHLIGHT_OFFSET + 5);
   private static int FONT_SIZE = 16;
   private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, FONT_SIZE);
   private static final Color BORDER_COLOR = Color.BLACK;
   private static final Color BACKGROUND_COLOR = Color.LIGHT_GRAY;
   private static final Color SEP_COLOR = Color.WHITE;
   private static final Color RECT_FILL = Color.DARK_GRAY;
   private static final Color HIGHLIGHT_COLOR = Color.ORANGE.darker();
   private static final Color FONT_COLOR = Color.ORANGE;
   private static final Color LETTERING_COLOR = Color.WHITE;
   private static final Stroke HIGHLIGHT_STROKE = new BasicStroke(HIGHLIGHT_STROKE_SIZE);

   private DrawingPanel drawing = null;
   private Graphics2D graphics = null;
   private int panelWidth, panelHeight;
   private int targetLeft, targetRight, targetTop, targetBottom;

   private void redraw() {
      if (drawing == null && setupComplete) {
         setupGraphics();
      }
      drawBackground();
      for (RectLoc loc : placedRects) {
         if (loc != null)
            drawRect(loc, false);
      }
   }

   private void setupGraphics() {
      createDrawingPanel();

      drawing.setBackground(BACKGROUND_COLOR);
      graphics = drawing.getGraphics();

      targetLeft = BORDER_WIDTH;
      targetRight = panelWidth - BORDER_WIDTH;
      targetTop = BORDER_WIDTH;
      targetBottom = panelHeight - BORDER_WIDTH;

      drawBackground();
   }

   private void createDrawingPanel() {
      BufferedImage tempImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
      Graphics2D tempGraphics = (Graphics2D)tempImage.getGraphics();
      tempGraphics.setFont(LABEL_FONT);
      tempGraphics.setColor(FONT_COLOR);
      Rectangle2D labelRect = tempGraphics.getFontMetrics().getStringBounds("99x99", tempGraphics);
      // System.out.println("LabelRect = " + labelRect);

      // Make sure window is big enough relative to target rectangle
      squareSize = MIN_SQUARE_SIZE; // Minimum squareSize
      // System.out.println("MIN_SQUARE_SIZE = " + MIN_SQUARE_SIZE);
      int minWindowSize = (int)Math.ceil(((float)MINIMUM_WINDOW) / (Math.max(targetWidth, targetHeight)));
      // System.out.println("MINIMUM_WINDOW = " + MINIMUM_WINDOW + " targetWidth = " + targetWidth
      //       + " targetHeight =" + targetHeight + " minWindowSize = " + minWindowSize);
      squareSize = Math.max(squareSize, minWindowSize);

      // Make sure window is big enough realtive to labels in rectangles
      int minRectHeight = (int)Math.ceil((labelRect.getHeight() + 2 * LABEL_SPACE) / minHeight);
      int minRectWidth = (int)Math.ceil((labelRect.getWidth() + 2 * LABEL_SPACE) / minWidth);
      // System.out.println("LABEL_SPACE = " + LABEL_SPACE + " minHeight = " + minHeight + " minWidth = " + minWidth);
      // System.out.println("minRectHeight = " + minRectHeight + " minRectWidth = " + minRectWidth);
      squareSize = Math.max(squareSize, minRectHeight);
      squareSize = Math.max(squareSize, minRectWidth);

      // Set size of drawing panel
      panelWidth = targetWidth * squareSize + 2 * BORDER_WIDTH;
      panelHeight = targetHeight * squareSize + 2 * BORDER_WIDTH;
      drawing = new DrawingPanel(panelWidth, panelHeight);
   }

   private void drawBackground() {
      graphics.setColor(BORDER_COLOR);
      graphics.fillRect(0, 0, panelWidth, targetTop);
      graphics.fillRect(0, 0, targetLeft, panelHeight);
      graphics.fillRect(0, targetBottom, panelWidth, BORDER_WIDTH);
      graphics.fillRect(targetRight, 0, BORDER_WIDTH, panelHeight);

      drawSeparators(0, targetWidth, 0, targetHeight);
   }

   private void drawSeparators(int left, int right, int top, int bottom) {
      graphics.setColor(SEP_COLOR);
      int ytop = targetTop + top * squareSize;
      int ybottom = targetTop + bottom * squareSize;
      for (int c = left; c <= right; ++c) {
         int x = targetLeft + c * squareSize;
         graphics.drawLine(x, ytop, x, ybottom);
      }
      int xleft = targetLeft + left * squareSize;
      int xright = targetLeft + right * squareSize;
      for (int r = top; r <= bottom; ++r) {
         int y = targetTop + r * squareSize;
         graphics.drawLine(xleft, y, xright, y);
      }
   }

   private void drawRect(RectLoc loc, boolean sleep) {
      int rectW = loc.r - loc.l;
      int rectH = loc.b - loc.t;
      // Basic Rectangle
      graphics.setColor(RECT_FILL);
      int fillX = loc.l * squareSize + targetLeft + SEP_WIDTH;
      int fillY = loc.t * squareSize + targetTop + SEP_WIDTH;
      int fillW = rectW * squareSize - SEP_WIDTH;
      int fillH = rectH * squareSize - SEP_WIDTH;
      graphics.fillRect(fillX, fillY, fillW, fillH);

      // Highlight loop
      graphics.setColor(HIGHLIGHT_COLOR);
      Stroke curStroke = graphics.getStroke();
      graphics.setStroke(HIGHLIGHT_STROKE);
      graphics.drawRoundRect(
              fillX + HIGHLIGHT_OFFSET - SEP_WIDTH, fillY + HIGHLIGHT_OFFSET - SEP_WIDTH,
              fillW - 2 * HIGHLIGHT_OFFSET + SEP_WIDTH, fillH - 2 * HIGHLIGHT_OFFSET + SEP_WIDTH,
              HIGHLIGHT_ARC, HIGHLIGHT_ARC);
      graphics.setStroke(curStroke);

      // Label
      graphics.setFont(LABEL_FONT);
      graphics.setColor(FONT_COLOR);
      String label;
      if (rectW >= rectH) {
         label = String.format("%dx%d", rectW, rectH);
      } else {
         label = String.format("%dx%d", rectH, rectW);
      }
      Rectangle2D labelRect = graphics.getFontMetrics().getStringBounds(label, graphics);
      int centerX = fillX + fillW / 2 + SEP_WIDTH;
      int centerY = fillY + fillH / 2 + SEP_WIDTH;
      AffineTransform oldTransform = graphics.getTransform();
      graphics.translate(centerX, centerY);
      if (rectW < rectH) {
         graphics.rotate(Math.PI / 2);
      }
      graphics.drawString(label, -(int)Math.round(labelRect.getCenterX()), -(int)Math.round(labelRect.getCenterY()));
      graphics.setTransform(oldTransform);

      // Sleep if wanted
      if (sleep) {
         drawing.sleep(drawSleepTime);
      }
   }

   private void clearRect(RectLoc loc) {
      graphics.setColor(BACKGROUND_COLOR);
      int fillX = loc.l * squareSize + targetLeft;
      int fillY = loc.t * squareSize + targetTop;
      int fillW = (loc.r - loc.l) * squareSize;
      int fillH = (loc.b - loc.t) * squareSize;
      graphics.fillRect(fillX, fillY, fillW, fillH);
      drawSeparators(loc.l, loc.r, loc.t, loc.b);
      drawing.sleep(drawSleepTime);
   }
}
