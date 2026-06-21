package com.mchi.proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mchi.proyecto.databinding.ActivityRegisterBinding
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var usuariosRef: DatabaseReference
    private var usuario = Usuario()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")

        binding.layoutPaso1.visibility = View.VISIBLE
        binding.layoutPaso2.visibility = View.GONE
        binding.layoutPaso3.visibility = View.GONE

        ArrayAdapter.createFromResource(this, R.array.tipos_documento, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spTipoDoc.adapter = it
        }

        ArrayAdapter.createFromResource(this, R.array.generos, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spGenero.adapter = it
        }

        binding.etFechaNac.setOnClickListener { showDatePicker() }
        binding.btnContinuar.setOnClickListener { validarPaso1() }
        binding.btnContinuar2.setOnClickListener { validarPaso2() }
        binding.btnFinalizar.setOnClickListener { guardarEnFirebase() }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            binding.etFechaNac.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validarPaso1() {
        val tipo = binding.spTipoDoc.selectedItem?.toString() ?: ""
        val num = binding.etNumDoc.text?.toString()?.trim() ?: ""
        val fnac = binding.etFechaNac.text?.toString()?.trim() ?: ""

        if (num.isEmpty()) { mostrar("Ingresa el N° de documento"); return }
        if (tipo == "DNI" && num.length != 8) { mostrar("El DNI debe tener 8 dígitos"); return }
        if (fnac.isEmpty()) { mostrar("Selecciona tu fecha de nacimiento"); return }
        if (!binding.cbTyC.isChecked) { mostrar("Debes aceptar los Términos y Condiciones"); return }

        usuario.tipoDoc = tipo
        usuario.numDoc = num
        usuario.fechaNac = fnac
        usuario.aceptaTerminos = binding.cbTyC.isChecked
        usuario.aceptaPromociones = binding.cbPromo.isChecked

        binding.layoutPaso1.visibility = View.GONE
        binding.layoutPaso2.visibility = View.VISIBLE
    }

    private fun validarPaso2() {
        val nombres = binding.etNombres.text?.toString()?.trim() ?: ""
        val apellidos = binding.etApellidos.text?.toString()?.trim() ?: ""
        val celular = binding.etCelular.text?.toString()?.trim() ?: ""
        val genero = binding.spGenero.selectedItem?.toString() ?: ""
        val peso = binding.etPeso.text?.toString()?.trim() ?: ""
        val talla = binding.etTalla.text?.toString()?.trim() ?: ""

        if (nombres.isEmpty()) { mostrar("Ingresa tus nombres completos"); return }
        if (apellidos.isEmpty()) { mostrar("Ingresa tus apellidos completos"); return }
        if (!celular.matches("^9\\d{8}$".toRegex())) { mostrar("Ingresa un número de celular válido"); return }
        if (peso.isEmpty()) { mostrar("Ingresa tu peso"); return }
        if (talla.isEmpty()) { mostrar("Ingresa tu talla"); return }

        usuario.nombres = nombres
        usuario.apellidos = apellidos
        usuario.celular = celular
        usuario.genero = genero
        usuario.peso = peso.toInt()
        usuario.talla = talla.toInt()

        mostrar("Paso 2 OK")
        binding.layoutPaso2.visibility = View.GONE
        binding.layoutPaso3.visibility = View.VISIBLE
    }

    private fun guardarEnFirebase() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString()?.trim() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString()?.trim() ?: ""

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrar("Ingresa un correo electrónico válido"); return
        }
        if (password.length < 8 || !password.matches(".*[A-Z].*".toRegex())
            || !password.matches(".*\\d.*".toRegex())
            || !password.matches(".*[!@#\$%^&].*".toRegex())) {
            mostrar("La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial"); return
        }
        if (password != confirmPassword) { mostrar("Las contraseñas no coinciden"); return }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    usuariosRef.child(userId).setValue(usuario)
                        .addOnSuccessListener {
                            mostrar("Usuario registrado exitosamente")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e -> mostrar("Error: ${e.message}") }
                } else {
                    mostrar("Error al crear el usuario: ${task.exception?.message}")
                }
            }
    }

    private fun mostrar(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}
