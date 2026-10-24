
package banking;

import banking.handler.BankingApiHandler;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class BankingApiServer {

    public static void main(String[] args) {

        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(8080), 0);

            server.createContext("/", new BankingApiHandler());

            server.setExecutor(null);

            server.start();

            System.out.println("Banking REST API started successfully!");
            System.out.println("Server URL: http://localhost:8080");
            System.out.println("Press Ctrl+C to stop the server.");

        } catch (IOException e) {
            System.out.println("Could not start the REST API server.");
            e.printStackTrace();
        }
    }
}
