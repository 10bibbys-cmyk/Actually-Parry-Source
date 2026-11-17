package dev.pota.weaponparry.core;

/** Additional boundary/consumption tests run in release CI without Minecraft. */
public final class ParryTimelineReleaseTest {
    private static int checked;
    private static void ok(boolean value, String message) {
        checked++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ParryTimeline guard = new ParryTimeline();
        ok(!guard.guardActive(0), "no guard before activation");
        ok(guard.activate(100, 10, 40), "activate guard");
        ok(guard.guardActive(100), "guard starts immediately");
        ok(!guard.activate(101, 10, 40), "cannot spam guard");
        ok(guard.guardActive(109), "guard tick 9");
        ok(!guard.guardActive(110), "guard expires at tick 10");
        ok(guard.penaltyReady(110), "failed guard becomes punishable");
        ok(guard.penaltyReady(139), "miss penalty lasts through recovery");
        ok(!guard.penaltyReady(140), "miss penalty expires with recovery");
        ok(guard.activate(140, 10, 40), "can rearm after recovery");

        ParryTimeline many = new ParryTimeline();
        ok(many.activate(200, 10, 40), "activate multihit guard");
        ok(many.parryHit(201, 20), "first parry");
        ok(many.remainingCooldown(201) == 0, "first success resets cooldown");
        ok(many.parryHit(205, 20), "second parry while guard up");
        ok(many.parryHit(209, 20), "last tick parry");
        ok(many.remainingCounter(209) == 20, "counter refreshes on parry");
        ok(!many.parryHit(210, 20), "parry fails after window");
        ok(!many.penaltyReady(210), "successful guard is not punishable");
        ok(many.activate(210, 10, 40), "next guard allowed after success");

        ParryTimeline flank = new ParryTimeline();
        flank.activate(300, 10, 40);
        flank.missedGuard(301);
        ok(!flank.guardActive(301), "flank breaks failed guard");
        ok(flank.penaltyReady(301), "flank triggers bonus vulnerability");
        flank.consumePenalty();
        ok(!flank.penaltyReady(302), "penalty consumed only once");
        ParryTimeline success = new ParryTimeline();
        success.activate(400, 10, 40);
        success.parryHit(401, 20);
        success.missedGuard(402);
        ok(success.guardActive(402), "successful guard persists");
        ok(!success.penaltyReady(402), "successful guard never gets flank penalty");

        ParryTimeline counter = new ParryTimeline();
        counter.activate(500, 10, 40);
        counter.parryHit(501, 20);
        ok(counter.freeSwingReady(502), "free counter stamina available");
        ok(counter.consumeFreeSwing(502), "first stamina deduction bypassed");
        ok(!counter.consumeFreeSwing(503), "no second free deduction");
        ok(counter.counterReady(503), "counter damage remains after stamina use");
        counter.consumeCounter();
        ok(!counter.counterReady(503), "counter damage consumed");
        ok(!counter.freeSwingReady(503), "free stamina cleared on damage counter consumption");

        ParryTimeline expired = new ParryTimeline();
        expired.activate(600, 10, 40);
        expired.parryHit(601, 20);
        ok(expired.counterReady(620), "counter valid through tick 19");
        ok(!expired.counterReady(621), "counter expires at tick 20");
        ok(!expired.freeSwingReady(621), "free stamina expires with counter");

        ParryTimeline noCooldown = new ParryTimeline();
        noCooldown.activate(700, 10, 0);
        ok(noCooldown.guardActive(700), "zero cooldown still guards");
        ok(!noCooldown.activate(700, 10, 0), "zero cooldown cannot overlap active guard");
        noCooldown.cancelGuard();
        ok(!noCooldown.guardActive(701), "cancel guard works");
        ok(!noCooldown.penaltyReady(701), "cancel does not create missed penalty");
        ok(noCooldown.activate(701, 10, 0), "can activate after cancellation when cooldown zero");
        System.out.println("PASS: " + checked + " release candidate assertions");
    }
}
