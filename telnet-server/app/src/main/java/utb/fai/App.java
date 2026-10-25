package utb.fai;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args){
        int port = 12345;
        int max_threads = 10;    

        // Implement input parameter processing
        try{
            if (args.length > 0) port = Integer.parseInt(args[0]);
            if (args.length > 1) max_threads = Integer.parseInt(args[1]);
        }catch (NumberFormatException e){
            System.err.println("Port and max threads must be integers.");
            return;
        }
        if (port < 1 || port > 65535){
            System.err.println("Port must be between 1 and 65535.");
            return;
        }
        if (max_threads < 1){
            System.err.println("Max threads must be a positive integer.");
            return;
        }

        // Implementation of the main server loop
        ExecutorService pool = new ThreadPoolExecutor(
            max_threads, 
            max_threads, 
            0L, TimeUnit.MILLISECONDS, 
            new LinkedBlockingQueue<>());
        try(ServerSocket serverSocket = new ServerSocket(port)){
            while (true){
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientThread(clientSocket));
            }
        }catch (IOException e){
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            pool.shutdown();
        }   
    }
}
