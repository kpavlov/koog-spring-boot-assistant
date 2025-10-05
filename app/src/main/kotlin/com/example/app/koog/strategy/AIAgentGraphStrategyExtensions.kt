package com.example.app.koog.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.AIAgentNode

/**
 * AIAgentGraphStrategy utility for generating Mermaid diagrams from agent strategies.
 *
 * References: https://docs.koog.ai/complex-workflow-agents/
 */
public fun <I : Any, O : Any> AIAgentGraphStrategy<I, O>.createMermaidDiagram(): String =
    try {
        val graphData = collectGraphData()
        graphData.toMermaidDiagram()
    } catch (e: Exception) {
        throw RuntimeException("Can't generate Mermaid diagram for graph", e)
    }

/**
 * Data class representing collected graph information for mermaid diagram generation.
 */
private data class GraphData(
    val nodes: Map<String, AIAgentNode<*, *>>,
    val edges: List<EdgeInfo>,
)

/**
 * Data class representing edge information with condition.
 */
private data class EdgeInfo(
    val fromNode: AIAgentNode<*, *>,
    val toNode: AIAgentNode<*, *>,
    val condition: String?,
)

/**
 * Data class representing raw edge information extracted from reflection.
 */
private data class RawEdgeInfo(
    val toNode: AIAgentNode<*, *>?,
    val condition: String?,
)

/**
 * Collects all graph data (nodes and edges) from the strategy.
 */
private fun <I : Any, O : Any> AIAgentGraphStrategy<I, O>.collectGraphData(): GraphData {
    val nodes = mutableMapOf<String, AIAgentNode<*, *>>()
    val edges = mutableListOf<EdgeInfo>()

    // Add essential nodes
    nodes[nodeStart.id] = nodeStart
    nodes[nodeFinish.id] = nodeFinish

    // Collect all nodes from metadata
    metadata.nodesMap.forEach { (_, node) ->
        if (node is AIAgentNode<*, *>) {
            nodes[node.id] = node
        }
    }

    // Edges are extracted from individual nodes using their public API

    // Collect edges from all nodes
    val allNodes =
        listOf(nodeStart as AIAgentNode<*, *>) +
            metadata.nodesMap.values.filterIsInstance<AIAgentNode<*, *>>()

    collectEdgesRecursively(allNodes, edges, nodes)

    return GraphData(nodes.toMap(), edges.toList())
}

/**
 * Recursively collect edges from nodes using tail recursion optimization.
 */
private tailrec fun collectEdgesRecursively(
    remainingNodes: List<AIAgentNode<*, *>>,
    edges: MutableList<EdgeInfo>,
    nodes: MutableMap<String, AIAgentNode<*, *>>,
) {
    if (remainingNodes.isEmpty()) return

    val currentNode = remainingNodes.first()
    val restNodes = remainingNodes.drop(1)

    // Extract edges from current node
    try {
        val nodeEdges = currentNode.extractEdges()
        nodeEdges.forEach { edge ->
            edge.toNode?.let { toNode ->
                nodes[currentNode.id] = currentNode
                nodes[toNode.id] = toNode
                edges.add(EdgeInfo(currentNode, toNode, edge.condition))
            }
        }
    } catch (e: Exception) {
        // Skip nodes that don't have edges or can't be processed
    }

    // Tail recursive call
    collectEdgesRecursively(restNodes, edges, nodes)
}

/**
 * Extension function to extract edges from an AIAgentNode using public API only.
 */
private fun AIAgentNode<*, *>.extractEdges(): List<RawEdgeInfo> =
    try {
        // Use the public edges property to get edge information
        this.edges.mapNotNull { edge ->
            extractEdgeInfo(edge)
        }
    } catch (e: Exception) {
        emptyList()
    }

/**
 * Extracts condition information from ForwardOutput class name.
 */
private fun extractConditionFromClassName(className: String?): String? =
    when {
        className == null -> null
        className.contains("onCondition") -> "onCondition"
        className.contains("onToolCall") -> "onToolCall"
        className.contains("onAssistantMessage") -> "onAssistantMessage"
        className.contains("transformed") -> "transformed"
        className.contains("forwardTo") -> null // Simple forward, no condition
        else -> null
    }

/**
 * Extracts edge information from an AIAgentEdge using public API only.
 */
private fun extractEdgeInfo(edge: Any): RawEdgeInfo? {
    val toNode =
        runCatching {
            edge::class.java.getMethod("getToNode").invoke(edge) as? AIAgentNode<*, *>
        }.getOrElse { return null }

    val forwardOutput =
        runCatching {
            edge::class.java.methods
                .firstOrNull { it.name == "getForwardOutput\$agents_core" || it.name == "getForwardOutput" }
                ?.invoke(edge)
        }.getOrNull()

    return RawEdgeInfo(toNode, extractConditionFromForwardOutput(forwardOutput))
}

/**
 * Extracts condition information from ForwardOutput function.
 */
private fun extractConditionFromForwardOutput(forwardOutput: Any?): String? {
    return try {
        if (forwardOutput == null) return null

        // The ForwardOutput is a function, we need to examine its class name or toString
        val className = forwardOutput::class.java.name
        val toString = forwardOutput.toString()

        // Try to extract condition from class name or string representation
        extractConditionFromClassName(className) ?: extractConditionFromString(toString)
    } catch (e: Exception) {
        null
    }
}

/**
 * Extracts condition information from string representation.
 */
private fun extractConditionFromString(str: String): String? =
    when {
        str.contains("onCondition") -> "onCondition"
        str.contains("onToolCall") -> "onToolCall"
        str.contains("onAssistantMessage") -> "onAssistantMessage"
        str.contains("transformed") -> "transformed"
        else -> null
    }

/**
 * Extension function to convert GraphData to mermaid diagram string.
 */
private fun GraphData.toMermaidDiagram(): String =
    buildString {
        appendLine("graph TD")

        // Render nodes
        nodes.values.forEach { node ->
            appendLine("    ${node.toMermaidNode()}")
        }

        // Add blank line before edges if there are any
        if (edges.isNotEmpty()) {
            appendLine()
            // Render edges
            edges.forEachIndexed { index, edge ->
                if (index == edges.size - 1) {
                    // Last edge - don't add newline
                    append("    ${edge.toMermaidEdge()}")
                } else {
                    appendLine("    ${edge.toMermaidEdge()}")
                }
            }
        }
    }.trimEnd()

/**
 * Extension function to render an EdgeInfo as a mermaid edge string.
 */
private fun EdgeInfo.toMermaidEdge(): String =
    if (condition != null && condition.isNotBlank()) {
        "${fromNode.id} --> |\"$condition\"| ${toNode.id}"
    } else {
        "${fromNode.id} --> ${toNode.id}"
    }

/**
 * Extension function to render an AIAgentNode as a mermaid node string.
 */
private fun AIAgentNode<*, *>.toMermaidNode(): String = "$id[\"${name}\"]"