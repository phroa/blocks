import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Given a file describing a board plus a set of blocks to place on the board,
 * use a recursive algorithm to find a way to place them all down.
 *
 * @author Jack Stratton (strattj2@wwu.edu) w/ pseudocode from Chris Reedy (Chris.Reedy@wwu.edu)
 */
public class Blocks {

    private static final String USAGE = "Usage: Blocks <file>\n\n\twhere <file> is the path to a properly formatted input file.";

    /**
     * Incremented by one for each recursive call to explore().
     */
    private static int calls;

    /**
     * Width and height of the grid to place all the blocks on.
     */
    private static int width;
    private static int height;

    /**
     * List of blocks given in the input file.
     *
     * A block, throughout this program, is described as a two-element int[] where the
     * first element is the block's width and the second element the block's height.
     */
    private static final List<int[]> inputBlocks = new ArrayList<>();

    /**
     * List of blocks that have been placed on the grid so far.
     */
    private static final List<int[]> used = new ArrayList<>();

    /**
     * The actual grid.  A value in the grid other than zero indicates that a block was
     * placed there - the value will be the call # that placed it there.
     */
    private static int[][] grid;

    /**
     * Hacky constants for getting parts of blocks as if they were object properties.
     *
     * rect[x] is... something like rect.x I guess?
     */
    private static final int x = 0;
    private static final int y = 1;

    /**
     * Handles printing and animating solutions.
     */
    private static DrawBlock drawBlock;

    /**
     * Program entry point.
     */
    public static void main(String[] args) throws Exception {
        if (!processArgs(args))
            return;

        // This sets up the global variables.
        readData(new Scanner(new File(args[0])));

        if (explore()) {
            System.out.println("Solved in " + calls + " calls");
        } else {
            System.out.println("Can't solve, took " + calls + " calls to find that out");
        }
    }

    /**
     * Backtracking recursive solver that places blocks on the grid,
     * recurses to place the next block, and if that doesn't help then it
     * removes the block from the grid and tries again with the next block.
     *
     * @return true if a complete solution was achieved
     */
    private static boolean explore() {
        calls++;

        for (int[] block : inputBlocks) {

            int[] next = findEmptyLocation();
            if (next == null)
                return true;
            if (used.contains(block))
                continue;

            // Check to see if the block will fit at `next`
            if (rectFits(block, next)) {
                // Place it
                place(block, next);
                // Keep track of it
                used.add(block);
                // Try placing the rest of the rectangles down after that one.
                if (explore())
                    return true;
                // Implicit 'else' - this rectangle at `next` didn't help solve the problem.
                clear(block, next);
                used.remove(block);
            }

            // Same thing, but try flipping the block around so that its width and height are swapped.
            int[] reversed = {block[y], block[x]};

            if (rectFits(reversed, next)) {
                place(reversed, next);
                used.add(block);
                if (explore())
                    return true;
                clear(reversed, next);
                used.remove(block);
            }
        }

        // At this point we have failed to find a solution, return false
        // indicating failure
        return false;
    }

    /**
     * Fills the grid with a given value, starting from `startingLocation`
     * and ending at `startingLocation` plus the given block's dimensions.
     *
     * @param block The size of the block to fill in
     * @param startingLocation Where to start filling the grid from
     * @param filledValue The value to place in the grid at every point in the rectangle
     */
    private static void fill(int[] block, int[] startingLocation, int filledValue) {
        for (int row = startingLocation[y]; row < startingLocation[y] + block[y]; row++) {
            for (int column = startingLocation[x]; column < startingLocation[x] + block[x]; column++) {
                grid[row][column] = filledValue;
            }
        }
    }

    /**
     * Checks to see if a block will fit in the grid at `startingLocation`
     * and won't overlap another already-placed block.
     *
     * @param block The block to try and place
     * @param startingLocation Where to try to place the block down
     * @return Whether the block will fit at `startingLocation`
     */
    private static boolean rectFits(int[] block, int[] startingLocation) {
        for (int row = 0; row < block[y]; row++) {
            for (int column = 0; column < block[x]; column++) {
                if (row + startingLocation[y] >= grid.length // Vertical bounds check
                        || column + startingLocation[x] >= grid[row].length // Horizontal bounds check
                        || grid[row + startingLocation[y]][column + startingLocation[x]] != 0) // Overlap check
                    return false;
            }
        }
        return true;
    }

    /**
     * Find a place on the grid that a block isn't occupying.
     *
     * @return The coordinates of an empty position, or null if the entire grid is filled up.
     */
    private static int[] findEmptyLocation() {
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                if (grid[row][column] == 0)
                    return new int[]{column, row};
            }
        }
        return null;
    }

    /**
     * Places a block at a given location.  This method assumes the block will fit no matter what.
     *
     * @param block The block to place
     * @param startingLocation Where to place the block
     */
    private static void place(int[] block, int[] startingLocation) {
        drawBlock.placeRect(block[x], block[y], startingLocation[x], startingLocation[y]);
        // Fill with the current call number, to help in debugging. Anything nonzero will work.
        fill(block, startingLocation, calls);
    }

    /**
     * Removes the given block from the given location.
     * This method assumes the block at `startingLocation` is accurately described by the parameter `block`.
     *
     * @param block The block to remove
     * @param startingLocation Where to remove it from
     */
    private static void clear(int[] block, int[] startingLocation) {
        drawBlock.clearRect(block[x], block[y], startingLocation[x], startingLocation[y]);
        fill(block, startingLocation, 0);
    }

    /**
     * Read the file given by `input` and set up the global variables with the data inside it.
     *
     * @param input The input data
     */
    private static void readData(Scanner input) {
        width = input.nextInt();
        height = input.nextInt();
        int blockCount = input.nextInt();

        drawBlock = new DrawBlock(width, height);
        grid = new int[height][width];

        for (int i = 0; i < blockCount; i++) {
            int[] block = {input.nextInt(), input.nextInt()};
            inputBlocks.add(block);
            drawBlock.useRect(block[x], block[y]);
        }

        drawBlock.setupComplete();

        // This puts the largest (by area) blocks at the front of the list, so they are placed first.
        // This tends to help with extremely large problems, as it's hard to place a giant block down last.
        if (inputBlocks.size() > 8)
            inputBlocks.sort(Comparator.comparingInt(block -> -block[x] * block[y]));
    }

    /**
     * Handle the command-line argument.
     *
     * @param args The command-line arguments, as belonging to main()
     * @return Whether the arguments are valid
     */
    private static boolean processArgs(String[] args) {
        if (args.length != 1) {
            System.out.println(USAGE);
            return false;
        }

        // Using path objects eliminates the necessity of two File objects
        // pointing to the same file, both of which would never be closed...
        Path path = Paths.get(args[0]);

        if (!(Files.isReadable(path) && Files.isRegularFile(path))) {
            System.out.println(USAGE);
            return false;
        }

        return true;
    }
}
