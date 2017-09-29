/*
 * Copyright (C) 2016, 2017, Shaded Reality, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shadedreality.sudokugen;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A Board is a container for holding a generated Sudoku board.
 * @author ddehaven
 */
public class Board {
    public static final int MIN_BOARD_SIZE = 2;
    public static final int MAX_BOARD_SIZE = 3; // eventually 4 or 5, once I fix the gen algorithm

    private final int size; // dimension
    private final int groupCount;
    private final int cellCount;
    private long randomSeed;
    private boolean randomSeedSet;

    private final ArrayList<Cell> cells;
    private final ArrayList<CellGroup> rows;
    private final ArrayList<CellGroup> columns;
    private final ArrayList<CellGroup> blocks;
    
    /**
     * Create a traditional 3x3 Sudoku board.
     */
    public Board() {
        this(3);
    }
    
    /**
     * Create a new board of the given dimension. The board is composed of
     * NxN blocks of NxN cells each. There are no provisions for oddly sized
     * Boards.
     * @param dimension The number of blocks per side, or cells on each side of
     * each Block.
     */
    public Board(int dimension) {
        // sanity check dimension, don't allow absurd numbers
        if (dimension < MIN_BOARD_SIZE || dimension > MAX_BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid board dimension given ("+dimension+"), must be 2-5");
        }
        size = dimension;

        // NxN groups per type (rows, columns, blocks)
        groupCount = size * size;

        // NxN blocks of NxN cells, so size^4 (without the call to Math)
        cellCount = groupCount * groupCount;

        cells = new ArrayList<>(cellCount);
        rows = new ArrayList<>(groupCount);
        columns = new ArrayList<>(groupCount);
        blocks = new ArrayList<>(groupCount);

        // allocate the groups
        for (int index = 0; index < groupCount; index++) {
            rows.add(new CellGroup(groupCount));
            columns.add(new CellGroup(groupCount));
            blocks.add(new CellGroup(size, size));
        }

        // initialize the cells
        for (int index = 0; index < cellCount; index++) {
            Cell c = new Cell();
            cells.add(c);

            // calculate row, column and block, then set accordingly
            int row = index / groupCount;
            int col = index % groupCount;

            int blockRow = (row / size);
            int blockCol = (col / size);
            int block = (blockRow * size) + blockCol;

            // just swap col/row for indices
            rows.get(row).set(col, c);
            columns.get(col).set(row, c);
            // cell coords are just col % size, row % size
            blocks.get(block).set(col % size, row % size, c);

            // be sure to tell the Cell where it is
            c.setRow(rows.get(row));
            c.setColumn(columns.get(col));
            c.setBlock(blocks.get(block));
        }

        // validate all rows, cols and blocks
        for (int ii = 0; ii < groupCount; ii++) {
            if (!rows.get(ii).validate()) {
                throw new InternalError("row "+ii+" is not completely initialized");
            }
            if (!columns.get(ii).validate()) {
                throw new InternalError("column "+ii+" is not completely initialized");
            }
            if (!blocks.get(ii).validate()) {
                throw new InternalError("block "+ii+" is not completely initialized");
            }
        }
    }

    public Board(int dimension, long seed) {
        this(dimension);
        randomSeed = seed;
        randomSeedSet = true;
    }

    public int getSize() {
        return size;
    }
    
    public Cell[] getCells() {
        return cells.toArray(new Cell[cells.size()]);
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public boolean isRandomSeedSet() {
        return randomSeedSet;
    }

    /**
     * Get a block with the given index. Blocks are indexed 1..N starting at top
     * left and ending at bottom right. So, a 3x3 boards blocks will be TL, TM,
     * TR, ML, MM, MR, BL, BM, BR
     * @param index block index
     * @return Block at the given index
     */
    public CellGroup getBlock(int index) {
        if (index < 0 || index > blocks.size()) {
            throw new IndexOutOfBoundsException("Invalid block index: "+index);
        }
        return blocks.get(index);
    }
    
    /**
     * Resets the board to a clean state. All cells are reset to -1 value so the
     * board may be reused for a generator.
     */
    public void reset() {
        blocks.forEach(b -> b.reset());
    }
    
    public Stream<CellGroup> blockStream() {
        return blocks.stream();
    }
    
    /**
     * Invoke the given <code>Consumer</code> for each block on the
     * board. The blocks will be called in index order from top left to bottom
     * right. This is particularly useful with Lambdas.
     * 
     * Example:
     * <code>
     * board.forEachBlock(b -> b.print());
     * </code>
     *
     * @param r Consumer defining the method to be called on each block.
     */
    public void forEachBlock(Consumer<CellGroup> r) {
        blocks.forEach(r);
    }
    
    public void forEachRow(Consumer<CellGroup> r) {
        rows.forEach(r);
    }
    
    public void forEachColumn(Consumer<CellGroup> r) {
        columns.forEach(r);
    }
    
    public void print() {
        StringBuilder sb = new StringBuilder("    ");
        for (int ii = 0; ii < groupCount; ii++) {
            sb.append("-----");
            if (ii > 0 && ((ii % size) == 0)) {
                sb.append("---");
            }
        }
        String divider = sb.toString();
        
        for (int yy = 0; yy < groupCount; yy++) {
            sb = new StringBuilder();
            sb.append("    ");
            for (int xx = 0; xx < groupCount; xx++) {
                if (xx > 0 && ((xx % size) == 0)) {
                    sb.append(" | ");
                }
                sb.append(String.format(" %3d ", rows.get(yy).get(xx).getValue()));
            }

            if (yy > 0 && ((yy % size) == 0)) {
                System.out.println(divider);
            }
            System.out.println(sb.toString());
        }
    }

    public int[] toIntArray() {
        int[] outArray = new int[cellCount];
        for (int ii = 0; ii < cells.size(); ii++) {
            outArray[ii] = cells.get(ii).getValue();
        }
        return outArray;
    }
}
