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

import com.shadedreality.data.Randomeister;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Simple generator that uses backtracking. May not be the best in the world, but it seems to work for all supported
 * sizes.
 */
public class Generator {
    //                                           0  1  2  3        4          5
    private static final int[] MAX_GEN_COUNTS = {0, 0, 0, 1000000, 100000000, 1000000000};

    // Board to generate, we'll start with the params provided in this object
    // and when finished populate the board cell array
    private final Board board;

    private final int cellCount;
    private final Random genRandom = new Random();
    private Consumer<Integer> genMonitor = null;
    private boolean randomizedSeed = false;

    private int lastPct = 0; // to prevent us from "progressing" backwards
    public void setMonitor(Consumer<Integer> monitor) {
        genMonitor = monitor;
    }

    private void monitorUpdate(int loc, int max) {
        if (genMonitor == null) return;

        int pct = loc * 100 / max;
        if (pct > lastPct) {
            genMonitor.accept(pct);
            lastPct = pct;
        }
    }

    public Generator(Board b) {
        board = b;
        int size = board.getSize();
        cellCount = size * size * size * size;

        long seed = board.getRandomSeed();
        if (seed == 0) {
            seed = Randomeister.randomLong();
            randomizedSeed = true;
            board.setRandomSeed(seed); // update board with actual seed
        }
        genRandom.setSeed(seed);
    }

    public boolean generate() {
        int tryCount = 5;
        int loopCount = 0;
        int maxCount = MAX_GEN_COUNTS[board.getSize()];
        int cellIndex = 0;
        boolean backtrack = false;
        // keep a local copy of this for quick access
        // From each cell we can access what we need
        Cell[] cells = board.getCells();

        // clear any existing cells
        board.reset();

        while (cellIndex < cellCount) {
            if (cellIndex < 0) {
                throw new InternalError("Cell index should not fall below zero!");
            }
            monitorUpdate(cellIndex, cellCount);

            loopCount++;
            if (maxCount > 0 && loopCount > maxCount) {
                // Report for posterity, and so we can check this in the future.
                System.out.println("Loop limit reached! ("+maxCount+"), giving up board generation.");
                System.out.println("The seed that caused this: "+board.getRandomSeed()+" with size "+board.getSize());
                board.reset();
                // if we chose a randomized seed, try another
                if (randomizedSeed && tryCount > 0) {
                    board.setRandomSeed(Randomeister.randomLong());
                    genRandom.setSeed(board.getRandomSeed());

                    tryCount--;
                    loopCount = 0;
                    cellIndex = 0;
                    backtrack = false;
                    continue;
                }
                return false;
            }

            Cell cc = cells[cellIndex];
            if (backtrack) {
                // continue to backtrack until we get to a cell that has at least one number available
                if (cc.invalidateValue() == 0) {
                    cc.reset();
                    cellIndex--;
                    continue;
                }
                backtrack = false;
            } else {
                if (cc.calculateAvailableValues() == 0) {
                    // no values available
                    backtrack = true;
                    cellIndex--;
                    continue;
                }
            }

            // Randomly choose an available value
            cc.chooseRandomValue(genRandom);
            cellIndex++;
        }

        // If we get this far, it succeeded
        System.out.println("Generated board after "+loopCount+" tries");
        return true;
    }
}
