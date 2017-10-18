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

package com.shadedreality.rest;

import com.shadedreality.data.Puzzle;
import com.shadedreality.data.QueryParams;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("puzzles")
public class PuzzleResource {
    /**
     * Temporary demo puzzle endpoint. This will be replaced when the puzzle generator is finished.
     * @param size size of puzzle to generate
     * @return Puzzle of the requested size
     */
    @GET
    @Path("demo")
    @Produces(MediaType.APPLICATION_JSON)
    public Puzzle getPuzzle(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        int size = queryParams.hasSize() ? queryParams.getSize() : 3;
        if (size == 0) {
            // choose random supported size
            size = (int)(Math.random() * 2 + 2);
        }
        return Puzzle.getDemoPuzzle(size);
    }
}
