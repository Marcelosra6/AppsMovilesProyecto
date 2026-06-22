package com.mchi.proyecto

import android.app.AlertDialog
import android.content.Context
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mchi.proyecto.databinding.FragmentMisCitasBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MisCitasFragment : Fragment() {

    private var _binding: FragmentMisCitasBinding? = null
    private val binding get() = _binding!!

    private var mostrandoProximas = true
    private var esEspecialista = false
    private var nombreEspecialista = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisCitasBinding.inflate(inflater, container, false)

        val prefs = requireContext().getSharedPreferences("rol", Context.MODE_PRIVATE)
        esEspecialista = prefs.getString("tipo", "paciente") == "especialista"
        nombreEspecialista = prefs.getString("nombre", "") ?: ""

        if (esEspecialista) {
            binding.btnAgendarCita.visibility = View.GONE
        } else {
            binding.btnAgendarCita.setOnClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, AgendarCitaFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        mostrarCitas(true)

        binding.btnProximas.setOnClickListener {
            mostrandoProximas = true
            mostrarCitas(true)
        }

        binding.btnHistorial.setOnClickListener {
            mostrandoProximas = false
            mostrarCitas(false)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarCitas(mostrarProximas: Boolean) {
        binding.contenedorCitas.removeAllViews()

        val ref = if (esEspecialista) {
            FirebaseDatabase.getInstance().getReference("citas")
        } else {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseDatabase.getInstance().getReference("citas").child(userId)
        }

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    mostrarMensaje("No tienes citas registradas aún")
                    return
                }

                var hayCitas = false
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val ahora = sdf.parse(sdf.format(Date())) ?: Date()


                val citas = mutableListOf<Cita>()

                if (esEspecialista) {
                    for (userCitas in snapshot.children) {
                        val userKey = userCitas.key ?: continue
                        for (citaSnapshot in userCitas.children) {
                            val esp = citaSnapshot.child("especialista").getValue(String::class.java) ?: continue
                            if (esp == nombreEspecialista) {
                                val descripcion = citaSnapshot.child("descripcion").getValue(String::class.java) ?: ""
                                val estado = citaSnapshot.child("estado").getValue(String::class.java) ?: ""
                                val fecha = citaSnapshot.child("fecha").getValue(String::class.java) ?: ""
                                val nombrePaciente = citaSnapshot.child("nombreUsuario").getValue(String::class.java) ?: ""
                                val citaKey = citaSnapshot.key ?: continue
                                citas.add(Cita(
                                    especialista = esp,
                                    fecha = fecha,
                                    descripcion = descripcion,
                                    estado = estado,
                                    paciente = nombrePaciente,
                                    refPath = "citas/$userKey/$citaKey"
                                ))

                            }
                        }
                    }
                } else {
                    for (citaSnapshot in snapshot.children) {
                        val especialista = citaSnapshot.child("especialista").getValue(String::class.java) ?: continue
                        val descripcion = citaSnapshot.child("descripcion").getValue(String::class.java) ?: ""
                        val estado = citaSnapshot.child("estado").getValue(String::class.java) ?: ""
                        val fecha = citaSnapshot.child("fecha").getValue(String::class.java) ?: ""
                        citas.add(Cita(
                            especialista = especialista,
                            fecha = fecha,
                            descripcion = descripcion,
                            estado = estado
                        ))
                    }
                }

                for (cita in citas) {
                    try {
                        val fechaCita: Date = sdf.parse(cita.fecha) ?: continue
                        val estadoFinalizado = cita.estado == "Completada" || cita.estado == "No asistió"
                        val esFutura = fechaCita.time >= ahora.time && !estadoFinalizado

                        if ((mostrarProximas && esFutura) || (!mostrarProximas && !esFutura)) {
                            hayCitas = true
                            agregarCitaVista(cita)
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

    private fun agregarCitaVista(cita: Cita) {
        TextView(requireContext()).apply {
            val header = if (cita.paciente.isNotEmpty()) "Paciente: ${cita.paciente}\n" else ""
            text = "${header}Especialista: ${cita.especialista}\n" +
                    "Fecha: ${cita.fecha}\n" +
                    "${cita.descripcion}\n" +
                    "Estado: ${cita.estado}"
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.black, null))
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 25) }

            if (esEspecialista && cita.refPath.isNotEmpty()) {
                setOnClickListener {
                    val opciones = arrayOf("Sí", "No vino", "Cancelar")
                    AlertDialog.Builder(requireContext())
                        .setTitle("¿Se realizó la cita?")
                        .setItems(opciones) { _, which ->
                            when (which) {
                                0 -> FirebaseDatabase.getInstance().getReference(cita.refPath)
                                    .child("estado").setValue("Completada")
                                1 -> FirebaseDatabase.getInstance().getReference(cita.refPath)
                                    .child("estado").setValue("No asistió")
                            }
                            mostrarCitas(mostrandoProximas)
                        }
                        .show()
                }
            }
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