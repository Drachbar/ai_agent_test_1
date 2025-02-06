package se.drachbar;

import se.drachbar.server.HttpServerInitializer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServerInitializer.startServer();
    }
}