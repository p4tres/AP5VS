package utb.fai;

public class App {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port;

        try {
            port = args.length > 1 ? Integer.parseInt(args[1]) : 23;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number. Please provide a valid integer.");
            return;
        }
        if (port < 1 || port > 65535) {
            System.err.println("Port number must be between 1 and 65535.");
            return;
        }

        TelnetClient telnetClient = new TelnetClient(host, port);
        telnetClient.run();
    }
}
