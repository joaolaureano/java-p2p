package p2p.peer;

import p2p.network.UdpEndpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Reads commands from an input stream, executes them and prints the results.
 * Bad lines never kill the console: they are reported and the loop continues.
 */
public final class PeerConsole {

    private final BufferedReader in;
    private final PrintStream out;
    private final UdpEndpoint endpoint;
    private final PeerConfig config;
    private final SharedResource resource;

    public PeerConsole(BufferedReader in, PrintStream out, UdpEndpoint endpoint, PeerConfig config,
                       SharedResource resource) {
        this.in = in;
        this.out = out;
        this.endpoint = endpoint;
        this.config = config;
        this.resource = resource;
    }

    /** Runs until the user types {@code quit}/{@code exit} or the input ends (EOF). */
    public void run() {
        out.println(ConsoleCommand.help());

        while (true) {
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                out.println("Error: could not read input: " + e.getMessage());
                return;
            }
            if (line == null) {
                return;
            }

            try {
                ConsoleCommand command = ConsoleCommand.parse(line, config, resource);
                switch (command.kind()) {
                    case QUIT -> {
                        out.println("Bye.");
                        return;
                    }
                    case EMPTY -> { }
                    case HELP -> out.println(ConsoleCommand.help());
                    case INFO -> printInfo();
                    case SEND -> send(command);
                }
            } catch (RuntimeException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private void send(ConsoleCommand command) {
        InetAddress target;
        try {
            target = InetAddress.getByName(command.targetHost());
        } catch (UnknownHostException e) {
            out.println("Error: unknown host " + command.targetHost());
            return;
        }
        String payload = command.message().format();
        endpoint.send(payload, target, command.targetPort());
        out.println("-> " + command.targetHost() + ":" + command.targetPort() + ": " + payload);
    }

    private void printInfo() {
        out.println("Nickname: " + config.nickname());
        out.println("Port:     " + endpoint.localPort());
        out.println("Content:  " + resource.content());
        out.println("Hash:     " + resource.hash());
    }
}
