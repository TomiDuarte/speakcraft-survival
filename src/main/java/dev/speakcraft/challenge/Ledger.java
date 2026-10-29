package dev.speakcraft.challenge;

import java.util.*;

/** Server-thread owned escrow. T is an owned stack, never an inventory reference. */
public final class Ledger<T> {
    public record Parcel<T>(String item, List<T> stacks) {
        public Parcel { Objects.requireNonNull(item); stacks = List.copyOf(stacks); }
    }
    public record Snapshot<T>(String language, Set<String> unlocked, int passed, int failed,
                              List<Parcel<T>> pending, List<T> deliveries) {
        public Snapshot {
            unlocked = Set.copyOf(unlocked); pending = List.copyOf(pending); deliveries = List.copyOf(deliveries);
        }
    }
    private String language = "es";
    private final Set<String> unlocked = new HashSet<>();
    private final List<Parcel<T>> pending = new ArrayList<>();
    private final List<T> deliveries = new ArrayList<>();
    private int passed, failed;
    private UUID token;
    private ChallengeState state = ChallengeState.PENDING;
    private long deadline;
    private boolean suspended;

    public Ledger() { }
    public Ledger(Snapshot<T> saved) {
        language(saved.language()); unlocked.addAll(saved.unlocked());
        passed = saved.passed(); failed = saved.failed();
        pending.addAll(saved.pending()); deliveries.addAll(saved.deliveries());
        // A loaded challenge needs a fresh microphone handshake; elapsed time is never restored.
    }
    public Snapshot<T> snapshot() { return new Snapshot<>(language, unlocked, passed, failed, pending, deliveries); }
    public void language(String value) {
        if (!Set.of("es", "de", "ja").contains(value)) throw new IllegalArgumentException("Unsupported language");
        language = value;
    }
    public String language() { return language; }
    public boolean unlocked(String item) { return unlocked.contains(item); }
    public boolean hold(String item, T ownedStack) {
        if (unlocked(item)) return false;
        for (int i = 0; i < pending.size(); i++) {
            var p = pending.get(i);
            if (p.item().equals(item)) {
                var stacks = new ArrayList<>(p.stacks()); stacks.add(ownedStack);
                pending.set(i, new Parcel<>(item, stacks)); return true;
            }
        }
        pending.add(new Parcel<>(item, List.of(ownedStack))); return true;
    }
    public UUID begin() {
        if (token == null && !pending.isEmpty() && !suspended) {
            token = UUID.randomUUID(); state = ChallengeState.PENDING;
        }
        return token;
    }
    public boolean listen(UUID id, long now, int seconds) {
        if (!valid(id) || state != ChallengeState.PENDING) return false;
        if (seconds < 5 || seconds > 60) throw new IllegalArgumentException("Duration out of range");
        deadline = now + seconds * 1000L; state = ChallengeState.LISTENING; return true;
    }
    public boolean answer(UUID id, boolean matched, long now) {
        if (!valid(id) || state != ChallengeState.LISTENING) return false;
        if (now >= deadline) return false;
        if (!matched) return false; // Another spoken attempt is allowed.
        var parcel = pending.removeFirst();
        unlocked.add(parcel.item()); deliveries.addAll(parcel.stacks()); passed++;
        state = ChallengeState.SUCCESS; token = null; return true;
    }
    public boolean expire(long now) {
        if (token == null || state != ChallengeState.LISTENING || now < deadline) return false;
        pending.removeFirst(); failed++; state = ChallengeState.FAILED; token = null; return true;
    }
    public boolean suspend(UUID id) {
        if (!valid(id)) return false;
        state = ChallengeState.CANCELLED; token = null; suspended = true; return true;
    }
    public void pause() {
        if (token != null) suspend(token);
    }
    public void resume() { suspended = false; }
    public void reset() {
        // Retain objects but invalidate the old response token, including on administrative reset.
        token = null; state = ChallengeState.PENDING; suspended = false;
        unlocked.clear(); passed = 0; failed = 0;
    }
    public boolean valid(UUID id) { return id != null && id.equals(token); }
    public UUID token() { return token; }
    public Parcel<T> current() { return pending.isEmpty() ? null : pending.getFirst(); }
    public ChallengeState state() { return state; }
    public long remaining(long now) { return state == ChallengeState.LISTENING ? Math.max(0, deadline - now) : 0; }
    public List<T> deliveries() { return Collections.unmodifiableList(deliveries); }
    public void deliveredFirst() { deliveries.removeFirst(); }
    public int queued() { return pending.size(); }
}
