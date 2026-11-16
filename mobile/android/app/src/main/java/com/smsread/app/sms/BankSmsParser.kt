package com.smsread.app.sms

import java.math.BigInteger

enum class Direction { DEPOSIT, WITHDRAWAL }
enum class Bank { MELLI, MELLAT, SADERAT }
data class ParsedSms(
    val bank: Bank,
    val direction: Direction,
    val amountIrr: Long,
    val balanceIrr: Long?,
    val accountHint: String?,
    val rawText: String,
    val dateText: String?,
    val parserVersion: String = "prototype-1"
)

/**
 * Prototype parser for synthetic fixtures only. Not production-verified against bank formats.
 * Rejects ambiguous amounts, unknown units and multiple conflicting bank identifiers.
 */
object BankSmsParser {
    private val banks = mapOf(
        Bank.MELLI to listOf("بانک ملی", "بانك ملي", "bank melli"),
        Bank.MELLAT to listOf("بانک ملت", "بانك ملت", "bank mellat"),
        Bank.SADERAT to listOf("بانک صادرات", "بانك صادرات", "bank saderat")
    )
    private val amount = Regex("""(?:مبلغ|مبلغ تراکنش)\s*[:：]?\s*([0-9][0-9,٬،]*)\s*(ریال|تومان)""")
    private val balance = Regex("""(?:مانده|موجودی)\s*[:：]?\s*([0-9][0-9,٬،]*)\s*(ریال|تومان)""")
    private val date = Regex("""(?<!\d)(1[34]\d{2}/(?:0?[1-9]|1[0-2])/(?:0?[1-9]|[12]\d|3[01]))(?:\s+([01]?\d|2[0-3]):([0-5]\d))?""")
    private val card = Regex("""(?:کارت|شماره کارت)\s*[:：]?\s*([*xX۰-۹0-9-]{4,24})""")
    private val digits = "۰۱۲۳۴۵۶۷۸۹"
    private val arabicDigits = "٠١٢٣٤٥٦٧٨٩"

    fun parse(sender: String, raw: String): ParsedSms? {
        val text = normalize(raw)
        val senderText = normalize(sender).lowercase()
        val matches = banks.filter { (_, tokens) ->
            tokens.any { token -> text.lowercase().contains(normalize(token).lowercase()) ||
                senderText.contains(normalize(token).lowercase()) }
        }.keys
        if (matches.size != 1) return null
        val direction = when {
            listOf("واریز", "بستانکار").any(text::contains) &&
                listOf("برداشت", "بدهکار").none(text::contains) -> Direction.DEPOSIT
            listOf("برداشت", "بدهکار").any(text::contains) &&
                listOf("واریز", "بستانکار").none(text::contains) -> Direction.WITHDRAWAL
            else -> return null
        }
        val amountMatches = amount.findAll(text).toList()
        if (amountMatches.size != 1) return null
        val value = convert(amountMatches.single().groupValues[1], amountMatches.single().groupValues[2]) ?: return null
        if (value <= 0) return null
        val balances = balance.findAll(text).toList()
        if (balances.size > 1) return null
        val remaining = balances.singleOrNull()?.let { convert(it.groupValues[1], it.groupValues[2]) }
        if (balances.isNotEmpty() && remaining == null) return null
        return ParsedSms(
            bank = matches.single(), direction = direction, amountIrr = value,
            balanceIrr = remaining, accountHint = card.find(text)?.groupValues?.get(1),
            rawText = raw, dateText = date.find(text)?.value
        )
    }

    private fun normalize(value: String): String = buildString {
        for (c in value) append(
            when {
                digits.indexOf(c) >= 0 -> ('0'.code + digits.indexOf(c)).toChar()
                arabicDigits.indexOf(c) >= 0 -> ('0'.code + arabicDigits.indexOf(c)).toChar()
                c == 'ي' -> 'ی'
                c == 'ك' -> 'ک'
                else -> c
            }
        )
    }
    private fun convert(number: String, unit: String): Long? = try {
        val n = BigInteger(number.replace(Regex("[,٬،]"), ""))
        val factor = if (unit == "تومان") BigInteger.TEN else BigInteger.ONE
        n.multiply(factor).longValueExact()
    } catch (_: Exception) { null }
}
