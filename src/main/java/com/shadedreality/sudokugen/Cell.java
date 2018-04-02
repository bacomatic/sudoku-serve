/*
 * Copyright (C) 2016, 2018, Shaded Reality, All Rights Reserved.
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
import java.util.Random;

/**
 * Individual cell in a Sudoku Board. Each cell is assigned a value at generation
 * time and tracks a guessed value when playing. Each cell also keeps track of
 * it's row, column and block to aid in board generation.
 */
public class Cell {
    private int value;

    private CellGroup row;
    private CellGroup column;
    private CellGroup block;

    private final List<Cell> peerCells = new ArrayList<>();
    private final List<Integer> availableValues = new ArrayList<>();

    public Cell() {
        value = -1;
    }
    
    private List<Cell> getPeerCells() {
        if (row == null || column == null || block == null) {
            // not finished initializing yet...
            throw new InternalError("Cell is not finished being initialized!");
        }

        // initialize if necessary
        synchronized (peerCells) {
            if (peerCells.isEmpty()) {
                row.forEach((c) -> {
                    if (c != this && !peerCells.contains(c)) {
                        peerCells.add(c);
                    }
                });
                column.forEach((c) -> {
                    if (c != this && !peerCells.contains(c)) {
                        peerCells.add(c);
                    }
                });
                block.forEach((c) -> {
                    if (c != this && !peerCells.contains(c)) {
                        peerCells.add(c);
                    }
                });
            }
        }
        return peerCells;
    }

    public int getValue() {
        return value;
    }
    
    public void setRow(CellGroup r) {
        row = r;
    }
    
    public void setColumn(CellGroup c) {
        column = c;
    }
    
    public void setBlock(CellGroup b) {
        block = b;
    }
    
    public void reset() {
        value = -1;
        availableValues.clear();
    }

    /**
     * Called when a new cell is being processed in the generator.
     * @return the number of available values
     */
    public int calculateAvailableValues() {
        // start clean
        reset();

        // Start with all values
        for (int ii = 0; ii < block.getSize(); ii++) {
            availableValues.add(ii);
        }

        // Filter out used values
        for (Cell c : getPeerCells()) {
            if (c.getValue() != -1) {
                availableValues.remove((Integer)c.getValue());
            }
        }

        return availableValues.size();
    }

    /**
     * Called when backtracking during board generation. This removes the current value from
     * the list of available values so it cannot be chosen.
     * @return the number of remaining available values
     */
    public int invalidateValue() {
        // Must box, or it will assume value is index
        availableValues.remove((Integer)value);
        value = -1;
        return availableValues.size();
    }

    /**
     * Choose a random value from the list of available values.
     * @param r PRNG used by the generator
     */
    public void chooseRandomValue(Random r) {
        if (availableValues.size() == 1) {
            value = availableValues.get(0);
        } else {
            int index = r.nextInt(availableValues.size());
            value = availableValues.get(index);
        }
    }

    /**
     * Force the value of the cell with no checks.
     * @param v 
     */
    public void forceValue(int v) {
        value = v;
    }
}
