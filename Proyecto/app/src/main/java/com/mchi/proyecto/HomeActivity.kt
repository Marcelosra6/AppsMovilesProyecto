package com.mchi.proyecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.mchi.proyecto.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.navigationBar.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, InicioFragment())
                .commit()
        }

        binding.imgLogout.setOnClickListener { cerrarSesion() }
        binding.navInicio.setOnClickListener { openFragment(InicioFragment()) }
        binding.navCitas.setOnClickListener { openFragment(MisCitasFragment()) }
        binding.navAsistencia.setOnClickListener {
            val tipo = getSharedPreferences("rol", Context.MODE_PRIVATE).getString("tipo", "paciente")
            if (tipo == "especialista") {
                openFragment(EspecialistaFragment())
            } else {
                openFragment(AsistenciaFragment())
            }
        }
    }

    private fun cerrarSesion() {
        getSharedPreferences("rol", Context.MODE_PRIVATE).edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
