package com.mchi.proyecto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mchi.proyecto.databinding.FragmentMisCitasBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MisCitasFragment : Fragment() {

    private var _binding: FragmentMisCitasBinding? = null
    private val binding get() = _binding!!

    private lateinit var citasRef: DatabaseReference
    private lateinit var userId: String
    private var mostrandoProximas = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisCitasBinding.inflate(inflater, container, false)

        userId = FirebaseAuth.getInstance().currentUser!!.uid
        citasRef = FirebaseDatabase.getInstance().getReference("citas").child(userId)

        mostrarCitas(true)

        binding.btnProximas.setOnClickListener {
            mostrandoProximas = true
            mostrarCitas(true)
        }

        binding.btnHistorial.setOnClickListener {
            mostrandoProximas = false
            mostrarCitas(false)
        }

        binding.btnAgendarCita.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AgendarCitaFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarCitas(mostrarProximas: Boolean) {
        binding.contenedorCitas.removeAllViews()

        citasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    mostrarMensaje("No tienes citas registradas aún")
                    return
                }

                var hayCitas = false
                val ahora = System.currentTimeMillis()

                for (citaSnapshot in snapshot.children) {
                    val especialista = citaSnapshot.child("especialista").getValue(String::class.java) ?: continue
                    val descripcion = citaSnapshot.child("descripcion").getValue(String::class.java)
                    val estado = citaSnapshot.child("estado").getValue(String::class.java)
                    val fechaReserva = citaSnapshot.child("fechaReserva").getValue(String::class.java) ?: continue

                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val fechaCita: Date = sdf.parse(fechaReserva) ?: continue
                        val esFutura = fechaCita.time >= ahora

                        if ((mostrarProximas && esFutura) || (!mostrarProximas && !esFutura)) {
                            hayCitas = true
                            agregarCitaVista(especialista, fechaReserva, descripcion, estado)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!hayCitas) {
                    mostrarMensaje(
                        if (mostrarProximas) "No tienes próximas citas"
                        else "No tienes historial de citas"
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                mostrarMensaje("Error al cargar citas")
            }
        })
    }

    private fun agregarCitaVista(especialista: String, fecha: String, descripcion: String?, estado: String?) {
        TextView(requireContext()).apply {
            text = "️Especialista: $especialista\n" +
                    "Fecha: $fecha\n" +
                    "$descripcion\n" +
                    "Estado: $estado"
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.black, null))
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 25) }
        }.also { binding.contenedorCitas.addView(it) }
    }

    private fun mostrarMensaje(mensaje: String) {
        binding.contenedorCitas.removeAllViews()
        TextView(requireContext()).apply {
            text = mensaje
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(24, 24, 24, 24)
        }.also { binding.contenedorCitas.addView(it) }
    }
}