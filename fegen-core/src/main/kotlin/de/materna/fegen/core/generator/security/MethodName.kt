package de.materna.fegen.core.generator.security

enum class MethodName(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    READ_ONE("readOne"),
    READ_ALL("readAll")
}
