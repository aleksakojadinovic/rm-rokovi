package custom_protocols.finger;

import java.net.MalformedURLException;
import java.net.URL;

public class Main {

    public static void main(String[] args) throws MalformedURLException {
        URL url = new URL(null, "finger://localhost:12345/usernames", new FingerHandler());

    }
}
