import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Blocks {

    private static final String USAGE = "Usage: Blocks <file>\n\n\twhere <file> is the path to a properly formatted input file.";
    private static int calls;

    private static int width;
    private static int height;
    private static final List<int[]> inputBlocks = new ArrayList<>();
    private static final List<int[]> used = new ArrayList<>();
    private static int[][] grid;

    /**
     * Hacky constants for getting parts of rectangles as if they were object properties.
     */
    private static final int x = 0;
    private static final int y = 1;

    private static final int[] origin = {0, 0};

    private static DrawBlock drawBlock;

    public static void main(String[] args) throws Exception {
        if (!processArgs(args))
            return;

        readData(new Scanner(new File(args[0])));

        drawBlock.setupComplete();

        if (explore()) {
            System.out.println("Solved in " + calls + " calls");
            System.out.println(Arrays.deepToString(grid));
        } else {
            System.out.println("Can't solve, took " + calls + " calls to find that out");
            System.out.println(Arrays.deepToString(grid));
        }

        drawBlock.printRect();
    }

    private static boolean explore() {
        calls++;
        System.out.println("calls = " + calls);

        for (int[] block : inputBlocks) {
            int[] reversed = {block[y], block[x]};

            int[] next = findEmptyLocation();
            if (next == null)
                return true;
            if (used.contains(block))
                continue;

            if (rectFits(block, next)) {
                place(block, next);
                used.add(block);
                if (explore())
                    return true;
                clear(block, next);
                used.remove(block);
            } else if (rectFits(reversed, next)) {
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

    private static void fill(int[] rect, int[] where, int filling) {
        for (int row = where[y]; row < where[y] + rect[y]; row++) {
            for (int column = where[x]; column < where[x] + rect[x]; column++) {
                grid[row][column] = filling;
            }
        }
    }

    private static void copy(int[][] src, int[][] dest, int[] where) {
        for (int row = 0; row < src.length; row++) {
            for (int column = 0; column < src[0].length; column++) {
                dest[row + where[y]][column + where[x]] = src[row][column];
            }
        }
        System.out.println();
    }

    private static boolean rectFits(int[] rect, int[] where) {
        for (int row = where[y]; row < rect[y]; row++) {
            for (int column = where[x]; column < rect[x]; column++) {
                if (row + where[y] >= grid.length || column + where[x] >= grid[row].length || grid[row][column] != 0)
                    return false;
            }
        }
        return true;
    }

    private static int[] findEmptyLocation() {
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                if (grid[row][column] == 0)
                    return new int[]{column, row};
            }
        }
        return null;
    }

    private static void place(int[] block, int[] where) {
        drawBlock.placeRect(block[x], block[y], where[x], where[y]);
        fill(block, where, calls);
    }

    private static void clear(int[] block, int[] where) {
        drawBlock.clearRect(block[x], block[y], where[x], where[y]);
        fill(block, where, 0);
    }

    private static void readData(Scanner input) {
        width = input.nextInt();
        height = input.nextInt();
        int blockCount = input.nextInt();

        drawBlock = new DrawBlock(width, height);
        grid = new int[height][width];

        for (int i = 0; i < blockCount; i++) {
            int[] rect = {input.nextInt(), input.nextInt()};
            inputBlocks.add(rect);
            drawBlock.useRect(rect[x], rect[y]);
        }

        inputBlocks.sort(Comparator.comparingInt(block -> -block[x] * block[y]));
    }

    private static boolean processArgs(String[] args) {
        if (args.length != 1) {
            System.out.println(USAGE);
            return false;
        }

        Path path = Paths.get(args[0]);

        if (!(Files.isReadable(path) && Files.isRegularFile(path))) {
            System.out.println(USAGE);
            return false;
        }

        return true;
    }
}
