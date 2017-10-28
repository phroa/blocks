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
    private static int[][] actualGrid;

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

        boolean result = explore(actualGrid, origin);
        if (result) {
            System.out.println("Solved in " + calls + " calls");
            System.out.println(Arrays.deepToString(actualGrid));
        } else {
            System.out.println("Can't solve, took " + calls + " calls to find that out");
            System.out.println(Arrays.deepToString(actualGrid));
        }

        drawBlock.printRect();
    }

    /**
     * Assume searchSpace is a totally empty grid, try to fit any remaining blocks inside it.
     * This modifies the input array.
     *
     * @param searchSpace
     * @return
     */
    private static boolean explore(int[][] searchSpace, int[] absoluteLocation) {
        calls++;
        System.out.println("calls = " + calls);
        System.out.println("trying to fit blocks in a " + searchSpace[0].length + " x " + searchSpace.length + " area");

        if (used.size() == inputBlocks.size() || searchSpace[0].length == 0)
            return true;

        for (int[] block : inputBlocks) {
            if (used.contains(block))
                continue;

            int[] reversed = {block[y], block[x]};

            int[] use;

            if (rectFits(searchSpace, block, origin)) {
                use = block;
            } else if (rectFits(searchSpace, reversed, origin)) {
                use = reversed;
            } else {
                continue;
            }
            place(searchSpace, use, absoluteLocation);

            int[] location = findEmptyCell(searchSpace);
            if (location == null)
                return true;

            int[][] newSearchSpace = findEmptyArea(searchSpace, location);
            boolean res = explore(newSearchSpace, new int[]{absoluteLocation[x] + location[x], absoluteLocation[y] + location[y]});
            copy(newSearchSpace, searchSpace, location);
            return res;

        }
        return false;
    }

    private static void fill(int[][] grid, int[] rect, int[] where, int filling) {
        for (int row = where[y]; row < rect[y]; row++) {
            for (int column = where[x]; column < rect[x]; column++) {
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

    private static boolean rectFits(int[][] space, int[] rect, int[] where) {

        for (int row = where[y]; row < rect[y]; row++) {
            for (int column = where[x]; column < rect[x]; column++) {
                if (row >= space.length || column >= space[row].length || space[row][column] != 0)
                    return false;
            }
        }
        return true;
    }

    private static int[] findEmptyCell(int[][] searchSpace) {
        for (int row = 0; row < searchSpace.length; row++) {
            for (int column = 0; column < searchSpace[row].length; column++) {
                if (searchSpace[row][column] == 0)
                    return new int[]{column, row};
            }
        }
        return null;
    }

    private static int[][] findEmptyArea(int[][] searchSpace, int[] relativeStartingPosition) {
        int width = 0;
        int height = 0;
        int i = relativeStartingPosition[x];
        int row = relativeStartingPosition[y];

        // First row
        while (i < searchSpace[row].length && searchSpace[row][i] == 0) {
            width++;
            i++;
        }

        // Other rows
        outer:
        for (; row < searchSpace.length; row++) {
            for (int column = relativeStartingPosition[x]; column < searchSpace[row].length; column++) {
                if (searchSpace[row][column] != 0)
                    break outer;
            }
            height++;
        }

        int[][] out = new int[height][width];
        return out;
    }

    private static void place(int[][] subGrid, int[] rect, int[] absoluteLocation) {
        used.add(rect);
        drawBlock.placeRect(rect[x], rect[y], absoluteLocation[x], absoluteLocation[y]);
        fill(subGrid, rect, origin, calls);
    }

    private static void readData(Scanner input) {
        width = input.nextInt();
        height = input.nextInt();
        int blockCount = input.nextInt();

        drawBlock = new DrawBlock(width, height);
        actualGrid = new int[height][width];

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
