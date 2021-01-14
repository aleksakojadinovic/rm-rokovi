package custom_protocols.finger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class FingerUrlConnection extends URLConnection {
    public static final int DEFAULT_PORT = 12345;
    private Socket connection = null;

    public FingerUrlConnection(URL url) {
        super(url);
    }

    // The user then asks for input stream and gets
    // the socket's input stream!
    @Override
    public InputStream getInputStream() throws IOException {
        if (!connected)
            connect();
        return connection.getInputStream();
    }

    @Override
    public void connect() throws IOException {
        if (connected)
            return;
        int port = url.getPort();
        if (port < 1 || port > 65535)
            port = DEFAULT_PORT;

        // (1) -- Parse the URL and send the data to the
        // socket!!
        connection = new Socket(url.getHost(), port);
        OutputStream out = connection.getOutputStream();
        String names = url.getFile();
        if (names != null && !names.equals("")){
            names = names.substring(1);
            names = URLDecoder.decode(names, US_ASCII);
            byte[] result = names.getBytes(US_ASCII);
            out.write(result);
        }
        out.write('\r');
        out.write('\n');
        out.flush();
        connected = true;
    }
}
