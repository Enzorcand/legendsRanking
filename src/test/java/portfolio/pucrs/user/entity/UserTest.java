package portfolio.pucrs.user.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void initializesExpectedDefaultStatesAndTimestamps() {
        User user = new User();

        user.onCreate();

        assertTrue(user.isActive());
        assertFalse(user.isEmailVerified());
        assertFalse(user.isRankingEligible());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void preservesCreationTimestampAndRefreshesUpdateTimestamp() {
        User user = new User();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 30, 10, 0);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(LocalDateTime.MIN);

        user.onCreate();
        user.onUpdate();

        assertEquals(createdAt, user.getCreatedAt());
        assertNotEquals(LocalDateTime.MIN, user.getUpdatedAt());
    }
}
