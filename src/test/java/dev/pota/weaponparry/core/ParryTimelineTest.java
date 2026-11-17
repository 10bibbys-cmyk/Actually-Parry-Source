package dev.pota.weaponparry.core;

/** Dependency-free state-machine regression checks; run with javac/java. */
public final class ParryTimelineTest {
    public static void main(String[] args) {
        var p = new ParryTimeline();
        check(p.activate(100,10,40),"can start");
        check(!p.activate(101,10,40),"cannot restart active guard");
        check(p.parryHit(102,20),"first success");
        check(p.parryHit(108,20),"second hit within same window");
        check(!p.guardActive(110),"window ends on time");
        check(p.activate(110,10,40),"success resets cooldown for next guard");
        check(p.counterReady(127),"counter refreshed from second deflect");
        check(!p.counterReady(128),"counter expires");
        var failed = new ParryTimeline();
        check(failed.activate(100,10,40),"start failed case");
        check(!failed.penaltyReady(105),"no penalty before miss");
        check(!failed.guardActive(110),"missed guard expires");
        check(failed.penaltyReady(110),"miss grants penalty");
        check(!failed.activate(120,10,40),"failed guard cooldown persists");
        failed.consumePenalty();
        check(!failed.penaltyReady(121),"penalty consumed exactly once");
        check(failed.activate(140,10,40),"cooldown finished");
        var behind = new ParryTimeline();
        behind.activate(100,10,40);
        behind.missedGuard(103);
        check(!behind.guardActive(104) && behind.penaltyReady(104),"flank breaks guard");
        var free = new ParryTimeline();
        free.activate(10,10,40);
        free.parryHit(11,20);
        check(free.consumeFreeSwing(12),"first BCE attack free");
        check(!free.consumeFreeSwing(13),"second BCE attack not free");
        check(free.counterReady(15),"counter damage token survives cost packet");
        free.consumeCounter();
        check(!free.counterReady(15),"counter consumed on confirmed damage");

        var locked = new ParryTimeline();
        check(locked.activate(400,10,40),"guard starts immediately");
        check(locked.guardActive(400),"guard active at first tick");
        check(locked.guardActive(409),"guard active at last tick");
        check(!locked.guardActive(410),"guard expires at tick ten");
        check(!locked.activate(410,10,40),"failed parry still on cooldown");
        var chain = new ParryTimeline();
        check(chain.activate(500,10,40),"chain initial guard");
        check(chain.parryHit(502,20),"successful guard");
        check(chain.remainingCooldown(503) == 0, "successful parry resets cooldown");
        check(chain.guardActive(509),"success still guards through window");
        check(!chain.guardActive(510),"success does not extend window");
        check(chain.activate(510,10,40),"next parry allowed after success");
        check(locked.remainingCooldown(410) == 30, "cooldown remaining after missed parry");
        check(chain.remainingGuard(510) == 10, "next guard has full duration");
        check(chain.remainingCooldown(510) == 40, "next guard starts a new cooldown");
        System.out.println("PASS: 32 parry timeline assertions");
    }
    private static void check(boolean condition,String name) {
        if (!condition) throw new AssertionError(name);
    }
}
