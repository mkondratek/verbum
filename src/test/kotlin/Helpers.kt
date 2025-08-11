fun waitFor(
    timeoutMs: Long = 5_000L,
    pollMs: Long = 50L,
    predicate: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!predicate()) {
        if (System.currentTimeMillis() > deadline) {
            throw AssertionError("Timeout waiting for event")
        }
        Thread.sleep(pollMs)
    }
}
