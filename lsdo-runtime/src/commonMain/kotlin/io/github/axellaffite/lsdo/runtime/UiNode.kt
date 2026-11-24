package io.github.axellaffite.lsdo.runtime

import kotlinx.serialization.Serializable

@Serializable
public data class UiBounds(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * One element of what is rendered, read from the platform's accessibility tree (what a screen reader
 * sees), so it shows the screen as a user gets it rather than as the state says it should be.
 */
@Serializable
public data class UiNode(
    val role: String? = null,
    val text: String? = null,
    val value: String? = null,
    val flags: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val bounds: UiBounds? = null,
    val children: List<UiNode> = emptyList(),
) {
    val meaningful: Boolean
        get() = role != null || !text.isNullOrBlank() || !value.isNullOrBlank() || flags.isNotEmpty() || actions.isNotEmpty()
}

/** Drops the layout wrappers that say nothing, keeping their children in place. */
public fun UiNode.pruned(): List<UiNode> {
    val kids = children.flatMap { it.pruned() }
    return if (meaningful) listOf(copy(children = kids)) else kids
}

/** One line per node, indented by nesting: `role "text" = "value" {flags} [actions]`. */
public fun List<UiNode>.render(bounds: Boolean = false): String = buildString {
    fun line(node: UiNode, depth: Int) {
        append("  ".repeat(depth))
        append(node.role ?: "node")
        node.text?.takeIf { it.isNotBlank() }?.let { append(" \"").append(it.replace("\n", " ")).append('"') }
        node.value?.takeIf { it.isNotBlank() }?.let { append(" = \"").append(it.replace("\n", " ")).append('"') }
        if (node.flags.isNotEmpty()) append(node.flags.joinToString(",", " {", "}"))
        if (node.actions.isNotEmpty()) append(node.actions.joinToString(",", " [", "]"))
        if (bounds) node.bounds?.let { append(" @${it.x},${it.y} ${it.width}x${it.height}") }
        append('\n')
        node.children.forEach { line(it, depth + 1) }
    }
    this@render.forEach { line(it, 0) }
}.trimEnd()
