import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Blocks {

    private static final String USAGE = "Usage: Blocks <file>\n\n\twhere <file> is the path to a properly formatted input file.";
    private static int calls;

    private static int width;
    private static int height;
    private static final List<int[]> inputBlocks = new ArrayList<>();
    private static int[][] actualGrid;

    /**
     * Hacky constants for getting parts of rectangles.
     */
    private static final int x = 0;
    private static final int y = 1;
    private static DrawBlock drawBlock;

    public static void main(String[] args) throws Exception {
        if (!processArgs(args))
            return;

        readData(new Scanner(new File(args[0])));

        drawBlock.setupComplete();

        if (explore(actualGrid, new int[]{0, 0})) {
            System.out.println("Solved in " + calls + " calls");
        } else {
            System.out.println("Can't solve, took " + calls + " calls to find that out");
        }

        drawBlock.printRect();
    }

    private static boolean explore(int[][] searchSpace, int[] absoluteStartingPosition) {
        calls++;
        System.out.println("calls = " + calls);
        System.out.println("trying to fit blocks in a " + searchSpace[0].length + " x " + searchSpace.length + " area");

        int[] relativeStartingPosition;
        // TODO WHILE?
        if ((relativeStartingPosition = findEmptyCell(searchSpace)) != null) {
            int[][] relativeEmptySpace = findEmptyArea(searchSpace, relativeStartingPosition);
            for (int[] block : inputBlocks) {
                int[] reversed = {block[y], block[x]};
                int[] actualLocation = {
                        absoluteStartingPosition[x] + relativeStartingPosition[x],
                        absoluteStartingPosition[y] + relativeStartingPosition[y]};

                System.out.println("relativeEmptySpace = " + Arrays.deepToString(relativeEmptySpace));
                System.out.println("block = " + Arrays.toString(block));
                System.out.println("rectFits(block)    = " + rectFits(relativeEmptySpace, block, relativeStartingPosition));
                System.out.println("rectFits(reversed) = " + rectFits(relativeEmptySpace, reversed, relativeStartingPosition));
                System.out.println();
                if (rectFits(relativeEmptySpace, block, relativeStartingPosition)) //noinspection Duplicates
                {
                    place(block, actualLocation);
                    inputBlocks.remove(block);
                    drawBlock.printRect();
                    System.out.println();

                    fill(relativeEmptySpace, block, relativeStartingPosition, calls);

                    int[] newEmptyCell = findEmptyCell(relativeEmptySpace);
                    if (newEmptyCell == null)
                        return true;
                    int[][] newEmptyArea = findEmptyArea(relativeEmptySpace, newEmptyCell);
                    int[] newActualLocation = {
                            absoluteStartingPosition[x] + newEmptyCell[x],
                            absoluteStartingPosition[y] + newEmptyCell[y]};
                    return explore(newEmptyArea, newActualLocation);
                } else if (rectFits(relativeEmptySpace, reversed, relativeStartingPosition)) //noinspection Duplicates
                {
                    place(reversed, actualLocation);
                    inputBlocks.remove(block);
                    drawBlock.printRect();
                    System.out.println();

                    fill(relativeEmptySpace, reversed, relativeStartingPosition, calls);

                    int[] newEmptyCell = findEmptyCell(relativeEmptySpace);
                    if (newEmptyCell == null)
                        return true;
                    int[][] newEmptyArea = findEmptyArea(relativeEmptySpace, newEmptyCell);
                    int[] newActualLocation = {
                            absoluteStartingPosition[x] + newEmptyCell[x],
                            absoluteStartingPosition[y] + newEmptyCell[y]};
                    return explore(newEmptyArea, newActualLocation);
                }
            }
        }



        return false;
    }

    private static void fill(int[][] grid, int[] rect, int[] where, int filling) {
        for (int i = where[y]; i < rect[y]; i++) {
            for (int j = where[x]; j < rect[x]; j++) {
                grid[j][i] = filling;
            }
        }
    }

    private static boolean rectFits(int[][] space, int[] rect, int[] where) {

        for (int i = where[y]; i < rect[y]; i++) {
            for (int j = where[x]; j < rect[x]; j++) {
                if (i >= space[0].length || j >= space.length || space[j][i] != 0)
                    return false;
            }
        }
        return true;
    }

    private static int[] findEmptyCell(int[][] searchSpace) {
        for (int i = 0; i < searchSpace.length; i++) {
            for (int j = 0; j < searchSpace[i].length; j++) {
                if (searchSpace[j][i] == 0)
                    return new int[]{j, i};
            }
        }
        return null;
    }

    private static int[][] findEmptyArea(int[][] searchSpace, int[] relativeStartingPosition) {
        int width = 0;
        int height = 0;
        int i = relativeStartingPosition[x];
        int j = relativeStartingPosition[y];

        // First row
        while (i < searchSpace[j].length - 1 && searchSpace[i][j] == 0) {
            width++;
            i++;
        }

        // Other rows
        outer:
        for (; j < searchSpace.length; j++) {
            for (int k = 0; k < searchSpace[j].length; k++) {
                if (searchSpace[j][k] != 0)
                    break outer;
            }
            height++;
        }
        height++;

        int[][] out = new int[width][height];
        return out;
    }

    private static void place(int[] rect, int[] nextPosition) {
        drawBlock.placeRect(rect[x], rect[y], nextPosition[x], nextPosition[y]);
        fill(actualGrid, rect, nextPosition, calls);
    }

    private static void readData(Scanner input) {
        String[] firstLine = input.nextLine().split(" ");
        width = Integer.parseInt(firstLine[0]);
        height = Integer.parseInt(firstLine[1]);
        int initial = Integer.parseInt(firstLine[2]);

        drawBlock = new DrawBlock(width, height);
        actualGrid = new int[width][height];

        for (int i = 0; i < initial; i++) {
            int[] rect = {input.nextInt(), input.nextInt()};
            inputBlocks.add(rect);
            drawBlock.useRect(rect[x], rect[y]);
        }
//        inputBlocks.sort(Comparator.comparingInt(r -> -r[x] * r[y]));
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
