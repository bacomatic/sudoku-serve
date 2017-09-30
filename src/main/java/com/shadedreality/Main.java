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

import com.shadedreality.rest.BoardResource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final Optional<String> host;
    public static final Optional<String> port;
    public static final String BASE_URI;

    static {
        host = Optional.ofNullable(System.getenv("HOST"));
        port = Optional.ofNullable(System.getenv("PORT"));
        BASE_URI = "http://"
                + host.orElse("localhost")
                + ":"
                + port.orElse("8080")
                + "/sudoku";
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        int iPort = Integer.valueOf(port.orElse("8080"));
        System.out.println("=== Initializing server using port " + iPort);

        Server jettyServer = new Server(iPort);
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
