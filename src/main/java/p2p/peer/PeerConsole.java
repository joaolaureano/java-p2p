package p2p.peer;

import p2p.dht.ResourceEntry;
import p2p.network.Message;
import p2p.network.UdpEndpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads commands from an input stream, executes them and prints the results.
 * Bad lines never kill the console: they are reported and the loop continues.
 */
public final class PeerConsole {

    private final BufferedReader in;
    private final PrintStream out;
    private final UdpEndpoint endpoint;
    private final PeerConfig config;
    private final SharedFiles files;
    private final FileDownloader downloader;
    private final InetAddress serverAddress;

    private final Map<String, ResourceEntry> lastList = new ConcurrentHashMap<>();

    public PeerConsole(BufferedReader in, PrintStream out, UdpEndpoint endpoint, PeerConfig config,
                       SharedFiles files, FileDownloader downloader, InetAddress serverAddress) {
        this.in = in;
        this.out = out;
        this.endpoint = endpoint;
        this.config = config;
        this.files = files;
        this.downloader = downloader;
        this.serverAddress = serverAddress;
    }

    /** Remembers a list entry so that {@code get <hash>} can resolve its owner later. */
    public void rememberListEntry(ResourceEntry entry) {
        if (entry != null) {
            lastList.put(entry.hash(), entry);
        }
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
                ConsoleCommand command = ConsoleCommand.parse(line, config);
                switch (command.kind()) {
                    case QUIT -> {
                        out.println("Bye.");
                        return;
                    }
                    case EMPTY -> { }
                    case HELP -> out.println(ConsoleCommand.help());
                    case INFO -> printInfo();
                    case SHARE -> share(command);
                    case FILES -> printFiles();
                    case REGISTER_ALL -> registerAll(command);
                    case GET -> get(command);
                    case SEND -> send(command);
                }
            } catch (RuntimeException | IOException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private void share(ConsoleCommand command) throws IOException {
        SharedFile file = files.addFile(Path.of(command.argument()));
        out.println("Sharing " + file.name() + " (" + file.size() + " bytes, " + file.hash() + ")");
        register(file, config.serverHost(), config.serverPort());
    }

    private void printFiles() {
        List<SharedFile> all = files.all();
        if (all.isEmpty()) {
            out.println("No shared files.");
            return;
        }
        for (SharedFile file : all) {
            out.println(file.hash() + "  " + file.size() + "  " + file.name());
        }
    }

    private void registerAll(ConsoleCommand command) {
        InetAddress target = resolve(command.targetHost());
        if (target == null) {
            return;
        }
        List<SharedFile> all = files.all();
        for (SharedFile file : all) {
            endpoint.send(ConsoleCommand.registerMessage(file), target, command.targetPort());
        }
        out.println("Registered " + all.size() + " file(s) at "
                + command.targetHost() + ":" + command.targetPort() + ".");
    }

    private void register(SharedFile file, String host, int port) {
        InetAddress target = resolve(host);
        if (target == null) {
            return;
        }
        Message message = ConsoleCommand.registerMessage(file);
        endpoint.send(message, target, port);
        out.println("-> " + host + ":" + port + ": " + message.header());
    }

    private void get(ConsoleCommand command) {
        String hash = command.argument();
        String host = command.targetHost();
        int port = command.targetPort();

        if (host == null) {
            ResourceEntry entry = lastList.get(hash);
            if (entry == null) {
                out.println("Unknown owner for " + hash
                        + ": run 'list' first or give <peer_host> <peer_port>");
                return;
            }
            host = entry.host();
            port = entry.port();
        }

        InetAddress target = resolve(host);
        if (target == null) {
            return;
        }

        String finalHost = host;
        int finalPort = port;
        InetAddress finalTarget = target;

        out.println("Downloading " + hash + " from " + finalHost + ":" + finalPort + "...");

        String name = "download-" + hash.substring(0, Math.min(8, hash.length()));
        Thread thread = new Thread(() -> {
            try {
                Path saved = downloader.download(hash, finalTarget, finalPort);
                long size = Files.size(saved);
                out.println("Saved " + saved + " (" + size + " bytes, md5 ok)");
            } catch (IOException | DownloadException e) {
                out.println("Download failed: " + e.getMessage());
            } catch (RuntimeException e) {
                out.println("Download failed: " + e.getMessage());
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
    }

    private void send(ConsoleCommand command) {
        InetAddress target = resolve(command.targetHost());
        if (target == null) {
            return;
        }
        Message message = command.message();
        endpoint.send(message, target, command.targetPort());
        out.println("-> " + command.targetHost() + ":" + command.targetPort() + ": " + message.header());
    }

    private void printInfo() {
        out.println("Nickname:     " + config.nickname());
        out.println("Port:         " + endpoint.localPort());
        out.println("Shared dir:   " + config.sharedDir().toAbsolutePath().normalize());
        out.println("Downloads:    " + config.downloadsDir().toAbsolutePath().normalize());
        out.println("Shared files: " + files.size());
    }

    private InetAddress resolve(String host) {
        if (host == null) {
            return serverAddress;
        }
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            out.println("Error: unknown host " + host);
            return null;
        }
    }
}
