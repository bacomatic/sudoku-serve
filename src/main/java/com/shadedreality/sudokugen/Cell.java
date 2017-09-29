/*
 * Copyright (C) 2016, Shaded Reality, All Rights Reserved.
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Individual cell in a Sudoku Board. Each cell is assigned a value at generation
 * time and tracks a guessed value when playing. Each cell also keeps track of
 * it's row, column and block to aid in board generation and gameplay.
 * 
 * @author ddehaven
 */
public class Cell {
    private int generatedValue;
    private int guessedValue;
    
    private CellGroup row;
    private CellGroup column;
    private CellGroup block;
    
    private boolean locked;
    
    private volatile List<Cell> otherCells = null;
    
    public Cell() {
        generatedValue = guessedValue = -1;
    }
    
    private synchronized Stream<Cell> getCellStream() {
        if (row == null || column == null || block == null) {
            // not finished initializing yet...
            throw new InternalError("Cell is not finished being initialized!");
        }
        if (otherCells == null) {
            Stream<Cell> rowsNotMe = row.stream().filter(c -> c != this);
            Stream<Cell> colsNotMe = column.stream().filter(c -> c != this);
            Stream<Cell> blockNotMe = block.stream().filter(c -> c != this);
            
            // Concat all three streams and use distinct to remove duplicates
            // each stream should contain (NxN * 3) - 1 - N - N
            // -1 from the block, -N from the row and -N from the column
            // for example, for 3x3, each stream should contain:
            // (3x3 x 3) - 1 - 3 - 3 = (9 x 3) - 7 = 27 - 7 = 20
            otherCells = Stream.concat(Stream.concat(rowsNotMe, colsNotMe), blockNotMe)
                         .distinct()
                         .collect(Collectors.toList());
        }
        return otherCells.stream();
    }
    // We don't know the dimensions of the board, so we won't check value bounds
    // here. That will be up to the board generator itself.
    // though, all we need to do is check the dimensions of row/col/block
    public boolean canSetValue(int v) {
        if (generatedValue == v || v == -1) {
            return true; // already set or can always set -1
        }
        return !getCellStream().anyMatch(c -> c.getValue() == v);
    }

    public boolean setValue(int v) {
        if (locked) {
            return false;
        }
        if (canSetValue(v)) {
            generatedValue = v;
            return true;
        }
        return false;
    }
    
    public int getValue() {
        return generatedValue;
    }
    
    // when locked, value cannot change
    public void setLocked(boolean b) {
        locked = b;
    }
    
    public boolean isLocked() {
        return locked;
    }
    
    public void setGuess(int g) {
        guessedValue = g;
    }
    
    public int getGuess() {
        return guessedValue;
    }
    
    public void setRow(CellGroup r) {
        row = r;
    }
    
    public CellGroup getRow() {
        return row;
    }
    
    public void setColumn(CellGroup c) {
        column = c;
    }
    
    public CellGroup getColumn() {
        return column;
    }
    
    public void setBlock(CellGroup b) {
        block = b;
    }
    
    public CellGroup getBlock() {
        return block;
    }

    public void reset() {
        locked = false;
        generatedValue = -1;
        guessedValue = -1;
    }
    
    /**
     * Force the value of the cell with no checks.
     * @param v 
     */
    public void forceValue(int v) {
        generatedValue = v;
    }
}
