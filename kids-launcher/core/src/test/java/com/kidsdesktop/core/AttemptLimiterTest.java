package com.kidsdesktop.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AttemptLimiterTest {

    @Test
    public void fifthFailureLocksForOneMinute() {
        AttemptLimiter.State state = new AttemptLimiter.State(0, 0L);
        long now = 1_000L;
        for (int i = 0; i < 4; i++) {
            state = AttemptLimiter.onFailure(state, now);
            assertFalse(AttemptLimiter.isLocked(state, now));
        }
        state = AttemptLimiter.onFailure(state, now);
        assertTrue(AttemptLimiter.isLocked(state, now));
        assertEquals(now + AttemptLimiter.LOCK_MS, state.lockUntilMillis);
        assertEquals(0, state.failures);
    }

    @Test
    public void failureDuringLockDoesNotExtendIt() {
        AttemptLimiter.State locked = new AttemptLimiter.State(0, 5_000L);
        AttemptLimiter.State again = AttemptLimiter.onFailure(locked, 4_000L);
        assertEquals(5_000L, again.lockUntilMillis);
        assertFalse(AttemptLimiter.isLocked(again, 5_000L));
        AttemptLimiter.State after = AttemptLimiter.onFailure(again, 5_000L);
        assertEquals(1, after.failures);
    }

    @Test
    public void successClearsFailures() {
        AttemptLimiter.State state = AttemptLimiter.onSuccess();
        assertEquals(0, state.failures);
        assertEquals(0L, state.lockUntilMillis);
        assertFalse(AttemptLimiter.isLocked(AttemptLimiter.onFailure(null, 10L), 10L));
    }
}
