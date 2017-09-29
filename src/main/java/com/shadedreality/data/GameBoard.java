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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shadedreality.sudokugen.Board;

import java.util.UUID;

/**
 * Representation of a Sudoku board. This separates board generation from the REST API.
 */
public class GameBoard {
    private int size;
    private long randomSeed;
    private int progress;
    private boolean generated;
    private int[] board;
    private String id;

    public GameBoard() {
        size = 3;
        randomSeed = 0;
        board = new int[size * size];
        id = UUID.randomUUID().toString();
    }

    public GameBoard(Board b) {
        size = b.getSize();
        randomSeed = b.getRandomSeed();
        board = b.toIntArray();
        id = UUID.randomUUID().toString();
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isGenerated() {
        return generated;
    }

    public int[] getBoard() {
        return board.clone();
    }

    public String getId() {
        return id;
    }

    @JsonIgnore
    public void setBoard(int[] board) {
        this.board = board.clone();
    }

    // The following should not be accessible to the REST API, they should be read-only
    @JsonIgnore
    public void setProgress(int progress) {
        this.progress = progress;
    }

    @JsonIgnore
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    @JsonIgnore
    public GameBoard normalize() {
        if (!generated) {
            return null;
        }

        /*
         * create a number map we'll use to translate
         * Each index corresponds to a number in the existing board (minus 1, because zero based)
         * The number at that index is the number to be stored in the normalized board
         *
         * Example:
         * boards[0..8] = {1, 7, 3, 2, 6, 8, 9, 5, 4} (index)
         *                 1  2  3  4  5  6  7  8  9  (value)
         * numMap[0..8] = {1, 4, 3, 9, 8, 5, 2, 6, 7} (sorted)
         */
        int blockSize = size * size;
        int[] numMap = new int[blockSize];
        for (int ii = 0; ii < blockSize; ii++) {
            numMap[board[ii] - 1] = ii + 1;
        }

        GameBoard gb = new GameBoard();
        gb.setSize(size);
        gb.setRandomSeed(0L);

        int[] outBoard = new int[board.length];

        for (int ii = 0; ii < board.length; ii++) {
            outBoard[ii] = numMap[board[ii] - 1];
        }
        gb.setBoard(outBoard);
        gb.setProgress(100);
        gb.setGenerated(true);

        return gb;
    }
}
