package com.example.openglow.domain.classifier

import java.io.File

data class TokenizedText(
    val inputIds: IntArray,
    val attentionMask: IntArray,
    val tokenTypeIds: IntArray,
)

class KoreanTokenizer(
    vocabFile: File,
    private val maxLength: Int = 128,
) {
    private val vocab: Map<String, Int> = vocabFile.readLines(Charsets.UTF_8)
        .mapIndexed { index, token -> token.trim() to index }
        .filter { it.first.isNotBlank() }
        .toMap()

    private val clsId = vocab["[CLS]"] ?: 2
    private val sepId = vocab["[SEP]"] ?: 3
    private val padId = vocab["[PAD]"] ?: 0
    private val unkId = vocab["[UNK]"] ?: 1

    fun tokenize(text: String): TokenizedText {
        val tokens = mutableListOf(clsId)
        basicTokenize(text).forEach { token ->
            if (tokens.size >= maxLength - 1) return@forEach
            tokens += wordPiece(token)
        }
        if (tokens.size >= maxLength) {
            tokens[maxLength - 1] = sepId
        } else {
            tokens += sepId
        }

        val inputIds = IntArray(maxLength) { padId }
        val attentionMask = IntArray(maxLength)
        val tokenTypeIds = IntArray(maxLength)
        tokens.take(maxLength).forEachIndexed { index, id ->
            inputIds[index] = id
            attentionMask[index] = 1
        }

        return TokenizedText(inputIds, attentionMask, tokenTypeIds)
    }

    private fun basicTokenize(text: String): List<String> {
        return text
            .replace(Regex("([,.!?;:()\\[\\]{}])"), " $1 ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(maxLength * 2)
    }

    private fun wordPiece(token: String): List<Int> {
        vocab[token]?.let { return listOf(it) }

        val pieces = mutableListOf<Int>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var current: String? = null
            while (start < end) {
                val sub = token.substring(start, end)
                val candidate = if (start == 0) sub else "##$sub"
                if (candidate in vocab) {
                    current = candidate
                    break
                }
                end--
            }
            if (current == null) return listOf(unkId)
            pieces += vocab[current] ?: unkId
            start = end
            if (pieces.size >= maxLength - 2) break
        }
        return pieces.ifEmpty { listOf(unkId) }
    }
}
