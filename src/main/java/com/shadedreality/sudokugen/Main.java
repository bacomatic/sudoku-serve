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

/**
 *
 * @author ddehaven
 */
public class Main {
    static final boolean DEBUG = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Board b = new Board();
        
        // fill with test data
        if (DEBUG) {
            Cell[] cells = b.getCells();
            int index = 0;
            for (Cell c : cells) {
                c.forceValue(index++);
            }
        }
        
        
        // print each row, col, block to stdout
        System.out.println("Rows:");
        b.forEachRow(row -> {
            row.print();
            System.out.println();
        });
        
        System.out.println("Columns:");
        b.forEachColumn(col -> {
            col.print();
            System.out.println();
        });

        System.out.println("Blocks:");
        b.forEachBlock(block -> {
            block.print();
            System.out.println();
        });
        
        // dump the board to stdout
        System.out.println("Board:");
        b.print();
        
        // reset and print again
        System.out.println("\nReset board:");
        b.reset();
        b.print();
        
        // grind through all hundreds of thousands of combinations to find
        // the best block order to use
        System.out.println("\nCell gen test:");
        Generator g = new Generator(b);
        g.generate();
        b.print();
    }
}
