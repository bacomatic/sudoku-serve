/*
 * Copyright (C) 2017, Shaded Reality, All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/*
 * POJO representation of a full Sudoku game, including board and actual puzzle.
 */
@JsonIgnoreProperties("_id")
public class Puzzle {
    private int size;
    private long randomSeed;
    private int[] board;
    private int[] puzzle;
    private String puzzleId;

    public Puzzle() {
        size = 3;
        randomSeed = 0;
        board = new int[0]; // these can't be null or we'll cause exceptions
        puzzle = new int[0];
        puzzleId = UUID.randomUUID().toString();
    }

    public Puzzle(GameBoard board) {
        // GameBoard must be fully generated
        if (board == null) {
            throw new IllegalArgumentException("Invalid board for building puzzle");
        }

        size = board.getSize();
        randomSeed = board.getRandomSeed();
        this.board = board.getBoard();
        puzzle = new int[size * size * size * size];
        puzzleId = UUID.randomUUID().toString();
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

    public void setBoard(int[] board) {
        this.board = board.clone();
    }

    public int[] getBoard() {
        return board.clone();
    }

    public void setPuzzle(int[] puzzle) {
        this.puzzle = puzzle.clone();
    }

    public int[] getPuzzle() {
        return puzzle.clone();
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    public static Puzzle getDemoPuzzle(int size) {
        Puzzle p = new Puzzle();
        p.setSize(size);
        // puzzleId and randomSeed already set
        if (size == 2) {
            p.setBoard(new int[] {
                    1,3, 2,4,
                    4,2, 1,3,
                    3,1, 4,2,
                    2,4, 3,1
            });
            p.setPuzzle(new int[] {
                    1,0, 0,1,
                    0,1, 1,0,

                    1,0, 0,1,
                    1,0, 1,0
            });
        } else if (size == 3) {
            p.setBoard(new int[] {
                    7,8,2, 5,4,6, 9,1,3,
                    6,4,3, 9,1,8, 7,5,2,
                    1,5,9, 3,7,2, 6,4,8,

                    9,7,4, 6,2,1, 8,3,5,
                    5,1,6, 4,8,3, 2,7,9,
                    3,2,8, 7,9,5, 4,6,1,

                    8,6,7, 1,3,9, 5,2,4,
                    4,9,1, 2,5,7, 3,8,6,
                    2,3,5, 8,6,4, 1,9,7
            });
            p.setPuzzle(new int[] {
                    0,0,0, 0,0,1, 1,1,0,
                    0,0,0, 1,0,0, 0,0,1,
                    0,1,0, 0,0,0, 1,0,0,

                    0,0,1, 0,1,0, 0,0,1,
                    0,0,1, 0,0,1, 0,0,0,
                    0,0,1, 0,1,0, 1,0,0,

                    1,0,1, 0,1,0, 1,0,0,
                    1,0,1, 0,0,1, 0,1,0,
                    0,1,0, 1,0,0, 0,1,1
            });
        }
        return p;
    }
}
