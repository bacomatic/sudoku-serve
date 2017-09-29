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
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 *
 * @author ddehaven
 */
public class Generator {
    private final Board board;

    private final int[] MAX_GEN_COUNTS = {0, 0, 1000000, 1000000, 10000000, 100000000};
    
    // map we'll store results in
    private final int blockSize;
    private final HashMap<Long, BlockPattern> patternMap = new HashMap<>();
    private long blockIteration = 0;

    private final Random genRandom = new Random();

    private Consumer<Integer> genMonitor = null;

    // iterations and counts can easily overflow int, so uze longs to store
    // such things
    
    // class to hold a specific board pattern
    // this will be used to store the first pattern where a new cell select
    // count is found. The count will be used as an index into a map
    static class BlockPattern {
        public long patternCount; // how many variations were found using this pattern (map key)
        public long iteration;    // which iteration this was found in
        public int[] pattern;    // the first pattern (indices of blocks used in order)
        public long bpCount;      // how many times this patternCount has been encountered
    }

    // loop over remaining blocks, recursively calling myself for each block
    // in remainder
    private void blockLoop(ArrayList<Integer> blockSequence, ArrayList<Integer> remainder) {
        if (remainder.size() > 0) {
            remainder.stream()
                     .forEach(blockIndex -> {
                        ArrayList<Integer> newSequence = (ArrayList)blockSequence.clone();
                        newSequence.add(blockIndex);

                        ArrayList<Integer> newRemainder = (ArrayList)remainder.clone();
                        newRemainder.remove(blockIndex);
                        blockLoop(newSequence, newRemainder);
                    });
        } else {
            blockIteration++;
            // end, run the pattern
            long count = doPatternScan(blockSequence);
            BlockPattern bp = patternMap.get(count);
            if (bp == null) {
                // new count, create a new entry
                bp = new BlockPattern();
                bp.patternCount = count;
                bp.iteration = blockIteration;
                bp.bpCount = 1;

                // have to unbox the array... streams are our friend here
                bp.pattern = blockSequence.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();

                System.out.printf("  new pattern count: %d, iteration %d\n", count, blockIteration);
                patternMap.put(count, bp);
            } else {
                // just bump the count
                bp.bpCount++;
            }
        }
    }

    private long doPatternScan(ArrayList<Integer> pattern) {
        board.reset();
        AtomicBoolean abort = new AtomicBoolean(false);
        AtomicLong bi = new AtomicLong(0);

        pattern.stream()
               .forEachOrdered(blockIndex -> {
            // stop processing if we hit a snag
            if (abort.get()) {
                return;
            }
            CellGroup block = board.getBlock(blockIndex);
            List<Cell> ac = block.getAvailableCells(1);
            if (!ac.isEmpty()) {
                int index = genRandom.nextInt(ac.size());
                ac.get(index).setValue(1);

                long patternCount = bi.longValue();
                if (patternCount > 0) {
                    patternCount *= ac.size();
                } else {
                    patternCount = ac.size();
                }
                bi.getAndSet(patternCount);
            } else {
                abort.getAndSet(true);
            }
        });

        return bi.longValue();
    }

    public void scanPatterns() {
        // reset things
        patternMap.clear();
        blockIteration = 0;

        ArrayList<Integer> startSequence = new ArrayList<>(); // empty list
        ArrayList<Integer> startRemainder = new ArrayList<>(blockSize);
        for (int ii = 0; ii < blockSize; ii++) {
            startRemainder.add(ii);
        }

        // dump the contents of the pattern map
        blockLoop(startSequence, startRemainder);

        System.out.printf("Scanned patterns over %d iterations\n", blockIteration);
        System.out.printf("Found %d distinct pattern counts\n", patternMap.size());
        System.out.println("");
    }

    public void setMonitor(Consumer<Integer> monitor) {
        genMonitor = monitor;
    }

    public Generator(Board b) {
        board = b;
        int size = board.getSize();
        blockSize = size * size;
    }

    public void setSeed(long seed) {
        genRandom.setSeed(seed);
    }

    public void generate() {
        int count = 0;
        int maxCount = MAX_GEN_COUNTS[board.getSize()];

        boolean success;
        if (board.isRandomSeedSet()) {
            setSeed(board.getRandomSeed());
        }

        StringBuilder sb;
        do {
            sb = new StringBuilder();
            success = doGenerate(sb);
            if (!success) {
                count++;
            }
        } while (!success && count < maxCount);

        if (success) {
            System.out.println("Generated board after "+count+" tries:");
            System.out.println(sb.toString());
        } else {
            System.out.printf("No boards after %d tries, giving up\n", maxCount);
        }
    }

    private void monitorUpdate(int loc, int max) {
        int pct = loc * 100 / max;
        genMonitor.accept(pct);
    }

    private boolean doGenerate(StringBuilder sb) {
        int size = board.getSize();
        int maxCell = size * size + 1;
        final AtomicInteger bi = new AtomicInteger(0);
        final AtomicBoolean abort = new AtomicBoolean(false);

        board.reset();

        // pre-generate one diagonal, this significantly reduces overhead but
        // might require backtracking
        // start at top left and go diagonally to bottom right
        // skip length is size+1, e.g., 4 for 3x3, 5 for 4x4
//        for (int ii = 0; ii < blockSize; ii += size+1) {
//            // pre-fill all the cells in this block with random numbers
//            CellGroup block = board.getBlock(ii);
//            List<Cell> cells = block.getUnset();
//            for (int vv = 1; vv < maxCell; vv++) {
//                // get a random cell from those remaining, assign it the next
//                // number
//                Cell c = cells.get(random(0, cells.size()-1));
//                // make sure we don't try to set it again
//                cells.remove(c);
//                c.setValue(vv);
//
//                // lock it so the generator below doesn't try to set it
//                c.setLocked(true);
//            }
//            block.setLocked(true);
//        }

        for (int ii = 1; ii < maxCell; ii++) {
            monitorUpdate(ii, maxCell);
            bi.getAndSet(0);
            int cellValue = ii;
            board.blockStream()
                 .filter(b -> !b.isLocked())
                 .forEach(block -> {
                // stop processing if we hit a snag
                if (abort.get()) {
                    return;
                }
                List<Cell> ac = block.getAvailableCells(cellValue);
                if (!ac.isEmpty()) {
                    int index = genRandom.nextInt(ac.size());
                    ac.get(index).setValue(cellValue);

                    int patternCount = bi.intValue();
                    if (patternCount > 0) {
                        patternCount *= ac.size();
                    } else {
                        patternCount = ac.size();
                    }
                    bi.getAndSet(patternCount);
                } else {
                    abort.getAndSet(true);
                }
            });

            if (abort.get()) {
                sb.append("Finished early.. need stepback code!!").append("\n");
                break;
            }
            sb.append("gen ").append(ii).append(" pattern count = ").append(bi.get()).append("\n");
        }
        return !abort.get();
    }

    private int random(int min, int max) {
        // bound is exclusive, so add 1 to make max inclusive
        return genRandom.nextInt(max-min+1)+min;
    }
}
