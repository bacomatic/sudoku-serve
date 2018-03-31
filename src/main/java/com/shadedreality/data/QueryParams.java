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

import javax.ws.rs.core.MultivaluedMap;

/**
 * Wrapper object for managing query parameters.
 */
public class QueryParams {
    private Integer size = null;
    private Long randomSeed = null;
    private Integer difficulty = null;
    private int skip = 0;
    private boolean limitReached = false;
    private int limit = 50;

    private boolean queryDatabase = true;
    private boolean queryGenerator = true;

    public QueryParams(MultivaluedMap<String,String> uriParams) {
        if (uriParams.containsKey("size")) {
            size = Integer.valueOf(uriParams.getFirst("size"));
        }

        if (uriParams.containsKey("randomSeed")) {
            randomSeed = Long.valueOf(uriParams.getFirst("randomSeed"));
        }

        if (uriParams.containsKey("difficulty")) {
            difficulty = Integer.valueOf(uriParams.getFirst("difficulty"));
        }

        if (uriParams.containsKey("inProgress")) {
            queryGenerator = Boolean.valueOf(uriParams.getFirst("inProgress"));
            queryDatabase = !queryGenerator;
        }

        if (uriParams.containsKey("skip")) {
            skip = Integer.valueOf(uriParams.getFirst("skip"));
        }

        if (uriParams.containsKey("limit")) {
            limit = Integer.valueOf(uriParams.getFirst("limit"));
        }
    }

    public boolean hasSize() {
        return size != null;
    }

    public Integer getSize() {
        return size;
    }

    public boolean hasRandomSeed() {
        return randomSeed != null;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public boolean hasDifficulty() {
        return difficulty != null;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public boolean isQueryDatabase() {
        return queryDatabase;
    }

    public boolean isQueryGenerator() {
        return queryGenerator;
    }

    public int getSkip() {
        return skip;
    }

    public int getLimit() {
        return limit;
    }

    /**
     * Check if we need to skip the next board. This decrements the skip counter each time it's called, so only
     * call this when absolutely necessary.
     * @return true if it should be skipped.
     */
    public boolean checkSkip() {
        if (limitReached) {
            return true;
        }
        if (skip > 0) {
            skip--;
            return true;
        }
        return false;
    }

    /**
     * Check if we're at a limit. Decrements the limit counter, if it's above zero. If limit is already at zero, will
     * always return false.
     * @return false if we're below the limit or there is no limit, true otherwise
     */
    public boolean checkLimit() {
        if (limit > 0) {
            if (limit == 1) {
                // stop adding/counting boards
                // Don't allow this to fall below 1, so it's a hard stop
                // this is fine if a limit of 1 is specified, since this is called after adding/counting a board
                limitReached = true;
                return true;
            } else {
                limit--;
            }
        }
        return false;
    }

    public boolean isLimitReached() {
        return limitReached;
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder("QueryParameters {\n");
        if (hasSize()) {
            sb.append("    size: ");
            sb.append(getSize().toString());
            sb.append("\n");
        }
        if (hasRandomSeed()) {
            sb.append("    randomSeed: ");
            sb.append(getRandomSeed().toString());
            sb.append("\n");
        }
        if (hasDifficulty()) {
            sb.append("    difficulty: ");
            sb.append(getDifficulty().toString());
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
