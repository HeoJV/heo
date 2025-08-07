package heo.server;

import heo.server.Heo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HeoTest {
    private Heo app;

    @BeforeEach
    void setUp() {
        app = new Heo();
    }

    @Test
    void shouldInitializeRouter() {
        assertNotNull(app);
    }
}