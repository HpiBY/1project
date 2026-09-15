package com.example.ozontracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ozontracker.data.Product

object NotificationHelper {

    private const val CHANNEL_ID = "price_changes"

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Изменение цены",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления об изменении цены товаров на Ozon"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyPriceChanged(context: Context, product: Product, oldPrice: Long, newPrice: Long) {
        val diff = newPrice - oldPrice
        val direction = if (diff < 0) "снизилась" else "выросла"
        val diffText = if (diff < 0) "-${-diff}" else "+$diff"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Цена $direction: ${product.title}")
            .setContentText("$oldPrice руб. → $newPrice руб. ($diffText руб.)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$oldPrice руб. → $newPrice руб. ($diffText руб.)\n${product.url}"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(product.id.toInt(), notification)
    }
}
