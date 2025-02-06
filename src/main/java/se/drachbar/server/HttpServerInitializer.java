package se.drachbar.server;

import com.sun.net.httpserver.HttpServer;
import se.drachbar.server.handlers.ApiHandler;
import se.drachbar.server.handlers.ChatHandler;
import se.drachbar.server.handlers.StaticFileHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerInitializer {
    private static final int PORT = 8080;

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api", new ApiHandler());
        server.createContext("/api/chat", new ChatHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server startad på http://localhost:" + PORT);
    }
}
