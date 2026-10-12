package com.huamani.mitiemposemanal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.huamani.mitiemposemanal.adapter.MainPagerAdapter
import com.huamani.mitiemposemanal.databinding.ActivityMainBinding
import com.huamani.mitiemposemanal.db.DatabaseHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Encabezado con información del estudiante y versión exactas
        binding.tvHeaderStudentInfo.text = getString(R.string.header_student_info)

        // Configuración de ViewPager2 y TabLayout con íconos
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.tab_tareas)
                    tab.setIcon(R.drawable.ic_task)
                }
                1 -> {
                    tab.text = getString(R.string.tab_categorias)
                    tab.setIcon(R.drawable.ic_category)
                }
            }
        }.attach()

        // Sincronización de eventos entre fragmentos y actualización de contadores
        pagerAdapter.categoriasFragment.onCategoriasChangedListener = {
            pagerAdapter.tareasFragment.loadTareas()
            updateHeaderCounters()
        }

        updateHeaderCounters()
    }

    fun updateHeaderCounters() {
        val totalTareas = dbHelper.getAllTareas().size
        val totalCategorias = dbHelper.getAllCategorias().size
        binding.tvCountTareas.text = totalTareas.toString()
        binding.tvCountCategorias.text = totalCategorias.toString()
    }
}
