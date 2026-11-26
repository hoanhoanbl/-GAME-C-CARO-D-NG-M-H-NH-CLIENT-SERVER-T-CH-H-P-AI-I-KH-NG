package caro.server;

public class ServerConfig {
    public final int port;

    public ServerConfig(int port) { this.port = port; }

    public static ServerConfig fromArgs(String[] args) {
        int port = caro.common.protocol.Protocol.TCP_PORT;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        return new ServerConfig(port);
    }
}
