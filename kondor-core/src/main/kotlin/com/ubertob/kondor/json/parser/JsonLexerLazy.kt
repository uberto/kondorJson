package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.outcome.asSuccess
import java.io.InputStream

class JsonLexerLazy(private val text: String) {
    private var currentIndex = 0

    fun tokenize(): JsonOutcome<TokensStreamEager> =
        TokensStreamEager(PeekingIteratorWrapper(LazyTokenStream(this::nextToken))).asSuccess() //!!! handle failure

    companion object {
        fun fromInputStream(inputStream: InputStream): JsonLexerLazy =
            JsonLexerLazy(inputStream.bufferedReader().readText())
    }

    private fun nextToken(): KondorToken? {
        skipWhitespace()

        if (currentIndex >= text.length)
            return null

        return when (val c = text[currentIndex]) {
            '{', '}', '[', ']', ':', ',' -> readPunctuation(c)
            '"' -> readString()
            in '0'..'9', '-' -> readNumber()
            't', 'f' -> readBoolean()
            'n' -> readNull()
            else -> error("!!!Unexpected character: $c at position $currentIndex")
        }
    }

    private fun skipWhitespace() {
        while (currentIndex < text.length && text[currentIndex].isWhitespace()) {
            currentIndex++
        }
    }

    private fun readPunctuation(c: Char): SeparatorToken {
        val start = currentIndex
        currentIndex++
        return when (c) {
            '{' -> OpeningCurlySep
            '}' -> ClosingCurlySep
            '[' -> OpeningSquareSep
            ']' -> ClosingSquareSep
            ':' -> ColonSep
            ',' -> CommaSep
            '"' -> OpeningQuotesSep
            else -> error("!!!Unexpected punctuation character: $c at position $start")
        }
    }

    private fun readString(): ValueToken {
        val start = currentIndex
        currentIndex++ // skip opening quote

        while (currentIndex < text.length && text[currentIndex] != '"') {
            if (text[currentIndex] == '\\') {
                currentIndex++ // skip escape character
            }
            currentIndex++
        }

        if (currentIndex >= text.length) {
            error("!!!Unterminated string starting at position $start")
        }

        currentIndex++ // skip closing quote
        return ValueTokenEager(text.substring(start, currentIndex), start)
    }

    private fun readNumber(): ValueToken {
        val start = currentIndex

        // Skip minus sign if present
        if (text[currentIndex] == '-') {
            currentIndex++
        }

        // Read digits before decimal point
        while (currentIndex < text.length && text[currentIndex].isDigit()) {
            currentIndex++
        }

        // Read decimal point and following digits if present
        if (currentIndex < text.length && text[currentIndex] == '.') {
            currentIndex++
            while (currentIndex < text.length && text[currentIndex].isDigit()) {
                currentIndex++
            }
        }

        // Read exponent if present
        if (currentIndex < text.length && (text[currentIndex] == 'e' || text[currentIndex] == 'E')) {
            currentIndex++
            if (currentIndex < text.length && (text[currentIndex] == '+' || text[currentIndex] == '-')) {
                currentIndex++
            }
            while (currentIndex < text.length && text[currentIndex].isDigit()) {
                currentIndex++
            }
        }

        return ValueTokenEager(text.substring(start, currentIndex), start)
    }

    private fun readBoolean(): ValueToken {
        val start = currentIndex
        val isTrue = text[currentIndex] == 't'

        val expected = if (isTrue) "true" else "false"
        val end = currentIndex + expected.length

        if (end > text.length || text.substring(currentIndex, end) != expected) {
            error("!!!Invalid boolean at position $start")
        }

        currentIndex = end
        return ValueTokenEager(text.substring(start, currentIndex), start)
    }

    private fun readNull(): ValueToken {
        val start = currentIndex
        val end = currentIndex + 4

        if (end > text.length || text.substring(currentIndex, end) != "null") {
            error("!!!Invalid null at position $start")
        }

        currentIndex = end
        return ValueTokenEager(text.substring(start, currentIndex), start)
    }
} 