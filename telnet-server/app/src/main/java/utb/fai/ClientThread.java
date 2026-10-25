package utb.fai;

import java.io.*;
import java.net.*;

public class ClientThread extends Thread {

    private Socket clientSocket;

    public ClientThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        // Implementation of processing incoming communication from the telnet client
        try(Socket socket = clientSocket){
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int n;

            while ((n = input.read(buffer)) != -1){
                output.write(buffer, 0, n);
                output.flush();
            }
        } catch (IOException e){} 
        
    }
}
