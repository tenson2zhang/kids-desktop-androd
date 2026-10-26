package com.kidsdesktop.core;

/** 连续输错保护密码后短暂锁定，避免孩子在旁边把密码试出来。 */
public final class AttemptLimiter {

    public static final int MAX_FAILURES = 5;
    public static final long LOCK_MS = 60_000L;

    private AttemptLimiter() {
    }

    public static final class State {
        public final int failures;
        public final long lockUntilMillis;

        public State(int failures, long lockUntilMillis) {
            this.failures = failures;
            this.lockUntilMillis = lockUntilMillis;
        }
    }

    public static boolean isLocked(State state, long nowMillis) {
        return state != null && state.lockUntilMillis > nowMillis;
    }

    public static State onFailure(State state, long nowMillis) {
        State current = state == null ? new State(0, 0L) : state;
        if (isLocked(current, nowMillis)) {
            return current;
        }
        int failures = current.failures + 1;
        if (failures >= MAX_FAILURES) {
            return new State(0, nowMillis + LOCK_MS);
        }
        return new State(failures, 0L);
    }

    public static State onSuccess() {
        return new State(0, 0L);
    }
}
