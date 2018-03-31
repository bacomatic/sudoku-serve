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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * FIXME: Combine with BoardRegistry so we only have one place where we do DB access
 */
public class PuzzleRegistry {
    private static final String DATABASE_URL;
    private MongoCollection<Document> puzzleCollection;
    private ObjectMapper jsonMapper = new ObjectMapper();

    static {
        DATABASE_URL = Optional.ofNullable(System.getenv("DATABASE_URL"))
                .orElse("mongodb://localhost:27017/");
    }

    private static class PuzzleRegistryFactory {
        private static final PuzzleRegistry globalRegistry = new PuzzleRegistry();

        static PuzzleRegistry getGlobalRegistry() {
            return globalRegistry;
        }
    }

    private PuzzleRegistry() {
        MongoClientURI dbURI = new MongoClientURI(DATABASE_URL);
        MongoClient mongoClient = new MongoClient(dbURI);
        MongoDatabase database = mongoClient.getDatabase("sudoku-db");

        // POJO support in this MongoDB driver doesn't support arrays and is a pain to work with
        // Since we already have JSON marshalling via Jackson, we'll just use Document and manually marshall
        // JSON data in and out of the database.
        puzzleCollection = database.getCollection("puzzles");
    }

    public static PuzzleRegistry getRegistry() {
        return PuzzleRegistry.PuzzleRegistryFactory.getGlobalRegistry();
    }

    private Bson buildQueryFilters(QueryParams params) {
        if (params != null) {
            List<Bson> filters = new ArrayList<>();
            if (params.hasSize()) {
                filters.add(Filters.eq("size", params.getSize()));
            }

            if (params.hasRandomSeed()) {
                filters.add(Filters.eq("randomSeed", params.getRandomSeed()));
            }

            if (params.hasDifficulty()) {
                filters.add(Filters.eq("difficulty", params.getDifficulty()));
            }

            if (!filters.isEmpty()) {
                return Filters.and(filters);
            }
        }
        return null;
    }

    public List<Puzzle> query(QueryParams params) {
        ArrayList<Puzzle> outList = new ArrayList<>();
        int limit = 50;
        int skip = 0;

        if (params != null) {
            limit = params.getLimit();
            skip = params.getSkip();
        }

        FindIterable<Document> results;
        Bson filters = buildQueryFilters(params);
        if (filters == null) {
            results = puzzleCollection.find();
        } else {
            results = puzzleCollection.find(filters);
        }

        // apply sort and limits
        results = results.sort(new Document("_id", 1));
        if (skip > 0) {
            results = results.skip(skip);
        }
        if (limit > 0) {
            results = results.limit(limit);
        }
        results.forEach((Block<Document>) document -> {
            Puzzle pz = documentToPuzzle(document);
            if (pz != null) {
                outList.add(pz);
            }
        });
        return outList;
    }

    public long count(QueryParams params) {
        Bson filters = buildQueryFilters(params);
        if (filters != null) {
            return puzzleCollection.count(filters);
        }
        return puzzleCollection.count();
    }

    void registerPuzzle(Puzzle puzzle) {
        Document document = puzzleToDocument(puzzle);
        if (document != null) {
            puzzleCollection.insertOne(document);
        }
    }

    public Puzzle getPuzzle(String puzzleId) {
        Puzzle pz = null;
        Document document = puzzleCollection.find(Filters.eq("puzzleId", puzzleId)).first();
        if (document != null) {
            pz = documentToPuzzle(document);
        }
        return pz;
    }

    public boolean removePuzzle(String puzzleId) {
        Document document = puzzleCollection.findOneAndDelete(Filters.eq("puzzleId", puzzleId));
        return (document != null);
    }

    private Puzzle documentToPuzzle(Document document) {
        String json = document.toJson();
        Puzzle pz = null;
        try {
            pz = jsonMapper.readValue(json, Puzzle.class);
        } catch (IOException ioe) {
            System.err.println("IOE marshalling DB object: " + ioe);
            System.err.println("Source object: " + json);
        }
        return pz;
    }

    private Document puzzleToDocument(Puzzle pz) {
        Document document = null;
        try {
            document = Document.parse(jsonMapper.writeValueAsString(pz));
        } catch (JsonProcessingException e) {
            System.err.println("JSON parse exception adding board to DB: " + e);
            e.printStackTrace();
        }
        return document;
    }
}
