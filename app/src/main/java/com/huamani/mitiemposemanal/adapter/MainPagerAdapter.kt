package com.huamani.mitiemposemanal.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.huamani.mitiemposemanal.ui.CategoriasFragment
import com.huamani.mitiemposemanal.ui.TareasFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    val tareasFragment = TareasFragment()
    val categoriasFragment = CategoriasFragment()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> tareasFragment
            1 -> categoriasFragment
            else -> tareasFragment
        }
    }
}
