package de.dfg.oc.otc.layer2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Die Klasse stellt Methoden zur Verfügung, um über eine Socketverbindung zu
 * kommunizieren. Die Klasse ist als Singleton realisert.
 *
 * @author hpr
 */
public final class SocketConnection {
    /**
     * IP-Adresse des zu kontaktierenden Hosts.
     */
    private static final String HOST = "127.0.0.1";
    /**
     * Basis-Port für die Verbindung.
     */
    private static final int PORT = 1234;
    /**
     * Pool of connections.
     */
    private static final Map<Integer, SocketConnection> socketConnections = new ConcurrentHashMap<>();

    /**
     * Gibt die Singleton-Instanz dieser Klasse zur�ck, die auf Port
     * {@code PORT + _portOffset} kommuniziert.
     *
     * @param portOffset Offset, der auf den Basisport addiert wird, um den
     *                   Kommunikationsport zu erhalten
     * @return die Singleton-Instanz dieser Klasse
     */
    public static SocketConnection getInstance(final int portOffset) throws IOException {
        SocketConnection instance = socketConnections.get(portOffset);
        if (instance == null) {
            instance = new SocketConnection(portOffset);
            socketConnections.put(portOffset, instance);
        }
        return instance;
    }

    /**
     * Reader for data from Python/AIMSUN.
     */
    private BufferedReader in;
    /**
     * Output of data to Python/AIMSUN.
     */
    private PrintWriter out;
    /**
     * Offset, der auf den Basisport addiert wird, um den Kommunikationsport zu
     * erhalten.
     */
    private int portOffset;
    /**
     * Socket for communication with Python/AIMSUN.
     */
    private Socket socket;

    /**
     * Erzeugt eine neue Verbindung.
     *
     * @param portOffset Offset, der auf den Basisport addiert wird, um den
     *                   Kommunikationsport zu erhalten
     */
    private SocketConnection(final int portOffset) throws IOException {
        // Create communication socket
        final int port = PORT + portOffset;
        this.socket = new Socket(HOST, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.portOffset = portOffset;
    }

    /**
     * Close connection.
     */
    public void close() throws IOException {
        socket.close();
        socketConnections.put(portOffset, null);
    }

    /**
     * Receives a string from AIMSUN.
     *
     * @return received message
     */
    public String recv() throws IOException {
        return in.readLine();
    }

    /**
     * Sendet einen String.
     *
     * @param message der zu sendende String
     */
    public void send(final String message) {
        out.print(message);
        out.flush();
    }
}
