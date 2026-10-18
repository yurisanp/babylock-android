package app.babylock.unlock

/**
 * Regras da resposta digitada no desbloqueio (PIN ou conta de multiplicar), sem depender do Android.
 * [maxFailures] erros seguidos bloqueiam novas tentativas por [lockoutMillis].
 */
class UnlockGate(
    private val checkPin: (String) -> Boolean,
    private val hasPin: () -> Boolean,
    private val challenge: Pair<Int, Int>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxFailures: Int = 5,
    private val lockoutMillis: Long = 30_000L,
) {
    sealed interface Result {
        data object Ok : Result
        data object Wrong : Result
        data class LockedOut(val secondsLeft: Long) : Result
    }

    var failures = 0
        private set
    private var lockedUntil = 0L

    fun submit(answer: String): Result {
        val now = clock()
        if (now < lockedUntil) return Result.LockedOut((lockedUntil - now) / 1000 + 1)
        val ok = if (hasPin()) checkPin(answer) else answer.toIntOrNull() == challenge.first * challenge.second
        if (ok) {
            failures = 0
            return Result.Ok
        }
        failures += 1
        if (failures >= maxFailures) {
            failures = 0
            lockedUntil = now + lockoutMillis
            return Result.LockedOut(lockoutMillis / 1000)
        }
        return Result.Wrong
    }
}
