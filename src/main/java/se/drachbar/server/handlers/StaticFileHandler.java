package se.drachbar.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {
    private static final String BASE_DIR = "public";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestedPath = exchange.getRequestURI().getPath();
        if (requestedPath.equals("/")) {
            requestedPath = "/index.html"; // Standardfil om ingen specifik fil anges
        }

        File file = new File(BASE_DIR + requestedPath);
        if (!file.exists() || file.isDirectory()) {
            send404(exchange);
            return;
        }

        // Sätt korrekt MIME-typ
        String mimeType = getMimeType(requestedPath);
        exchange.getResponseHeaders().set("Content-Type", mimeType);

        // Läs och skicka filen
        byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
        exchange.sendResponseHeaders(200, fileBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(fileBytes);
        os.close();
    }

    private void send404(HttpExchange exchange) throws IOException {
        String response = "404 - Not Found";
        exchange.sendResponseHeaders(404, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".js")) return "application/javascript";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream"; // Standard MIME-typ för okända filer
    }
}