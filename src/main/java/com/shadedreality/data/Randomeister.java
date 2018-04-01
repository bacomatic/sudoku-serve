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

import java.util.Random;

/**
 * Simple class to handle Random stuff. Mostly to avoid duplicate code.
 */
public class Randomeister {
    /**
     * @return a random long.
     */
    public static long randomLong() {
        return getMeister().getRandomLong();
    }

    /**
     * @return a Random with random seed.
     */
    public static Random randomRandom() {
        return new Random(randomLong());
    }

    private static class MeisterFactory {
        private static final Randomeister globalInstance = new Randomeister();
        private static Randomeister getMeister() {
            return globalInstance;
        }
    }

    private static Randomeister getMeister() {
        return MeisterFactory.getMeister();
    }

    private Random meister;
    private Randomeister() {
        meister = new Random(System.currentTimeMillis());
    }

    private long getRandomLong() {
        return meister.nextLong();
    }
}
