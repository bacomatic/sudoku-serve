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

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Container to hold generated boards.
 *
 * TODO: Back with JDBC connection for persistence.
 */
public final class BoardRegistry {
    private final Map<String,GameBoard> localCache = Collections.synchronizedMap(new HashMap<>());

    private static class BoardRegistryFactory {
        private static final BoardRegistry globalRegistry = new BoardRegistry();

        public static BoardRegistry getGlobalRegistry() {
            return globalRegistry;
        }
    }

    private BoardRegistry() {}

    public static BoardRegistry getRegistry() {
        return BoardRegistryFactory.getGlobalRegistry();
    }

    public List<GameBoard> getBoardList() {
        ArrayList<GameBoard> outList = new ArrayList<>();
        outList.addAll(localCache.values());
        return outList;
    }

    public void forEachBoard(Consumer<GameBoard> r) {
        localCache.values().forEach(r);
    }

    /**
     * Stream interface, allowing you to filter boards, for example by size or whether the board is completely generated
     * or not.
     * @return stream of all currently registered boards.
     */
    public Stream<GameBoard> boardStream() {
        return localCache.values().stream();
    }

    public void registerBoard(GameBoard board) {
        localCache.put(board.getId(), board);
    }

    public GameBoard getBoard(String id) {
        return localCache.get(id);
    }

    public boolean removeBoard(String id) {
        if (localCache.get(id) != null) {
            localCache.remove(id);
            return true;
        }
        return false;
    }
}
