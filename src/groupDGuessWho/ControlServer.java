package groupDGuessWho;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

/** Bounded localhost-only NDJSON server. Network I/O never runs on Swing's EDT. */
final class ControlServer implements AutoCloseable {
    interface Handler { Map<String,Object> handle(Map<String,Object> request); }
    private final ServerSocket listener;
    private final Handler handler;
    private final int vw, vh;
    private final ExecutorService workers=Executors.newFixedThreadPool(4, r -> {
        Thread t=new Thread(r,"guess-who-client"); t.setDaemon(true); return t;
    });
    private final Semaphore slots=new Semaphore(4);
    private final Set<Socket> clients=ConcurrentHashMap.newKeySet();
    private final Map<String,String> seen=new HashMap<>();
    private volatile boolean closed;

    ControlServer(int port,int vw,int vh,Handler handler) throws IOException {
        this.vw=vw; this.vh=vh; this.handler=handler;
        listener=new ServerSocket();
        try { listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),8); }
        catch(IOException ex) { listener.close(); throw ex; }
    }
    int port() { return listener.getLocalPort(); }
    void start() {
        Thread accept=new Thread(() -> {
            while (!closed) {
                try {
                    Socket socket=listener.accept();
                    if (!socket.getInetAddress().isLoopbackAddress() || !slots.tryAcquire()) { socket.close(); continue; }
                    clients.add(socket);
                    workers.execute(() -> serve(socket));
                } catch(IOException | RejectedExecutionException ex) {
                    if (!closed) System.err.println("CONTROL accept failed: "+ex.getMessage());
                }
            }
        },"guess-who-listener");
        accept.setDaemon(true); accept.start();
        System.out.println("CONTROL listening on 127.0.0.1:"+port()+" viewport="+vw+"x"+vh);
    }
    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        for (int b; (b=input.read()) != -1;) {
            if (b=='\n') {
                return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            }
            if (bytes.size() >= 8192) throw new IOException("Message exceeds 8192 bytes");
            bytes.write(b);
        }
        // An unterminated message is never executed.
        return null;
    }
    private void serve(Socket socket) {
        System.out.println("CONTROL connection established "+socket.getRemoteSocketAddress());
        try (socket) {
            socket.setSoTimeout(10000);
            InputStream in=socket.getInputStream();
            OutputStream out=socket.getOutputStream();
            String line;
            while ((line=readLine(in)) != null) {
                Map<String,Object> reply;
                try { reply=process(ControlJson.parse(line)); }
                catch (IllegalArgumentException ex) { reply=error(ex.getMessage()); }
                String json=ControlJson.encode(reply);
                System.out.println("CONTROL result "+json);
                out.write((json+"\n").getBytes(StandardCharsets.UTF_8)); out.flush();
            }
        } catch (IOException ex) {
            System.out.println("CONTROL client closed/error: "+ex.getMessage());
        } finally { clients.remove(socket); slots.release(); System.out.println("CONTROL disconnected"); }
    }
    private static Map<String,Object> error(String message) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("received",true); result.put("executed",false);
        result.put("status","rejected"); result.put("error",message); return result;
    }
    // Serializes IDs and actions across connections; waits only on background workers.
    private synchronized Map<String,Object> process(Map<String,Object> request) {
        if (closed) return error("Server is closing");
        if (!Set.of("type","x","y","id").containsAll(request.keySet()))
            throw new IllegalArgumentException("Unknown request field");
        Object type=request.get("type");
        if (!"SELECT".equals(type) && !"INFO".equals(type)) throw new IllegalArgumentException("type must be SELECT or INFO");
        String id=null;
        if (request.containsKey("id")) {
            if (!(request.get("id") instanceof String text) || !text.matches("[A-Za-z0-9_.:-]{1,128}"))
                throw new IllegalArgumentException("id must be 1-128 ASCII letters, digits, _, ., :, or -");
            id=(String)request.get("id");
        }
        if ("SELECT".equals(type)) {
            if (!(request.get("x") instanceof Double x) || !(request.get("y") instanceof Double y)
                    || x < 0 || y < 0 || x > vw-1 || y > vh-1)
                throw new IllegalArgumentException("SELECT requires numeric x/y inside configured viewport");
        } else if (request.containsKey("x") || request.containsKey("y")) {
            throw new IllegalArgumentException("INFO does not take coordinates");
        }
        System.out.println("CONTROL received "+ControlJson.encode(request));
        if ("SELECT".equals(type) && id != null) {
            if (seen.containsKey(id)) {
                Map<String,Object> duplicate=error("Event ID already processed; no action repeated");
                duplicate.put("id",id); duplicate.put("status","duplicate");
                duplicate.put("original_status",seen.get(id)); return duplicate;
            }
            if (seen.size() >= 10000) return error("ID capacity reached; restart server before more unique IDs");
            seen.put(id,"reserved");
        }
        FutureTask<Map<String,Object>> action=new FutureTask<>(() -> {
            if (closed) return error("Server closed before execution");
            return handler.handle(request);
        });
        SwingUtilities.invokeLater(action);
        Map<String,Object> result;
        try { result=new LinkedHashMap<>(action.get()); }
        catch (Exception ex) { result=error("Action failed: "+ex.getClass().getSimpleName()); }
        result.put("received",true);
        if (id != null) {
            result.put("id",id);
            if ("SELECT".equals(type)) seen.put(id,String.valueOf(result.get("status")));
        }
        return result;
    }
    @Override public void close() {
        closed=true;
        try { listener.close(); } catch(IOException ignored) { }
        for(Socket socket:clients) try { socket.close(); } catch(IOException ignored) { }
        workers.shutdownNow();
    }
}
