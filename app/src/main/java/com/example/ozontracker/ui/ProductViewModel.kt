package com.example.ozontracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.ozontracker.data.AppDatabase
import com.example.ozontracker.data.Product
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).productDao()

    val products: LiveData<List<Product>> = dao.observeAll()

    fun addProduct(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            dao.insert(Product(url = trimmed, title = shortTitleFromUrl(trimmed)))
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { dao.delete(product) }
    }

    private fun shortTitleFromUrl(url: String): String {
        return try {
            val path = url.substringAfter("/product/").substringBefore("/")
            val withoutId = path.substringBeforeLast('-').replace('-', ' ')
            withoutId.ifBlank { "Товар Ozon" }
        } catch (e: Exception) {
            "Товар Ozon"
        }
    }
}
