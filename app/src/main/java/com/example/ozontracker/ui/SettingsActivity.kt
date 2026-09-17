package com.example.ozontracker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ozontracker.data.SettingsRepository
import com.example.ozontracker.databinding.ActivitySettingsBinding
import com.example.ozontracker.worker.WorkScheduler

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentInterval = SettingsRepository.getIntervalHours(this)
        val radioId = when (currentInterval) {
            1L -> binding.interval1.id
            2L -> binding.interval2.id
            6L -> binding.interval6.id
            12L -> binding.interval12.id
            24L -> binding.interval24.id
            else -> binding.interval3.id
        }
        binding.intervalGroup.check(radioId)

        binding.saveButton.setOnClickListener {
            val hours = when (binding.intervalGroup.checkedRadioButtonId) {
                binding.interval1.id -> 1L
                binding.interval2.id -> 2L
                binding.interval6.id -> 6L
                binding.interval12.id -> 12L
                binding.interval24.id -> 24L
                else -> 3L
            }
            SettingsRepository.setIntervalHours(this, hours)
            WorkScheduler.schedule(this, hours)
            Toast.makeText(this, "Сохранено: проверка раз в $hours ч.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
