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

package com.shadedreality.data;

import com.shadedreality.sudokugen.Board;
import com.shadedreality.sudokugen.Generator;

import java.util.*;
import java.util.function.Consumer;

/**
 * Board generator. Handles board generation asynchronously.
 *
 * TODO: Create generator executor pool to avoid overloading the system.
 */
public class BoardGenerator {
    private static final Object generatorLock = new Object();
    private static Map<String, GeneratorTask> taskMap = Collections.synchronizedMap(new HashMap<>());

    // Discourage instantiation
    private BoardGenerator() {}

    /**
     * Kick off a generator running on the provided board.
     * @param size size of the board to generate
     * @param randomSeed random seed to be used, must be a valid see at this point
     * @return a unique identifier for this board
     */
    public static String generateBoard(int size, long randomSeed, Consumer<GameBoard> finishProc) {
        GeneratorTask task = new GeneratorTask(size, randomSeed, finishProc);
        String boardId = task.getGameBoard().getBoardId();
        synchronized (generatorLock) {
            taskMap.put(boardId, task);
        }
        task.start(); // call after adding to map to avoid race condition
        return boardId;
    }

    /**
     * Kick off generator using provided parameters. This version
     * allows specifying a random seed which produces the same results
     * each time. Future results not guaranteed as the underlying
     * algorithms may change.
     * @param queryParams parameters containing board information for the generator
     * @return unique id for the board being generated
     */
    public static String generateBoard(QueryParams queryParams, Consumer<GameBoard> finishProc) {
        int size = 3;
        long randomSeed = 0;
        if (queryParams.hasSize()) {
            size = queryParams.getSize();
        }
        if (queryParams.hasRandomSeed()) {
            randomSeed = queryParams.getRandomSeed();
        }
        return generateBoard(size, randomSeed, finishProc);
    }

    /**
     * Gets a GameBoard while it's being generated.
     * @param boardId unique Id for the board to get
     * @return GameBoard if it's being generated or null if it does not exist
     */
    public static GameBoard getBoard(String boardId) {
        GeneratorTask task;
        synchronized (generatorLock) {
            task = taskMap.get(boardId);
        }
        if (task == null) {
            return null;
        }
        return task.getGameBoard();
    }

    public static List<GameBoard> query(QueryParams queryParams) {
        ArrayList<GameBoard> outList = new ArrayList<>();

        synchronized (generatorLock) {
            taskMap.values().forEach(task -> {
                GameBoard gb = task.getGameBoard();
                if (gb.matchQuery(queryParams)) {
                    if (!queryParams.checkSkip()) {
                        outList.add(gb);
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

        synchronized (generatorLock) {
            taskMap.values().forEach(task -> {
                GameBoard gb = task.getGameBoard();
                if (gb != null && gb.matchQuery(queryParams)) {
                    counts[0]++;
                }
            });
        }
        return counts[0];
    }
    /**
     * Gets the progress of a generator.
     * @param boardId unique Id of the board to check
     * @return percentage of completion
     * @throws IllegalArgumentException if boardId does not exist in task list
     */
    public static Integer getBoardProgress(String boardId) {
        GeneratorTask task;
        synchronized (generatorLock) {
            task = taskMap.get(boardId);
        }
        if (task == null) {
            return null;
        }
        return task.getProgress();
    }

    private static class GeneratorTask {
        private final Generator generator;
        private final GameBoard gameBoard;
        private int progress;
        private final Thread genThread;

        GeneratorTask(final int size, final long randomSeed, final Consumer<GameBoard> finishProc) {
            Board board = new Board(size, randomSeed);
            generator = new Generator(board);
            gameBoard = new GameBoard(size, randomSeed);

            // spawn a thread to handle the generator
            genThread = new Thread(() -> {
                // Set the monitor to setProgress, so we can see how far along it is
                progress = 0;
                generator.setMonitor(this::setProgress);
                generator.generate();
                progress = 100;

                gameBoard.setBoard(board.toIntArray());
                if (randomSeed == 0) {
                    // If zero random seed, get actual seed used
                    gameBoard.setRandomSeed(board.getRandomSeed());
                }

                // Move from taskMap to database
                synchronized (generatorLock) {
                    BoardRegistry.getRegistry().registerBoard(gameBoard);
                    taskMap.remove(gameBoard.getBoardId());
                }

                // call finishProc if set
                if (finishProc != null) {
                    finishProc.accept(gameBoard);
                }
            });
        }

        GameBoard getGameBoard() {
            return gameBoard;
        }

        void start() {
            // FIXME: Remove magic seed for production
            if (gameBoard.getRandomSeed() != 8675309L) {
                genThread.start();
            }
        }

        int getProgress() {
            return progress;
        }

        void setProgress(int progress) {
            this.progress = progress;
        }
    }
}
