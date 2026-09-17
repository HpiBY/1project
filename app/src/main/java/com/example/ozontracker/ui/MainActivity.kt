package com.example.ozontracker.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ozontracker.databinding.ActivityMainBinding
import com.example.ozontracker.notification.NotificationHelper
import com.example.ozontracker.worker.WorkScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ProductViewModel
    private lateinit var adapter: ProductAdapter

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannelIfNeeded(this)
        askNotificationPermissionIfNeeded()

        viewModel = ViewModelProvider(this)[ProductViewModel::class.java]

        adapter = ProductAdapter(onDelete = { viewModel.deleteProduct(it) })
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.products.observe(this) { list ->
            adapter.submitList(list)
            binding.emptyText.visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.addButton.setOnClickListener {
            val url = binding.urlInput.text.toString()
            val isOzonLink = url.contains("ozon.by") || url.contains("ozon.ru")
            if (!isOzonLink) {
                Toast.makeText(this, "Похоже, это не ссылка на Ozon", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addProduct(url)
            binding.urlInput.text?.clear()
        }

        binding.checkNowButton.setOnClickListener {
            WorkScheduler.runNow(this)
            Toast.makeText(this, "Проверка запущена в фоне", Toast.LENGTH_SHORT).show()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        WorkScheduler.schedule(this)
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
