package custom_protocols.finger;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class FingerHandler extends URLStreamHandler {


    @Override
    protected int getDefaultPort() {
        return FingerUrlConnection.DEFAULT_PORT;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new FingerUrlConnection(u);
    }
}
