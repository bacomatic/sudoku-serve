# sudoku-serve
## Sudoku board generator

This is a maven based project to build a microservice that uses Jersey/Jackson to generate and serve sudoku boards.

Once cloned you can open the project directly in Intellij Idea. You can also build and run this from the command line using mvn.
The project uses eclipse.orgs Jetty HTTP server, because it was easy to set up.

To build and run, just use mvn as you would most other projects:
```
mvn clean install
mvn exec:java
```

This server uses language features only available in Java 8 or later, notably lamba expressions and streams. It has not
been tested with Java 9 yet.

This project is set up to work on services like Heroku that use environment variables to configure the running service.
Jetty only requires the port, so I use PORT to configure the HTTP server. The MongoDB driver, in BoardRegistry, uses DATABASE_URL
to determine how to connect to the database, or it falls back on localhost:27017. In either case, it connects to a database
named "sudoku-db", which is hard coded (at the moment). Heroku defines PORT for you, but you must supply your own DATABASE_URL,
and at the moment only MongoDB is supported. I may extend that to support other databases in the future.

## Terminology
* **board** - A full Sudoku game board, composed of a size x size array of blocks. For example a traditional Sudoku board is 3
              blocks high by 3 blocks wide.
* **block** - A subdivision of the game board, each block contains a size x size array of cells.
* **cell** - A cell is the smallest unit on the Sudoku board. Each cell contains exactly one number.

## REST API (relative to whatever the deployed base URL is):
<table>
  <tr>
    <th>Req Type</th>
    <th>Path</th>
    <th>Function</th>
  </tr>

  <tr>
    <td>GET</td>
    <td>/sudoku</td>
    <td>Test endpoint, should return "Hello, Sudoku!". You can use this to test server availability.</td>
  </tr>

  <tr>
    <td>GET</td>
    <td>/sudoku/boards</td>
    <td>
        List boards that have been created. Note that not all boards may have been generated yet.
        This endpoint supports query parameters, see below for a description. The returned list only
        contains board ID, size and random seed. To get full board information you will need to request
        it by ID.
    </td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/boards/count</td>
    <td>
        Count of boards available. This endpoint supports query parameters, see below for a description.
    </td>
  </tr>
  <tr>
    <td>POST</td>
    <td>/sudoku/boards/new</td>
    <td>Create a new board using given parameters</td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/boards/{id}</td>
    <td>Get a specific board by id</td>
  </tr>
  <tr>
    <td>DELETE</td>
    <td>/sudoku/boards/{id}</td>
    <td>Delete a board, may not take effect immediately if the board is being generated</td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/boards/{id}/status</td>
    <td>Get just the status of a board, only the progress and generated fields.</td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/boards/{id}/normalized</td>
    <td>Get a normalized board, used for testing and pattern matching. A normalized board has all cells in the first box arranged in sequential order, so all normalized boards of the same size have the same first box.</td>
  </tr>

  <tr>
    <td>GET</td>
    <td>/sudoku/puzzles</td>
    <td>
        List puzzles that have been created. Note that not all puzzles may have been generated yet.
        This endpoint supports query parameters, see below for a description. Only returns puzzle ID, size
        and random seed. To get full puzzle data, you will need to request it by ID.
    </td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/puzzles/count</td>
    <td>
        Count of puzzles available. This endpoint supports query parameters, see below for a description.
    </td>
  </tr>
  <tr>
    <td>POST</td>
    <td>/sudoku/puzzles/new</td>
    <td>Create a new puzzle using given parameters</td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/puzzles/{id}</td>
    <td>
        Get a specific puzzle by id. You can pass "Demo-{size}" as ID to get a demo board.
        Demo boards are always the same and should really only be used for frontend development
    </td>
  </tr>
  <tr>
    <td>DELETE</td>
    <td>/sudoku/puzzles/{id}</td>
    <td>Delete a puzzle, may not take effect immediately if the puzzle is being generated</td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/puzzles/{id}/status</td>
    <td>Get just the status of a puzzle, only the progress and generated fields.</td>
  </tr>

</table>

All data, including boards, are returned to the client in JSON objects, for example:
```
{
    "board": [...],
    "boardId": "b9ade00f-7ab4-4dd4-96fa-f8a12a203afa",
    "randomSeed": 0,
    "size": 3
}
```

### Board Fields:
* **board** - Array of ints, in cell order starting at the top left and going horizontally to the bottom right
* **boardId** - UUID generated for this board, use this when requesting specific boards in the REST API
* **randomSeed** - (64 bit long integer) Random number generator seed used to create this board, passing zero will give you a random seed.
                   Passing the same non-zero seed and size should produce the same board each time, unless or until the underlying board
                   algorithm changes.
* **size** - The number of blocks per side and cells per block row/column. A traditional 3x3 Sudoku board has size "3". Only size 2 and 3
             boards are supported at the moment. Size 4 boards will be supported when the generator is fixed.

### Puzzle Fields:
* **board** - Array of ints, in cell order starting at the top left and going horizontally to the bottom right
* **puzzle** - Array of ints of value 1 or 0 (can be decoded as boolean) arranged in the same manner as the board, representing whether
               the associated cell is hidden or shown. A 1 value (true) means the value is given and the number should be visible in the
               puzzle.
* **puzzleId** - UUID generated for this board, use this when requesting specific boards in the REST API
* **difficulty** - An integer value giving the difficulty level of the puzzle generated. Defaults to 4. This is currently undefined (TBD).
* **randomSeed** - (64 bit long integer) Random number generator seed used to create this board, passing zero will give you a random seed.
                   Passing the same non-zero seed and size should produce the same board each time, unless or until the underlying board
                   algorithm changes.
* **size** - The number of blocks per side and cells per block row/column. A traditional 3x3 Sudoku board has size "3". Only size 2 and 3
             boards are supported at the moment. Size 4 boards will be supported when the generator is fixed.

The board and puzzle fields for boards and puzzle that have not finished being generated will be set to an empty list (e.g.: "board": []).
Use the status endpoints to determine how much of the board or puzzle has been generated.

### Query Parameters
* **size** - The size of the board or puzzle
* **randomSeed** - The random seed used to generate the board or puzzle
* **inProgress** - If "true" then only show boards which are being generated at the moment. Any other value will only show already
                   generated boards. Useful for filtering out items that are still in progress.
* **skip** - The number of items to skip in the results. Use for pagination. The count endpoint ignores this parameter.
* **limit** - The maximum number of items to return. Defaults to 50. Set to zero to have no limit
              (all items returned). This parameter is ignored in the count endpoint.

### TODO
- [ ] Add puzzle generation logic
- [X] Add puzzle generator endpoints
- [ ] Refactor DB code, put it all into one class
- [ ] Move generator defaults to QueryParams, or at least define them somewhere...
- [ ] Create generator executor pool to manage system load
- [X] GET /sudoku/{boards,puzzles}: Instead of passing a list of entire puzzles or boards, pass only a list of IDs back
