package com.mchi.proyecto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mchi.proyecto.api.RetrofitClient
import com.mchi.proyecto.databinding.FragmentInicioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)

        binding.btnGuardarFlexiones.setOnClickListener { guardarFlexionesHoy() }

        cargarNombreUsuario()
        actualizarTotales()
        cargarConsejoDelDia()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun cargarNombreUsuario() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("usuarios").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val nombres = dataSnapshot.child("nombres").getValue(String::class.java)
                        val apellidos = dataSnapshot.child("apellidos").getValue(String::class.java)
                        binding.txtHola.text = "¡Hola, $nombres $apellidos!"
                    } else {
                        binding.txtHola.text = "¡Hola, Usuario!"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun guardarFlexionesHoy() {
        val input = binding.etFlexionesHoy.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(context, "Ingresa el número de flexiones", Toast.LENGTH_SHORT).show()
            return
        }
        val flexionesHoy = input.toIntOrNull()
        if (flexionesHoy == null || flexionesHoy <= 0) {
            Toast.makeText(context, "Ingresa un número válido", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val flexionesRef = FirebaseDatabase.getInstance()
            .getReference("usuarios").child(userId).child("progreso").child("flexiones")

        flexionesRef.child(fechaHoy).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existentes = snapshot.getValue(Int::class.java) ?: 0
                flexionesRef.child(fechaHoy).setValue(existentes + flexionesHoy)
                    .addOnSuccessListener {
                        Toast.makeText(context, "¡$flexionesHoy flexiones registradas!", Toast.LENGTH_SHORT).show()
                        binding.etFlexionesHoy.text.clear()
                        actualizarTotales()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun cargarConsejoDelDia() {
        binding.txtConsejo.text = "Cargando consejo..."
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getHealthTip()
                }
                binding.txtConsejo.text = "\"${response.slip.advice}\""
            } catch (e: Exception) {
                binding.txtConsejo.text = "Consejo no disponible: ${e.localizedMessage}"
            }
        }
    }

    private fun actualizarTotales() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("usuarios").child(userId).child("progreso").child("flexiones")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0
                    for (child in snapshot.children) {
                        total += child.getValue(Int::class.java) ?: 0
                    }
                    binding.txtContadorFlexiones.text = "Flexiones totales: $total"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
