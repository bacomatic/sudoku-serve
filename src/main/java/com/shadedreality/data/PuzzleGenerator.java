/*
 * Copyright (C) 2018, Shaded Reality, All Rights Reserved.
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

package com.shadedreality.data;

import java.util.*;

/**
 * Puzzle generator. Handles puzzle generation asynchronously.
 *
 * TODO: Create generator executor pool to avoid overloading the system.
 */
public class PuzzleGenerator {
    // Used to synchronize access to tasks
    private static final Object puzzleGenLock = new Object();
    private static Map<String, PuzzleTask> taskMap = Collections.synchronizedMap(new HashMap<>());

    // Discourage instantiation
    private PuzzleGenerator() {}

    /**
     * Kick off a generator running using the query parameters from a REST call.
     * @param queryParams parameters for the generator to use to generate the puzzle
     * @return a unique identifier for this puzzle
     */
    public static String generatePuzzle(QueryParams queryParams) {
        int size = 3;
        long randomSeed = 0;
        int difficulty = 4;
        PuzzleTask task;
        String puzzleId;

        if (queryParams.hasSize()) {
            size = queryParams.getSize();
        }
        if (queryParams.hasRandomSeed()) {
            randomSeed = queryParams.getRandomSeed();
        }
        if (queryParams.hasDifficulty()) {
            difficulty = queryParams.getDifficulty();
        }
        task = new PuzzleTask(size, randomSeed, difficulty);
        puzzleId = task.getPuzzle().getPuzzleId();
        synchronized (puzzleGenLock) {
            taskMap.put(puzzleId, task);
            task.start();
        }
        return puzzleId;
    }

    /**
     * Gets a Puzzle while it's being generated.
     * @param puzzleId unique Id for the puzzle to get
     * @return Puzzle if it's being generated or null if it does not exist
     */
    public static Puzzle getPuzzle(String puzzleId) {
        PuzzleTask task;
        synchronized (puzzleGenLock) {
            task = taskMap.get(puzzleId);
        }
        if (task == null) {
            return null;
        }
        return task.getPuzzle();
    }

    public static List<Puzzle> query(QueryParams queryParams) {
        ArrayList<Puzzle> outList = new ArrayList<>();
        synchronized (puzzleGenLock) {
            taskMap.values().forEach(task -> {
                Puzzle pz = task.getPuzzle();
                if (pz.matchQuery(queryParams)) {
                    if (!queryParams.checkSkip()) {
                        outList.add(pz);
                        // We can't break a forEach loop, without resorting to ugly hacks
                        // this will set a kill flag when the limit is reached so we'll skip
                        // anything past the limit
                        queryParams.checkLimit();
                    }
                }
            });
        }
        return outList;
    }

    public static long count(QueryParams queryParams) {
        // My kingdom for some real closures!!!
        final long[] counts = new long[1];
        synchronized (puzzleGenLock) {
            taskMap.values().forEach(task -> {
                Puzzle pz = task.getPuzzle();
                if (pz != null && pz.matchQuery(queryParams)) {
                    counts[0]++;
                }
            });
        }
        return counts[0];
    }
    /**
     * Gets the progress of a generator.
     * @param puzzleId unique Id of the board to check
     * @return percentage of completion or null if no puzzle with that ID being generated
     * @throws IllegalArgumentException if puzzleId does not exist in task list
     */
    public static Integer getPuzzleProgress(String puzzleId) {
        PuzzleTask task;
        synchronized (puzzleGenLock) {
            task = taskMap.get(puzzleId);
        }
        if (task == null) {
            return null;
        }
        return task.getProgress();
    }

    private static class PuzzleTask {
        private int progress;
        private Puzzle puzzle;
        private String gameBoardId; // needed to get board gen progress

        PuzzleTask(int size, long randomSeed, int difficulty) {
            gameBoardId = null;
            // Make puzzle object to hold our generator parameters
            this.puzzle = new Puzzle(size, randomSeed, difficulty);
        }

        void gameBoardFinished(GameBoard gameBoard) {
            progress = 50;
            puzzle.setBoard(gameBoard.getBoard());

            // Update random seed if zero (random random)
            if (puzzle.getRandomSeed() == 0) {
                puzzle.setRandomSeed(gameBoard.getRandomSeed());
            }

            // generate a bogus puzzle, for now
            // TODO: Share PRNG between board gen and puzzle gen, finish puzzle gen first
            // FIXME: add real puzzle generator
            switch (puzzle.getSize()) {
                case 2:
                    puzzle.setPuzzle(new int[] {
                            0,1, 1,1,
                            1,1, 0,1,

                            1,1, 1,0,
                            1,0, 1,1,
                    });
                    break;
                case 3:
                    puzzle.setPuzzle(new int[] {
                            0,1,1, 1,1,1, 1,1,1,
                            1,1,1, 1,0,1, 1,1,1,
                            1,1,1, 1,1,1, 1,1,0,

                            1,0,1, 1,1,1, 1,1,1,
                            1,1,1, 1,1,0, 1,1,1,
                            1,1,1, 1,1,1, 0,1,1,

                            1,1,0, 1,1,1, 1,1,1,
                            1,1,1, 0,1,1, 1,1,1,
                            1,1,1, 1,1,1, 1,0,1,
                    });
                    break;
                case 4:
                    puzzle.setPuzzle(new int[] {
                            0,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,0,1,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,1,0,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,0,

                            1,0,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,0,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,1,1,0, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,1,1,1, 0,1,1,1,

                            1,1,0,1, 1,1,1,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,0, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 0,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,1,1,1, 1,0,1,1,

                            1,1,1,0, 1,1,1,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 0,1,1,1, 1,1,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,0,1,1, 1,1,1,1,
                            1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,0,1,
                    });
                    break;
            }
            progress = 100;
            // Register the puzzle with PuzzleRegistry
            // This needs to be synchronized otherwise there's a very very slim chance
            // we could get a puzzle requested by this ID *between* these two calls
            // This works because the caller knows to check the registry if it's not found in the generator
            synchronized (puzzleGenLock) {
                PuzzleRegistry.getRegistry().registerPuzzle(puzzle);
                taskMap.remove(puzzle.getPuzzleId());
            }
        }

        Puzzle getPuzzle() {
            return puzzle;
        }

        void start() {
            gameBoardId = BoardGenerator.generateBoard(puzzle.getSize(), puzzle.getRandomSeed(), this::gameBoardFinished);
        }

        int getProgress() {
            if (gameBoardId == null) {
                return 0; // not started yet
            }
            // board gen progress is 50%
            Integer pct = BoardGenerator.getBoardProgress(gameBoardId);
            if (pct != null) {
                return pct / 2;
            }
            // otherwise we're tracking the puzzle generator progress
            return progress;
        }

        void setProgress(int progress) {
            this.progress = progress;
        }
    }
}
