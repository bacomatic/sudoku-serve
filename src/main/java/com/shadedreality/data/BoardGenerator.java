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

/**
 * Board generator. Handles board generation asynchronously.
 *
 * TODO: Create generator executor pool to avoid overloading the system.
 */
public class BoardGenerator {
    // Discourage instantiation
    private BoardGenerator() {}

    /**
     * Kick off a generator running on the provided board.
     * @param board the board to generate.
     * @return a unique identifier for this board
     */
    public static String generateBoard(Board board) {
        GeneratorTask task = new GeneratorTask(board);
        String id = task.getGameBoard().getId();
        BoardRegistry.getRegistry().registerBoard(task.getGameBoard());
        return id;
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

    private static class GeneratorTask {
        private final Generator generator;
        private final GameBoard gameBoard;
        private final Thread genThread;

        public GeneratorTask(Board board) {
            generator = new Generator(board);
            gameBoard = new GameBoard(board);

            // spawn a thread to handle the generator
            genThread = new Thread(() -> {
               gameBoard.setProgress(0);
               gameBoard.setGenerated(false);

               // Set the monitor to setProgress, so we can see how far along it is
               generator.setMonitor(gameBoard::setProgress);
               generator.generate();

               gameBoard.setBoard(board.toIntArray());
               gameBoard.setProgress(100);
               gameBoard.setGenerated(true);
            });
            genThread.start();
        }

        public GameBoard getGameBoard() {
            return gameBoard;
        }
    }
}
