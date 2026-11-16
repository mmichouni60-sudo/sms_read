package com.smsread.app.sms

import org.junit.Assert.*
import org.junit.Test

class BankSmsParserTest {
    @Test fun parsesMelliDepositInToman() {
        val result = BankSmsParser.parse("BANK", "بانک ملی\nواریز\nمبلغ: ۱۲,۵۰۰ تومان\nمانده: ۴۰,۰۰۰ تومان\n1405/08/25 10:30")
        assertNotNull(result)
        assertEquals(Bank.MELLI, result!!.bank)
        assertEquals(Direction.DEPOSIT, result.direction)
        assertEquals(125000L, result.amountIrr)
        assertEquals(400000L, result.balanceIrr)
    }

    @Test fun parsesMellatWithdrawalInRial() {
        val result = BankSmsParser.parse("BANK", "بانک ملت برداشت مبلغ: 500,000 ریال مانده: 900,000 ریال")
        assertEquals(Bank.MELLAT, result?.bank)
        assertEquals(Direction.WITHDRAWAL, result?.direction)
        assertEquals(500000L, result?.amountIrr)
    }

    @Test fun parsesSaderatDeposit() {
        val result = BankSmsParser.parse("BANK", "بانک صادرات واریز مبلغ: ۷۵۰۰۰ ریال مانده: ۹۵۰۰۰۰ ریال کارت: ****1234")
        assertEquals(Bank.SADERAT, result?.bank)
        assertEquals("****1234", result?.accountHint)
    }

    @Test fun rejectsUnknownUnitAndAmbiguity() {
        assertNull(BankSmsParser.parse("BANK", "بانک ملی برداشت مبلغ: 1000"))
        assertNull(BankSmsParser.parse("BANK", "بانک ملی بانک ملت برداشت مبلغ: 1000 ریال"))
        assertNull(BankSmsParser.parse("BANK", "بانک ملی واریز برداشت مبلغ: 1000 ریال"))
    }
}
