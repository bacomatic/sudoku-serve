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

package com.shadedreality;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.util.Optional;

/**
 * Main class.
 *
 */
public class Main {
    // Use PORT and DATABASE_URL from the environment to set up our server
    public static final int PORT;

    static {
        Optional<String> envPort = Optional.ofNullable(System.getenv("PORT"));
        PORT = Integer.valueOf(envPort.orElse("8080"));
    }

    /**
     * Main method. Where the magic starts. It's sort of like walking through the gates at Disneyland.
     * @param args Things a lot of pirates say.
     * @throws IOException when it's bored.
     */
    public static void main(String[] args) throws IOException {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // Establish DB connection first

        // now start the server
        System.out.println("=== Initializing server on port " + PORT);

        Server jettyServer = new Server(PORT);
        jettyServer.setHandler(context);

        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/sudoku/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.shadedreality.rest");

        try {
            jettyServer.start();
            jettyServer.dumpStdErr();
            jettyServer.join();
        } catch (Exception e) {
            System.err.println("Uncaught Exception in server: "+e);
        }
        finally {
            jettyServer.destroy();
        }
    }
}
