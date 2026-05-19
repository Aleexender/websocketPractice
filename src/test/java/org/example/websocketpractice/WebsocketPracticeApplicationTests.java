package org.example.websocketpractice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "kis.app-key=test-key",
        "kis.app-secret=test-secret"
})
class WebsocketPracticeApplicationTests {

    @Test
    void contextLoads() {
    }
}
