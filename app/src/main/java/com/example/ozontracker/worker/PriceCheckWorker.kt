package com.example.ozontracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ozontracker.data.AppDatabase
import com.example.ozontracker.notification.NotificationHelper

class PriceCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).productDao()
        val products = dao.getAll()

        for (product in products) {
            val outcome = PriceExtractor.fetchPrice(applicationContext, product.url)
            when (outcome) {
                is PriceExtractor.Result.Success -> {
                    val newPrice = outcome.priceRub
                    val oldPrice = product.lastPrice

                    if (oldPrice != null && oldPrice != newPrice) {
                        NotificationHelper.notifyPriceChanged(
                            applicationContext, product, oldPrice, newPrice
                        )
                    }

                    product.lastPrice = newPrice
                    product.lastCheckedAt = System.currentTimeMillis()
                    product.lastError = null
                    dao.update(product)
                }
                is PriceExtractor.Result.Error -> {
                    product.lastCheckedAt = System.currentTimeMillis()
                    product.lastError = outcome.message
                    dao.update(product)
                }
            }
        }

        return Result.success()
    }
}
