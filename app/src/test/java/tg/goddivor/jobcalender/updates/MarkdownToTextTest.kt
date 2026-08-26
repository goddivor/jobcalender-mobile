package tg.goddivor.jobcalender.updates

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownToTextTest {

    @Test
    fun `rejoins a sentence broken by the source line width`() {
        val body = """
            Une phrase écrite sur deux lignes dans le fichier
            de notes, qui doit se relire d'un seul tenant.
        """.trimIndent()

        assertEquals(
            "Une phrase écrite sur deux lignes dans le fichier de notes, " +
                "qui doit se relire d'un seul tenant.",
            markdownToText(body),
        )
    }

    @Test
    fun `keeps a bullet on its own line and folds its continuation into it`() {
        val body = """
            - Première entrée, elle aussi coupée
            au milieu.
            - Deuxième entrée.
        """.trimIndent()

        assertEquals(
            "• Première entrée, elle aussi coupée au milieu.\n• Deuxième entrée.",
            markdownToText(body),
        )
    }

    @Test
    fun `a blank line still separates a heading from what precedes it`() {
        val body = """
            **Réglages**

            - Une entrée.

            **Synchronisation**
        """.trimIndent()

        assertEquals("Réglages\n• Une entrée.\nSynchronisation", markdownToText(body))
    }
}
