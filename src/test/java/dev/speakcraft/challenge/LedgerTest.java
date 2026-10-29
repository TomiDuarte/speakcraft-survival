package dev.speakcraft.challenge;

import dev.speakcraft.language.AnswerMatcher;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {
    record Stack(String id, int count, String components) { }
    private static final String OAK = "minecraft:oak_log", DIAMOND = "minecraft:diamond";
    private Ledger<Stack> first(int count) {
        var ledger = new Ledger<Stack>();
        assertTrue(ledger.hold(OAK, new Stack(OAK, count, "original")));
        return ledger;
    }
    @Test void firstItemHeldUntilRealAnswer() {
        var ledger = first(1); var id = ledger.begin();
        assertEquals(ChallengeState.PENDING, ledger.state()); assertTrue(ledger.deliveries().isEmpty());
        assertFalse(ledger.answer(id, true, 0));
        assertTrue(ledger.listen(id, 1000, 15)); assertTrue(ledger.answer(id, true, 2000));
        assertTrue(ledger.unlocked(OAK)); assertEquals(1, ledger.deliveries().getFirst().count());
    }
    @Test void unlockedItemBypassesEscrow() {
        var ledger = first(1); var id = ledger.begin(); ledger.listen(id, 0, 15); ledger.answer(id, true, 1);
        assertFalse(ledger.hold(OAK, new Stack(OAK, 64, ""))); assertEquals(0, ledger.queued());
    }
    @Test void correctSpeechComparedByServer() {
        var ledger = first(1); var id = ledger.begin(); ledger.listen(id, 0, 15);
        assertTrue(ledger.answer(id, AnswerMatcher.matches("¡Tronco de roble!", List.of("Tronco de roble"), "es", .1), 1));
    }
    @Test void incorrectSpeechCanBeRetried() {
        var ledger = first(1); var id = ledger.begin(); ledger.listen(id, 0, 15);
        assertFalse(ledger.answer(id, false, 5000)); assertEquals(ChallengeState.LISTENING, ledger.state());
        assertTrue(ledger.answer(id, true, 6000)); assertEquals(0, ledger.snapshot().failed());
    }
    @Test void timeoutDeletesOnlyPendingAcquisition() {
        var ledger = first(7); var id = ledger.begin(); ledger.listen(id, 0, 15);
        assertFalse(ledger.expire(14999)); assertTrue(ledger.expire(15000));
        assertFalse(ledger.answer(id, true, 15000)); assertFalse(ledger.expire(16000));
        assertTrue(ledger.deliveries().isEmpty()); assertEquals(1, ledger.snapshot().failed());
    }
    @Test void entireStacksAndComponentsPreserved() {
        var ledger = first(64); var id = ledger.begin(); ledger.listen(id, 0, 15); ledger.answer(id, true, 1);
        assertEquals(new Stack(OAK, 64, "original"), ledger.deliveries().getFirst());
    }
    @Test void batchedCraftingOutputsMergeWithoutLosingQuantity() {
        var ledger = first(4);
        for (int i = 0; i < 15; i++) ledger.hold(OAK, new Stack(OAK, 4, "original"));
        assertEquals(1, ledger.queued()); var id = ledger.begin(); ledger.listen(id, 0, 15); ledger.answer(id, true, 1);
        assertEquals(64, ledger.deliveries().stream().mapToInt(Stack::count).sum());
    }
    @Test void snapshotRestoresUnlocksCountersAndLanguage() {
        var ledger = first(1); ledger.language("de"); var id = ledger.begin();
        ledger.listen(id, 0, 15); ledger.answer(id, true, 1);
        var restored = new Ledger<>(ledger.snapshot());
        assertTrue(restored.unlocked(OAK)); assertEquals("de", restored.language());
        assertEquals(1, restored.snapshot().passed()); assertEquals(ledger.deliveries(), restored.deliveries());
    }
    @Test void twoConsecutiveAcquisitionsHaveOnlyOneActiveChallenge() {
        var ledger = first(1); ledger.hold(DIAMOND, new Stack(DIAMOND, 2, ""));
        var id = ledger.begin(); assertEquals(id, ledger.begin());
        ledger.listen(id, 0, 15); ledger.answer(id, true, 1);
        assertEquals(DIAMOND, ledger.current().item()); assertNotEquals(id, ledger.begin());
    }
    @Test void restartRetainsEscrowButDoesNotResumeCountdown() {
        var ledger = first(6); var old = ledger.begin(); ledger.listen(old, 0, 15);
        var restored = new Ledger<>(ledger.snapshot()); var next = restored.begin();
        assertFalse(restored.answer(old, true, 10)); assertNotEquals(old, next);
        assertFalse(restored.expire(100000)); assertEquals(6, restored.current().stacks().getFirst().count());
    }
    @Test void resetDuringChallengeInvalidatesOldResponseWithoutDeletingItems() {
        var ledger = first(6); var old = ledger.begin(); ledger.listen(old, 0, 15); ledger.reset();
        assertFalse(ledger.answer(old, true, 10)); assertNotEquals(old, ledger.begin()); assertEquals(1, ledger.queued());
    }
    @Test void missingModelOrMicrophoneSuspendsWithoutPenalty() {
        var ledger = first(5); var id = ledger.begin(); assertTrue(ledger.suspend(id));
        assertNull(ledger.begin()); assertFalse(ledger.expire(1000000)); assertEquals(0, ledger.snapshot().failed());
        ledger.resume(); var retry = ledger.begin(); assertNotEquals(id, retry);
        assertEquals(5, ledger.current().stacks().getFirst().count());
    }
    @Test void repeatedSuccessAndDeliveryCannotDuplicateItems() {
        var ledger = first(64); var id = ledger.begin(); ledger.listen(id, 0, 15);
        assertTrue(ledger.answer(id, true, 1));
        for (int i = 0; i < 50; i++) assertFalse(ledger.answer(id, true, 2));
        assertEquals(1, ledger.deliveries().size()); ledger.deliveredFirst(); assertTrue(ledger.deliveries().isEmpty());
    }
    @Test void readyHandshakeCannotExtendTimer() {
        var ledger = first(1); var id = ledger.begin(); assertTrue(ledger.listen(id, 0, 15));
        assertFalse(ledger.listen(id, 14000, 15)); assertTrue(ledger.expire(15000));
    }
}
