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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A group of cells. Rather than having a separate class for rows, columns and
 * blocks, which all contain the same number of cells, we have a single "group"
 * class that understands it's own dimensions.
 * @author ddehaven
 */
public class CellGroup {
    private final int width, height;
    private boolean locked = false;
    
    // Use ArrayList instead of Cell[] so we can make use of lambdas and streams
    // for efficiency
    private final ArrayList<Cell> cells = new ArrayList<>();
    
    public CellGroup(int width, int height) {
        this.width = width;
        this.height = height;
        init(width * height);
    }
    
    public CellGroup(int size) {
        width = size;
        height = 1;
        init(size);
    }

    private void init(int size) {
        // fill cells with null values so it's not empty
        cells.ensureCapacity(size);
        for (int ii = 0; ii < size; ii++) {
            cells.add(null);
        }
    }

    public void setLocked(boolean b) {
        locked = b;
    }
    
    public boolean isLocked() {
        return locked;
    }
    
    public Stream<Cell> stream() {
        return cells.stream();
    }

    public void forEach(Consumer<Cell> p) {
        cells.forEach(p);
    }

    public void set(int x, int y, Cell c) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("invalid cell coordinates: "+x+","+y);
        }
        set((y * height) + x, c);
    }
    
    public void set(int index, Cell c) {
        if (index > cells.size()) {
            throw new ArrayIndexOutOfBoundsException("cell index exceeds cell count");
        }
        if (cells.get(index) != null) {
            throw new IllegalArgumentException("cell at index "+index+" is already set!");
        }
        cells.set(index, c);
    }
    
    /**
     * Get a Cell at the given coordinates. For blocks.
     * @param x the x coordinate
     * @param y the y coordinate
     * @return 
     */
    public Cell get(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("invalid cell coordinates: "+x+","+y);
        }
        return get((y * height) + x);
    }

    /**
     * Get a linearly indexed Cell. For rows and columns.
     * @param index
     * @return 
     */
    public Cell get(int index) {
        if (index > cells.size()) {
            throw new ArrayIndexOutOfBoundsException("cell index exceeds cell count");
        }
        return cells.get(index);
    }
    
    public int getSize() {
        return cells.size();
    }

    /**
     * Find all cells that have no value set.
     * @return Cell array containing all the cells that are unset. This array
     * could be empty.
     */
    public List<Cell> getUnset() {
        return cells.stream()
                    .filter(c -> c.getValue() == -1)
                    .collect(Collectors.toList());
    }
    
    /**
     * Return an array of <code>Cells</code> that are unset (value == -1) and
     * can be set to the given value.
     * @param value the value to test
     * @return Cell array, possibly empty if no cells are available
     */
    public List<Cell> getAvailableCells(int value) {
        return getUnset().stream()
                    .filter(c -> c.canSetValue(value))
                    .collect(Collectors.toList());
    }
    
    /**
     * Find the cell with the given value.
     * @param value the value to find
     * @return cell if found, null otherwise
     */
    public Cell find(int value) {
        for (Cell c : cells) {
            if (c.getValue() == value) {
                return c;
            }
        }
        return null;
    }
    
    /**
     * Find the cell with the given guess.
     * @param guess the guess value to search for
     * @return cell with the given guess or null if not found
     */
    public Cell findGuess(int guess) {
        for (Cell c : cells) {
            if (c.getGuess() == guess) {
                return c;
            }
        }
        return null;
    }
    
    /**
     * Validate each cell in this group.
     * Rules:
     *  - each cell must exist (no null entries)
     *  - each cell must have a unique value
     *  - cells with no value (-1) are skipped
     * @return true if all cells are set and unique
     */
    public boolean validate() {
        int size = cells.size();
        // do null check first, so we don't have to do it twice in the next loops
        if (!cells.stream().noneMatch((c) -> (c == null))) {
            return false;
        }

        // 0..n-1, nothing to check against the last one
        // this could probably be optimized...
        for (int ii = 0; ii < size-1; ii++) {
            int value = cells.get(ii).getValue();
            if (value == -1) {
                continue; // skip unset cells
            }
            for (int jj = ii; jj < size; jj++) {
                int other = cells.get(jj).getValue();
                if (other == -1) {
                    continue;
                }
                if (other == value) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public void reset() {
        locked = false;
        cells.stream()
             .forEachOrdered((c) -> c.reset());
    }
    
    /**
     * Prints this CellGroup using it's given shape. Rows and Columns will both
     * be printed horizontally.
     */
    public void print() {
        for (int yy = 0; yy < height; yy++) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            for (int xx = 0; xx < width; xx++) {
                int value = cells.get(yy * height + xx).getValue();
                sb.append(String.format(" %3s ", value));
            }
            System.out.println(sb.toString());
        }
    }
}
