package com.fincen.sar.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SarApplicationTest {

    @Test
    void contextLoads() {
        // Verify the Spring context loads successfully with all modules
    }
}
