package com.smsread.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.smsread.app.data.SmsDatabase
import com.smsread.app.data.SmsEvent
import com.smsread.app.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * Prototype: bounded receiver work persists each received SMS before queuing network work.
 * No raw SMS is logged. For production, add encrypted storage, robust multipart grouping,
 * durable crash recovery and instrumentation tests on target devices.
 */
class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val raw = messages.joinToString("") { it.messageBody.orEmpty() }
        val sender = messages.first().originatingAddress.orEmpty()
        val receivedAt = messages.minOf { it.timestampMillis }
        val parsed = BankSmsParser.parse(sender, raw)
        val pending = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                runBlocking {
                    withTimeout(8000L) {
                        withContext(Dispatchers.IO) {
                            SmsDatabase.get(appContext).events().insert(
                                SmsEvent(
                                    eventId = UUID.randomUUID().toString(),
                                    sender = sender,
                                    rawText = raw,
                                    receivedAtMillis = receivedAt,
                                    bankCode = parsed?.bank?.name,
                                    direction = parsed?.direction?.name,
                                    amountIrr = parsed?.amountIrr,
                                    balanceIrr = parsed?.balanceIrr,
                                    accountHint = parsed?.accountHint,
                                    dateText = parsed?.dateText,
                                    parseStatus = if (parsed == null) "REVIEW" else "PARSED"
                                )
                            )
                        }
                    }
                }
                SyncWorker.enqueue(appContext)
            } catch (e: Exception) {
                Log.e("BankSmsReceiver", "Local persistence failed; event may require recovery", e)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
