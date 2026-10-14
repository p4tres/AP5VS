package utb.fai;

import java.net.*;
import java.io.*;

public class EmailSender {

    private Socket socket;
    private OutputStream out;
    private BufferedReader in;

    /*
     * Constructor opens Socket to host/port. If the Socket throws an exception
     * during opening,
     * the exception is not handled in the constructor.
     */
    public EmailSender(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        out = socket.getOutputStream();
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        readResponse();
        sendCommand("HELO Localhost");
    }

    private void sendCommand(String cmd) throws IOException {
        out.write((cmd + "\r\n").getBytes());
        out.flush();
        readResponse();
    }

    private String readResponse() throws IOException {
        String line = in.readLine();
        System.out.println(line); // pre ladenie
        // viacriadková odpoveď má tvar "250-..." a posledný riadok "250 ..."
        while (line != null && line.length() > 3 && line.charAt(3) == '-') {
            line = in.readLine();
            System.out.println(line);
        }
        return line;
    }

    /*
     * Sends email from an email address to an email address with some subject and
     * text.
     * If the Socket throws an exception during sending, the exception is not
     * handled by this method.
     */
    public void send(String from, String to, String subject, String text) throws IOException {
        sendCommand("MAIL FROM:<" + from + ">");
        sendCommand("RCPT TO:<" + to + ">");
        sendCommand("DATA");

        StringBuilder msg = new StringBuilder();
        msg.append("From: ").append(from).append("\r\n");
        msg.append("To: ").append(to).append("\r\n");
        msg.append("Subject: ").append(subject).append("\r\n");
        msg.append("\r\n");
        msg.append(text.replace("\n", "\r\n")).append("\r\n");
        msg.append(".\r\n");

        out.write(msg.toString().getBytes());
        out.flush();
        readResponse();
    }

    /*
     * Sends QUIT and closes the socket
     */
    public void close() {
        try {
            sendCommand("QUIT");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
