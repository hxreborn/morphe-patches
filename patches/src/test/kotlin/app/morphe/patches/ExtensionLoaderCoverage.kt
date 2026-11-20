/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

internal class ExtensionLoaderCoverage(
    private val bytecode: PatchBytecode,
    private val payloadsByClass: Map<String, String>,
) {
    class Member(val className: String, val field: String)

    private val calledDescriptor = Regex("""L((?:app/hxreborn/extension|app/morphe/extension)/[\w/$]+);""")

    private val dottedName = Regex("""app\.(?:hxreborn|morphe)\.extension\.(?:\w+\.)+[A-Z][\w$]*""")

    private val packagePrefix = Regex("""^L?(app[/.](?:hxreborn|morphe)[/.]extension(?:[/.][a-z][a-z0-9]*)*)[/.]?(?:\u0001)?;?$""")

    private val componentShape = Regex("""^[A-Z][A-Za-z0-9_$]*(?:Activity|Provider|Service|Receiver|View|Dialog|Fragment)(?:[$][A-Za-z0-9_]+)*$""")

    private val simpleClassName = Regex("""^[A-Z][A-Za-z0-9_]*(?:[$][A-Za-z0-9_]+)*$""")

    fun calledBy(member: Member): Set<String> {
        val strings = bytecode.stringsReachedBy(member.className, member.field)
        val literal = strings.flatMap { text ->
            calledDescriptor.findAll(text).map { it.groupValues[1] } +
                dottedName.findAll(text).map { it.value.replace('.', '/') }
        }
        val arguments = bytecode.qualifiedArguments(member.className, member.field) { packagePrefix.matches(it) }
        val qualified = arguments.flatMap { (prefix, names) ->
            val normalized = packagePrefix.find(prefix)!!.groupValues[1].replace('.', '/')
            names.filter { simpleClassName.matches(it) && (payloadsByClass.containsKey("$normalized/$it") || componentShape.matches(it)) }
                .map { "$normalized/$it" }
        }
        return (literal + qualified).toSet()
    }

    fun problems(root: String, closure: List<Member>, loaded: Set<String>): List<String> {
        val problems = mutableListOf<String>()
        for (member in closure) {
            for (descriptor in calledBy(member)) {
                val owner = payloadsByClass[descriptor]
                val where = "${member.className}#${member.field}"
                when {
                    owner == null -> problems += "$where names $descriptor, which no payload defines"
                    owner !in loaded -> problems += "$root reaches $where, which calls $owner, but no patch in its dependencies loads it"
                }
            }
        }
        return problems
    }
}
