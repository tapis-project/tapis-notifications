package edu.utexas.tacc.tapis.notifications.frontend;
import okhttp3.*;
import okhttp3.WebSocket.Factory;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.testng.annotations.Test;


@Test
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestWebsocketAuthFilter {

    private static final Logger log = LoggerFactory.getLogger(TestWebsocketAuthFilter.class);

    @LocalServerPort
    private int port;



    public class TestListener extends WebSocketListener {

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);
            log.info("Opened socket");
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            log.info(text);
        }
    }

    @Test
    void testAuthFilter() throws Exception {
        String url = String.format("ws://localhost:%s", port);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url("ws://localhost:8080")
            .addHeader("x-tapis-token", "12323ewdfsdf43")
            .build();
        TestListener listener = new TestListener();
        WebSocket ws = client.newWebSocket(request, listener);
    }



}
