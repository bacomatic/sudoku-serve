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

package com.shadedreality.data;

import com.shadedreality.sudokugen.Board;
import com.shadedreality.sudokugen.Generator;

import java.util.*;

/**
 * Board generator. Handles board generation asynchronously.
 *
 * TODO: Create generator executor pool to avoid overloading the system.
 */
public class BoardGenerator {
    private static Map<String, GeneratorTask> taskMap = Collections.synchronizedMap(new HashMap<>());

    // Discourage instantiation
    private BoardGenerator() {}

    /**
     * Kick off a generator running on the provided board.
     * @param board the board to generate.
     * @return a unique identifier for this board
     */
    public static String generateBoard(Board board) {
        GeneratorTask task = new GeneratorTask(board);
        String boardId = task.getGameBoard().getBoardId();
        taskMap.put(boardId, task);
        task.start(); // call after adding to map to avoid race condition
        return boardId;
    }

    /**
     * Kick off generator using provided parameters.
     * @param size size of the board to generate
     * @return unique id for the board being generated
     */
    public static String generateBoard(int size) {
        return generateBoard(new Board(size));
    }

    /**
     * Kick off generator using provided parameters. This version
     * allows specifying a random seed which produces the same results
     * each time. Future results not guaranteed as the underlying
     * algorithms may change.
     * @param size size of the board to generate
     * @param randomSeed random seed to be used
     * @return unique id for the board being generated
     */
    public static String generateBoard(int size, long randomSeed) {
        return generateBoard(new Board(size, randomSeed));
    }

    /**
     * Gets a GameBoard while it's being generated.
     * @param boardId unique Id for the board to get
     * @return GameBoard if it's being generated or null if it does not exist
     */
    public static GameBoard getBoard(String boardId) {
        GeneratorTask task = taskMap.get(boardId);
        if (task == null) {
            return null;
        }
        return task.getGameBoard();
    }

    public static List<GameBoard> query(QueryParams queryParams) {
        ArrayList<GameBoard> outList = new ArrayList<>();

        taskMap.values().forEach(task -> {
            GameBoard gb = task.getGameBoard();
            if (gb.matchQuery(queryParams)) {
                if (!queryParams.checkSkip()) {
                    outList.add(gb);
                    // We can't break a forEach loop, without resorting to ugly hacks
                    // this will set a kill flag when the limit is reached so we'll skip
                    // anythis past the limit
                    queryParams.checkLimit();
                }
            }
        });
        return outList;
    }

    public static long count(QueryParams queryParams) {
        // My kingdom for some real closures!!!
        final long[] counts = new long[1];

        taskMap.values().forEach(task -> {
            GameBoard gb = task.getGameBoard();
            if (gb != null && gb.matchQuery(queryParams)) {
                counts[0]++;
            }
        });

        return counts[0];
    }
    /**
     * Gets the progress of a generator.
     * @param boardId unique Id of the board to check
     * @return percentage of completion
     * @throws IllegalArgumentException if boardId does not exist in task list
     */
    public static Integer getBoardProgress(String boardId) {
        GeneratorTask task = taskMap.get(boardId);
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

        public GeneratorTask(Board board) {
            generator = new Generator(board);
            gameBoard = new GameBoard(board.getSize(), board.getRandomSeed());

            // spawn a thread to handle the generator
            genThread = new Thread(() -> {
                // Set the monitor to setProgress, so we can see how far along it is
                progress = 0;
                generator.setMonitor(this::setProgress);
                generator.generate();
                progress = 100;

                gameBoard.setBoard(board.toIntArray());

                // Add to database
                BoardRegistry.getRegistry().registerBoard(gameBoard);

                // remove from taskMap now that done
                taskMap.remove(gameBoard.getBoardId());
            });
        }

        public GameBoard getGameBoard() {
            return gameBoard;
        }

        private void start() {
            // FIXME: Remove magic seed for production
            if (gameBoard.getRandomSeed() != 8675309L) {
                genThread.start();
            }
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }
    }
}
