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

package com.shadedreality;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;

/**
 * Main class.
 *
 */
public class Main {
    // Use PORT and DATABASE_URL from the environment to set up our server
    static final int PORT;
    static final boolean LOG_REQUESTS;

    static {
        Optional<String> envPort = Optional.ofNullable(System.getenv("PORT"));
        PORT = Integer.valueOf(envPort.orElse("8080"));

        Optional<String> logRequests = Optional.ofNullable(System.getenv("LOG_REQUESTS"));
        LOG_REQUESTS = Boolean.valueOf(logRequests.orElse("false"));
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

        if (LOG_REQUESTS) {
            RequestLog requestLog = new RipLogger();
            jettyServer.setRequestLog(requestLog);
        }

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

    public static class RipLogger implements RequestLog {
        public void log(Request request, Response response) {
            System.out.println(request.getMethod() + "  " + request.getRequestURI() + " --> " + response.getStatus());
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames.hasMoreElements()) {
                System.out.println("Request headers:");
            }
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                Enumeration<String> headers = request.getHeaders(name);
                while (headers.hasMoreElements()) {
                    System.out.println("    " + name + ": " + headers.nextElement());
                }
            }
        }
    }
}
