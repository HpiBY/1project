package com.example.ozontracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ozontracker.data.Product
import com.example.ozontracker.databinding.ItemProductBinding
import java.text.SimpleDateFormat
import java.util.*

class ProductAdapter(
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = getItem(position)
        val b = holder.binding

        b.titleText.text = product.title
        b.urlText.text = product.url

        b.priceText.text = product.lastPrice?.let { "$it руб." } ?: "цена ещё не проверена"

        if (product.lastError != null) {
            b.statusText.text = "Ошибка: ${product.lastError}"
            b.statusText.visibility = android.view.View.VISIBLE
        } else if (product.lastCheckedAt > 0) {
            val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            b.statusText.text = "Проверено: ${fmt.format(Date(product.lastCheckedAt))}"
            b.statusText.visibility = android.view.View.VISIBLE
        } else {
            b.statusText.visibility = android.view.View.GONE
        }

        b.deleteButton.setOnClickListener { onDelete(product) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
        }
    }
}
