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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * POJO class used to send board or puzzle lists over the wire.
 */
public class ListInfo {
    public String id; // board or puzzle ID
    public int size;  // size of board or puzzle
    public long randomSeed; // random seed used to generate the board or puzzle

    @JsonIgnore
    public ListInfo(Puzzle puzzle) {
        id = puzzle.getPuzzleId();
        size = puzzle.getSize();
        randomSeed = puzzle.getRandomSeed();
    }

    @JsonIgnore
    public ListInfo(GameBoard board) {
        id = board.getBoardId();
        size = board.getSize();
        randomSeed = board.getRandomSeed();
    }

    ListInfo() {
        id = null;
        size = 0;
        randomSeed = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long seed) {
        randomSeed = seed;
    }
}

