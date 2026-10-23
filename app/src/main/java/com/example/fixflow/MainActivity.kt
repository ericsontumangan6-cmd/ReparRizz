package com.example.fixflow

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fixflow.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null) {
            binding.bottomNav.setupWithNavController(navController)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.nav_repair_detail) {
                    binding.bottomNav.visibility = View.GONE
                } else {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }
}
