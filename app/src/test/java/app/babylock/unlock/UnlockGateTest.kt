package app.babylock.unlock

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnlockGateTest {

    private var now = 1_000_000L
    private var pin: String? = null

    private fun gate(challenge: Pair<Int, Int> = 7 to 8) = UnlockGate(
        checkPin = { it == pin },
        hasPin = { pin != null },
        challenge = challenge,
        clock = { now },
    )

    @Test
    fun `sem PIN aceita a resposta certa da conta`() {
        assertThat(gate(7 to 8).submit("56")).isEqualTo(UnlockGate.Result.Ok)
    }

    @Test
    fun `sem PIN recusa resposta errada ou vazia`() {
        val g = gate(7 to 8)
        assertThat(g.submit("55")).isEqualTo(UnlockGate.Result.Wrong)
        assertThat(g.submit("")).isEqualTo(UnlockGate.Result.Wrong)
        assertThat(g.submit("abc")).isEqualTo(UnlockGate.Result.Wrong)
    }

    @Test
    fun `com PIN usa o PIN e ignora a conta`() {
        pin = "2468"
        val g = gate(7 to 8)
        assertThat(g.submit("56")).isEqualTo(UnlockGate.Result.Wrong)
        assertThat(g.submit("2468")).isEqualTo(UnlockGate.Result.Ok)
    }

    @Test
    fun `cinco erros seguidos bloqueiam por 30 segundos`() {
        pin = "2468"
        val g = gate()
        repeat(4) { assertThat(g.submit("0000")).isEqualTo(UnlockGate.Result.Wrong) }
        assertThat(g.submit("0000")).isEqualTo(UnlockGate.Result.LockedOut(30))
        // Mesmo o PIN certo é recusado durante a espera.
        now += 10_000
        assertThat(g.submit("2468")).isEqualTo(UnlockGate.Result.LockedOut(21))
        // Depois da espera volta a aceitar.
        now += 20_001
        assertThat(g.submit("2468")).isEqualTo(UnlockGate.Result.Ok)
    }

    @Test
    fun `acerto zera a contagem de erros`() {
        pin = "2468"
        val g = gate()
        repeat(4) { g.submit("0000") }
        assertThat(g.failures).isEqualTo(4)
        assertThat(g.submit("2468")).isEqualTo(UnlockGate.Result.Ok)
        assertThat(g.failures).isEqualTo(0)
        repeat(4) { assertThat(g.submit("0000")).isEqualTo(UnlockGate.Result.Wrong) }
    }
}
