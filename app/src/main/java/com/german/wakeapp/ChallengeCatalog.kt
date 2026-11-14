package com.german.wakeapp

/**
 * Catálogo central de minijuegos disponibles.
 * Para añadir uno nuevo: añade una entrada aquí y su render en ChallengeActivity.
 * La app leerá esta lista para poblar los selectores de la UI.
 */
data class ChallengeType(
    val id: String,
    val label: String,
    val emoji: String
)

object ChallengeCatalog {
    val all = listOf(
        ChallengeType("MATH", "Cálculo", "∑"),
        ChallengeType("TEXT", "Frase", "✎"),
        ChallengeType("TRACING", "Delinear", "⌖")
    )

    fun byId(id: String): ChallengeType =
        all.firstOrNull { it.id == id } ?: all.first()

    fun label(id: String): String = byId(id).label

    /**
     * Construye una secuencia de [rounds] tipos ciclando [selectedTypes].
     * Si [selectedTypes] está vacío, devuelve [rounds] copias de "MATH".
     * Si hay más tipos que rondas, se truncan las primeras [rounds].
     * Si hay menos tipos que rondas, se ciclan hasta completar.
     */
    fun buildSequence(selectedTypes: List<String>, rounds: Int): List<String> {
        val safeRounds = rounds.coerceIn(1, 10)
        val pool = selectedTypes.ifEmpty { listOf("MATH") }
        return (0 until safeRounds).map { pool[it % pool.size] }
    }
}