import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

/**
 * Created by nathandunn on 4/27/17.
 */
public class TestConnection {

    public static void main(String args[]) {

        System.out.println("starting the client");
        CloseableHttpClient httpclient = HttpClients.createDefault(); // aka connection
        HttpGet httpget = new HttpGet("https://google.com");
        try {
            CloseableHttpResponse response = httpclient.execute(httpget);
            System.out.println("response: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("no error, yeah!");
    }

}
