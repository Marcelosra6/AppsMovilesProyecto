package com.mchi.proyecto

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.mchi.proyecto.databinding.FragmentAgendarCitaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendarCitaFragment : Fragment() {

    private var _binding: FragmentAgendarCitaBinding? = null
    private val binding get() = _binding!!

    private var nombreCompletoUsuario = ""
    private var fechaSeleccionada = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendarCitaBinding.inflate(inflater, container, false)

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseDatabase.getInstance().getReference("usuarios").child(user.uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val nombres = snapshot.child("nombres").getValue(String::class.java) ?: ""
                        val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                        nombreCompletoUsuario = "$nombres $apellidos".trim()
                        binding.tvNombreUsuario.text = nombreCompletoUsuario
                    } else {
                        binding.tvNombreUsuario.text = "Datos no encontrados"
                    }
                }
                .addOnFailureListener {
                    binding.tvNombreUsuario.text = "Error al cargar datos"
                }
        } else {
            binding.tvNombreUsuario.text = "No hay usuario activo"
        }

        val especialistas = arrayOf("ROBERT VERGARA", "LESLI ARIAS", "MARÍA SALAZAR")
        binding.spinnerEspecialistas.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            especialistas
        )

        binding.etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString().trim()
                val palabras = if (texto.isEmpty()) 0 else texto.split("\\s+".toRegex()).size
                binding.tvContadorPalabras.text = "$palabras / 50 palabras"
                if (palabras > 50) binding.etDescripcion.error = "Máximo 50 palabras"
            }
        })

        binding.etFechaCita.setOnClickListener { showDatePicker() }
        binding.etFechaCita.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePicker()
        }

        binding.btnReservarCita.setOnClickListener {
            if (user == null) {
                Toast.makeText(requireContext(), "Debes iniciar sesión.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val especialista = binding.spinnerEspecialistas.selectedItem.toString()
            val descripcion = binding.etDescripcion.text.toString().trim()

            if (descripcion.isEmpty()) {
                binding.etDescripcion.error = "Describe tu situación"
                return@setOnClickListener
            }

            if (fechaSeleccionada.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaReserva = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val citaRef = FirebaseDatabase.getInstance()
                .getReference("citas").child(user.uid).push()

            val citaData = mapOf(
                "nombreUsuario" to nombreCompletoUsuario,
                "especialista" to especialista,
                "descripcion" to descripcion,
                "fechaReserva" to fechaReserva,
                "fecha" to fechaSeleccionada,
                "estado" to "Pendiente"
            )

            citaRef.setValue(citaData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Cita reservada con $especialista", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, MisCitasFragment())
                        .addToBackStack(null)
                        .commit()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar cita", Toast.LENGTH_SHORT).show()
                }
        }

        return binding.root
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            binding.etFechaCita.setText(fechaSeleccionada)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
