package typingarena.net;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** TCP 한 줄(JSON) 프로토콜 클라이언트 */
public class NetClient implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread reader;
    private final Gson gson = new Gson();
    private Consumer<Message> onMessage = m -> {};

    public NetClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnMessage(Consumer<Message> c) { this.onMessage = c; }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        reader = new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    try {
                        Message msg = gson.fromJson(line, Message.class);
                        onMessage.accept(msg);
                    } catch (JsonSyntaxException e) {
                        // ignore malformed lines
                    }
                }
            } catch (IOException ignored) {
            }
        }, "NetClient-Reader");
        reader.setDaemon(true);
        reader.start();
    }

    public synchronized void send(Message msg) {
        try {
            String json = gson.toJson(msg);
            out.write(json);
            out.write("\n");
            out.flush();
        } catch (IOException e) {
            // ignore (UI 쪽에서 연결 상태로 처리)
        }
    }

    @Override public void close() throws IOException {
        try { if (socket != null) socket.close(); } finally { socket = null; }
    }
}
