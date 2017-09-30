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
    <td>Test endpoint, should return "Hello, Sudoku!" (will be removed in the future)</td>
  </tr>
  <tr>
    <td>GET</td>
    <td>/sudoku/boards</td>
    <td>List boards that have been created. Note that not all boards may have been generated yet.</td>
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
</table>

All data, including boards, are returned to the client in JSON objects, for example:
```
{
    "board": [...],
    "generated": false,
    "id": "b9ade00f-7ab4-4dd4-96fa-f8a12a203afa",
    "progress": 30,
    "randomSeed": 0,
    "size": 3
}
```

Fields:
* **board** - Array of ints, in cell order starting at the top left and going horizontally to the bottom right
* **generated** - Flag indicating whether the board has been fully generated or not
* **id** - UUID generated for this board, use this when requesting specific boards in the REST API
* **progress** - Shows the approximate percentage done while generating. This value can jump around wildly due to the nature of how
             the board generation algorithm works.
* **randomSeed** - (64 bit long integer) Random number generator seed used to create this board, passing zero will give you a random seed.
                   Passing the same seed and size should produce the same board each time, unless or until the underlying board algorithm
                   changes.
* **size** - The number of blocks per side and cells per block row/column. A traditional 3x3 Sudoku board has size "3". Only size 2 and 3
             boards are supported at the moment. Size 4 boards will be supported when the generator is fixed.
