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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shadedreality.sudokugen.Board;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * POJO representation of a Sudoku board. This separates board generation from the REST API. This class is used to
 * both store board data in the database and pass said data back to clients.
 *
 * We need to ignore _id from database objects. We don't use that id as we have our own.
 */
@JsonIgnoreProperties({"_id"})
public class GameBoard {
    private int size;
    private long randomSeed;
    private int[] board;
    private String boardId;

    public GameBoard() {
        size = 3;
        randomSeed = 0;
        board = new int[size * size];
        boardId = UUID.randomUUID().toString();
    }

    public GameBoard(GameBoard copy) {
        size = copy.getSize();
        randomSeed = copy.getRandomSeed();
        board = copy.getBoard(); // already a clone, no need to copy
        boardId = UUID.randomUUID().toString(); // copy gets unique id
    }

    public GameBoard(Board b) {
        size = b.getSize();
        randomSeed = b.getRandomSeed();
        board = b.toIntArray();
        boardId = UUID.randomUUID().toString();
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

    public int[] getBoard() {
        return board.clone();
    }

    public void setBoard(int[] board) {
        this.board = board.clone();
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    @JsonIgnore
    public GameBoard normalize() {
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

        GameBoard gb = new GameBoard(this);
        gb.setRandomSeed(0); // no longer valid

        for (int ii = 0; ii < board.length; ii++) {
            gb.board[ii] = numMap[board[ii] - 1];
        }

        return gb;
    }
}
