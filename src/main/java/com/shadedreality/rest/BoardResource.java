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

package com.shadedreality.rest;

import com.shadedreality.data.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Board generator endpoints
 *
 * GET    - /boards                 - List pre-generated boards. Accepts query parameters (see below). Only returns
 *                                    board ID, size and random seed.
 * GET    - /boards/count           - Number of boards, accepts same query params except skip and limit
 * POST   - /boards/new             - Create a new board using given parameters
 * GET    - /boards/{id}            - Get a specific board (even if not fully generated yet)
 * DELETE - /boards/{id}            - Delete a board, may not take effect immediately if the board is being generated
 * GET    - /boards/{id}/status     - Get just the status of a board, only the progress and generated fields.
 * GET    - /boards/{id}/normalized - Get a normalized board, for pattern matching. A normalized board has all cells in
 *                                    the first box arranged in sequential order, so all normalized boards of the same
 *                                    size have the same first box.
 */
@Path("/boards")
public class BoardResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ListInfo> getBoards(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        List<ListInfo> outList = new ArrayList<>();

        if (queryParams.isQueryGenerator()) {
            BoardGenerator.query(queryParams).forEach(board -> outList.add(new ListInfo(board)));
        }
        if (queryParams.isQueryDatabase() && !queryParams.isLimitReached()) {
            BoardRegistry.getRegistry().query(queryParams).forEach(board -> outList.add(new ListInfo(board)));
        }

        return outList;
    }

    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoardCount(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        long count = 0;

        if (queryParams.isQueryGenerator()) {
            count += BoardGenerator.count(queryParams);
        }
        if (queryParams.isQueryDatabase()) {
            count += BoardRegistry.getRegistry().count(queryParams);
        }

        return Response.ok("{\"count\": "+count+"}").build();
    }

    @POST
    @Path("new")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBoard(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());

        System.out.println("Board requested. Params: " + queryParams.toString());
        String id = BoardGenerator.generateBoard(queryParams, null);

        // We only need to replace "new" with the ID
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.replacePath("sudoku/boards/" + id);
        URI boardURI = builder.build();

        System.out.println("New board with UUID " + id);
        System.out.println("Redirect URI: " + boardURI);

        return Response.seeOther(boardURI).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoard(@PathParam("id") String id) {
        GameBoard gb = BoardGenerator.getBoard(id);
        if (gb == null) {
            gb = BoardRegistry.getRegistry().getBoard(id);
        }
        if (gb != null) {
            return Response.ok(gb).build();
        }
        throw new NotFoundException("Game board with id " + id + " does not exist");
    }

    @DELETE
    @Path("{id}")
    public Response deleteBoard(@PathParam("id") String id) {
        if (BoardRegistry.getRegistry().removeBoard(id)) {
            return Response.ok().build();
        }
        throw new NotFoundException("Game board with id " + id + " does not exist");
    }

    @GET
    @Path("{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoardStatus(@PathParam("id") String id) {
        Integer pct = BoardGenerator.getBoardProgress(id);
        if (pct == null) {
            GameBoard gb = BoardRegistry.getRegistry().getBoard(id);
            if (gb != null) {
                pct = 100;
            } else {
                throw new NotFoundException("Game board with id " + id + " does not exist");
            }
        }
        return Response.ok("{\"progress\": " + pct + "}").build();
    }

    @GET
    @Path("{id}/normalized")
    @Produces(MediaType.APPLICATION_JSON)
    public GameBoard getNormalizedBoard(@PathParam("id") String id) {
        GameBoard gb = BoardRegistry.getRegistry().getBoard(id);
        if (gb == null) {
            throw new NotFoundException("Game board with id " + id + " does not exist or is being generated still");
        }
        gb = gb.normalize();
        if (gb == null) {
            throw new NotSupportedException("Game board is not finished being generated");
        }
        return gb;
    }
}
