package com.ubertob.kondor.json.schema

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.*

//TODO
// nullable values should be type: [X, null]
// sealed class -> one of
// other types can add more constrain (i.e. date format on string, max, min on Int and Long, Double etc.)


internal fun valueSchema(nodeKind: NodeKind<*>): JsonObjectNode =
    mapOf(
        "type" to nodeKind.desc.lowercase().asNode()
    ).asNode()


internal fun <E : Enum<E>> enumSchema(values: List<E>): JsonObjectNode =

    mapOf(
        if (values.isEmpty())
            "type" to "string".asNode()
        else
            "enum" to values.map { it.name }.asNode()
    ).asNode()


internal fun arraySchema(itemsConverter: JsonConverter<*, *>): JsonObjectNode =
    mapOf(
        "type" to "array".asNode(),
        "items" to itemsConverter.schema()
    ).asNode()

internal fun objectSchema(properties: Iterable<JsonProperty<*>>): JsonObjectNode {

    val reqProp = mutableListOf<String>()
    val pmap = properties.flatMap { prop ->

        when (prop) {
            is JsonPropMandatory<*, *> -> listOf(prop.propName to prop.converter.schema()).also { reqProp.add(prop.propName) }
            is JsonPropMandatoryFlatten<*> -> prop.converter.schemaProperties()
                .also { reqProp.addAll(prop.converter.schemaRequiredProperties()) }
            is JsonPropOptional<*, *> -> listOf(prop.propName to prop.converter.schema())
        }

    }.toMap()
    val propNode = JsonObjectNode(pmap)

    val map = mapOf(
        "type" to "object".asNode(),
        "properties" to propNode,
        "required" to reqProp.asNode()
    )

    return JsonObjectNode(map)
}

fun sealedSchema(
    discriminatorFieldName: String,
    subConverters: Map<String, ObjectNodeConverter<*>>
): JsonObjectNode {
    val subMaps: List<JsonNode> = subConverters.map { (name, conv) ->
        val required = conv.schema()._fieldMap["required"]
        conv.schema()._fieldMap["properties"]
            .let { it as JsonObjectNode }
            .let { it._fieldMap + (discriminatorFieldName to listOf("type" to "string", "const" to name).asNode()) }.asNode()
            .let { mapOf("properties" to it) }
            .let {
                if (required != null && (required as JsonNodeArray).elements.count() > 0)
                    it + ("required" to required)
                else
                    it
            }.asNode()
    }

    val map = mapOf(
        "type" to "object".asNode(),
        "description" to "discriminant field: $discriminatorFieldName".asNode(),
        "oneOf" to subMaps.asNodes()
    )

    return JsonObjectNode(map)
}


private fun ObjectNodeConverter<*>.schemaProperties(): List<Pair<String, JsonNode>> =
    (schema()._fieldMap["properties"] as JsonObjectNode)._fieldMap.entries.map { it.key to it.value }

private fun ObjectNodeConverter<*>.schemaRequiredProperties(): List<String> =
    (schema()._fieldMap["required"] as JsonNodeArray).elements.map { (it as JsonNodeString).text }

internal fun String.asNode() = JsonNodeString(this)
internal fun List<String>.asNode() = JsonNodeArray(this.map { it.asNode() })
internal fun List<JsonNode>.asNodes() = JsonNodeArray(this)
internal fun FieldMap.asNode() = JsonObjectNode(this)
internal fun List<Pair<String, String>>.asNode() = JsonObjectNode(
    map { it.first to it.second.asNode() }.toMap()
)
