package dev.pota.weaponparry.core;

/** Server-tick-based state. No persistent character data and no per-tick polling needed. */
public final class ParryTimeline {
    private long guardEnd;
    private long cooldownEnd;
    private long counterEnd;
    private long penaltyEnd;
    private boolean guardOpen;
    private boolean guardSucceeded;
    private boolean penaltyPending;
    private boolean freeSwingPending;

    public synchronized boolean activate(long tick, int window, int cooldown) {
        if (guardActive(tick) || tick < cooldownEnd) return false;
        guardOpen = true;
        guardSucceeded = false;
        guardEnd = tick + window;
        cooldownEnd = tick + cooldown;
        penaltyPending = false;
        penaltyEnd = 0;
        return true;
    }

    public synchronized boolean guardActive(long tick) {
        expire(tick);
        return guardOpen;
    }

    public synchronized boolean parryHit(long tick, int counterDuration) {
        if (!guardActive(tick)) return false;
        guardSucceeded = true;
        cooldownEnd = tick; // Immediate reset, even while the current guard remains open.
        counterEnd = tick + counterDuration; // Refreshes, never stacks.
        freeSwingPending = true;
        return true;
    }

    /** A strike from outside the frontal cone breaks a failed guard and is punishable. */
    public synchronized void missedGuard(long tick) {
        if (guardActive(tick) && !guardSucceeded) {
            guardOpen = false;
            penaltyPending = true;
            penaltyEnd = cooldownEnd;
        }
    }

    public synchronized boolean penaltyReady(long tick) {
        expire(tick);
        return penaltyPending && tick < penaltyEnd;
    }

    public synchronized void consumePenalty() { penaltyPending = false; }

    public synchronized boolean counterReady(long tick) {
        expire(tick);
        return counterEnd > tick;
    }

    public synchronized void consumeCounter() {
        counterEnd = 0;
        freeSwingPending = false;
    }

    public synchronized boolean freeSwingReady(long tick) {
        expire(tick);
        return counterEnd > tick && freeSwingPending;
    }

    public synchronized boolean consumeFreeSwing(long tick) {
        if (!freeSwingReady(tick)) return false;
        freeSwingPending = false;
        return true;
    }

    public synchronized int remainingCounter(long tick) {
        expire(tick);
        return (int)Math.max(0, Math.min(Integer.MAX_VALUE, counterEnd - tick));
    }

    public synchronized void cancelGuard() { guardOpen = false; }

    public synchronized int remainingGuard(long tick) {
        expire(tick);
        return guardOpen ? (int) Math.max(0, guardEnd - tick) : 0;
    }

    public synchronized int remainingCooldown(long tick) {
        expire(tick);
        return (int) Math.max(0, cooldownEnd - tick);
    }

    private void expire(long tick) {
        if (guardOpen && tick >= guardEnd) {
            guardOpen = false;
            if (!guardSucceeded) {
                penaltyPending = true;
                penaltyEnd = cooldownEnd;
            }
        }
        if (penaltyPending && tick >= penaltyEnd) penaltyPending = false;
        if (tick >= counterEnd) freeSwingPending = false;
    }
}
