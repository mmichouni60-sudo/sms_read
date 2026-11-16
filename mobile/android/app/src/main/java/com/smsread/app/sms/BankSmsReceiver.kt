package com.smsread.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * Prototype receiver: captures broadcasts and passes messages to a short-lived handler.
 * TODO: replace with atomic Room persistence + outbox before production.
 * Never log raw SMS content.
 */
class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        // A multipart SMS is delivered as multiple PDUs. Join before parsing.
        val raw = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val sender = messages.first().originatingAddress.orEmpty()
        val parsed = BankSmsParser.parse(sender, raw)
        if (parsed != null) {
            // Intentionally not uploading or persisting until Room outbox is implemented.
            Log.d("BankSmsReceiver", "Recognized bank transaction; persistence pending")
        }
    }
}
