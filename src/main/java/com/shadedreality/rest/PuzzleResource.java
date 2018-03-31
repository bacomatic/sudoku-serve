/*
 * Copyright (C) 2017, 2018, Shaded Reality, All Rights Reserved.
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

import com.shadedreality.data.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Puzzle generator endpoints
 *
 * GET    - /puzzles                - List pre-generated puzzles. Accepts query parameters (see below)
 * GET    - /puzzles/count          - Number of puzzles available, accepts same query params except skip and limit
 * POST   - /puzzles/new            - Create a new puzzle using given parameters
 * GET    - /puzzles/{id}           - Get a specific puzzle (even if not fully generated yet). Demo puzzles are
 *                                    available, set id to "demo-{size}" where size is the board size
 * DELETE - /puzzles/{id}           - Delete a puzzle, may not take effect immediately if the board or puzzle is being
 *                                    generated. Demo puzzles will not be deleted.
 * GET    - /puzzles/{id}/status    - Get just the status of a puzzle, only the progress and generated fields.
 */

@Path("puzzles")
public class PuzzleResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Puzzle> getPuzzleList(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        List<Puzzle> outList = new ArrayList<>();

        if (queryParams.isQueryGenerator()) {
            outList.addAll(PuzzleGenerator.query(queryParams));
        }
        if (queryParams.isQueryDatabase() && !queryParams.isLimitReached()) {
            outList.addAll(PuzzleRegistry.getRegistry().query(queryParams));
        }

        return outList;
    }

    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPuzzleCount(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        long count = 0;

        if (queryParams.isQueryGenerator()) {
            count += PuzzleGenerator.count(queryParams);
        }
        if (queryParams.isQueryDatabase()) {
            count += PuzzleRegistry.getRegistry().count(queryParams);
        }

        return Response.ok("{\"count\": "+count+"}").build();
    }

    @POST
    @Path("new")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPuzzle(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());

        System.out.println("Puzzle requested. Params: " + queryParams.toString());
        String id = PuzzleGenerator.generatePuzzle(queryParams);

        // We only need to replace "new" with the ID
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.replacePath("sudoku/puzzles/" + id);
        URI boardURI = builder.build();

        System.out.println("New puzzle with UUID " + id);
        System.out.println("Redirect URI: " + boardURI);

        return Response.seeOther(boardURI).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPuzzle(@PathParam("id") String id) {
        if (id.startsWith("Demo-")) {
            String demoSize = id.replace("Demo-", "").trim();
            int size = Integer.valueOf(demoSize);
            if (size == 2 || size == 3) {
                Puzzle pz = Puzzle.getDemoPuzzle(size);
                return Response.ok(pz).build();
            }
            throw new NotFoundException("Demo Puzzle with id " + id + " does not exist");
        }
        Puzzle pz = PuzzleRegistry.getRegistry().getPuzzle(id);
        if (pz == null) {
            pz = PuzzleGenerator.getPuzzle(id);
        }
        if (pz != null) {
            return Response.ok(pz).build();
        }
        throw new NotFoundException("Puzzle with id " + id + " does not exist");
    }

    @DELETE
    @Path("{id}")
    public Response deletePuzzle(@PathParam("id") String id) {
        if (id.startsWith("Demo-")) {
            throw new NotFoundException("Demo puzzles cannot be deleted");
        }
        if (PuzzleRegistry.getRegistry().removePuzzle(id)) {
            return Response.ok().build();
        }
        throw new NotFoundException("Puzzle with id " + id + " does not exist");
    }

    @GET
    @Path("{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPuzzleStatus(@PathParam("id") String id) {
        Integer pct = PuzzleGenerator.getPuzzleProgress(id);
        if (pct == null) {
            Puzzle pz = PuzzleRegistry.getRegistry().getPuzzle(id);
            if (pz != null) {
                pct = 100;
            } else {
                throw new NotFoundException("Puzzle with id " + id + " does not exist");
            }
        }
        return Response.ok("{\"progress\": " + pct + "}").build();
    }
}
