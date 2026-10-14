package utb.fai;

import java.io.*;
import java.net.*;

public class TelnetClient {

    private String serverIp;
    private int port;
    private volatile boolean running = true;

    public TelnetClient(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public void run() {
        try(Socket socket = new Socket(serverIp, port)) {

            Thread readThread = new Thread(() -> readFromServer(socket));
            Thread writeThread = new Thread(() -> writeToServer(socket));

            readThread.start();
            writeThread.start();

            try{
                readThread.join();
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            System.out.println("Reader thread has finished. Stopping writer thread.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readFromServer(Socket socket){
        try {
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];

            while (running){
                if (in.available() > 0) {
                    int n = in.read(buffer);
                    if (n == -1) {
                        running = false;
                        break;
                    }
                    System.out.write(buffer, 0, n);
                    System.out.flush();
                }else {
                    Thread.sleep(20);
                }
            }
        }catch(IOException | InterruptedException e){
            e.printStackTrace();
        }finally {
            running = false;
        }
    }

    public void writeToServer(Socket socket){
        try {
            OutputStream out = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

            while(running){
                if (System.in.available() > 0){
                    int n = System.in.read(buffer);
                    if (n == -1){
                        running = false;
                        break;
                    }

                    for (int i = 0; i < n; i++){
                        byte b = buffer[i];
                        if (b == '\n'){
                            String line = lineBuffer.toString();
                            if (line.equals("/QUIT")){
                                running = false;
                                break;
                            }
                            out.write(lineBuffer.toByteArray());
                            out.write('\n');
                            out.flush();
                            lineBuffer.reset();
                        }else if (b == '\r'){
                            // Ignore carriage return
                        }else {
                            lineBuffer.write(b);
                        }
                    }
                }else {
                    Thread.sleep(20);
                }
            }

        }catch(IOException | InterruptedException    e){
            e.printStackTrace();
        }finally {
            running = false;
        }
    }
}