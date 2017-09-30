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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Database access for persistent board storage.
 */
public final class BoardRegistry {
    private static final String DATABASE_URL;
    private MongoCollection<Document> boardCollection = null;
    private ObjectMapper jsonMapper = new ObjectMapper();

    static {
        DATABASE_URL = Optional.ofNullable(System.getenv("DATABASE_URL"))
                .orElse("mongodb://localhost:27017/");
    }

    private static class BoardRegistryFactory {
        private static final BoardRegistry globalRegistry = new BoardRegistry();

        static BoardRegistry getGlobalRegistry() {
            return globalRegistry;
        }
    }

    private BoardRegistry() {
        MongoClientURI dbURI = new MongoClientURI(DATABASE_URL);
        MongoClient mongoClient = new MongoClient(dbURI);
        MongoDatabase database = mongoClient.getDatabase("sudoku-db");

        // POJO support in this MongoDB driver doesn't support arrays and is a pain to work with
        // Since we already have JSON marshalling via Jackson, we'll just use Document and manually marshall
        // JSON data in and out of the database.
        boardCollection = database.getCollection("boards");
    }

    public static BoardRegistry getRegistry() {
        return BoardRegistryFactory.getGlobalRegistry();
    }

    public List<GameBoard> getBoardList() {
        ArrayList<GameBoard> outList = new ArrayList<>();
        boardCollection.find().forEach((Block<Document>) document -> {
            GameBoard gb = documentToGameBoard(document);
            if (gb != null) {
                outList.add(gb);
            }
        });
        return outList;
    }

    void registerBoard(GameBoard board) {
        Document document = gameBoardToDocument(board);
        if (document != null) {
            boardCollection.insertOne(document);
        }
    }

    public GameBoard getBoard(String boardId) {
        GameBoard gb = null;
        Document document = boardCollection.find(Filters.eq("boardId", boardId)).first();
        if (document != null) {
            gb = documentToGameBoard(document);
        }
        return gb;
    }

    public boolean removeBoard(String boardId) {
        Document document = boardCollection.findOneAndDelete(Filters.eq("boardId", boardId));
        return (document != null);
    }

    private GameBoard documentToGameBoard(Document document) {
        String json = document.toJson();
        GameBoard gb = null;
        try {
            gb = jsonMapper.readValue(json, GameBoard.class);
        } catch (IOException ioe) {
            System.err.println("IOE marshalling DB object: " + ioe);
            System.err.println("Source object: " + json);
        }
        return gb;
    }

    private Document gameBoardToDocument(GameBoard gb) {
        Document document = null;
        try {
            document = Document.parse(jsonMapper.writeValueAsString(gb));
        } catch (JsonProcessingException e) {
            System.err.println("JSON parse exception adding board to DB: " + e);
            e.printStackTrace();
        }
        return document;
    }
}
