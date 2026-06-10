package com.mchi.proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    // layouts (pasos)
    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var layoutPaso3: LinearLayout

    // paso 1
    private lateinit var spTipoDoc: Spinner
    private lateinit var etNumDoc: TextInputEditText
    private lateinit var etFechaNac: TextInputEditText
    private lateinit var cbTyC: CheckBox
    private lateinit var cbPromo: CheckBox
    private lateinit var btnContinuar: Button

    // paso 2
    private lateinit var etPeso: EditText
    private lateinit var etTalla: EditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var etNombres: TextInputEditText
    private lateinit var etApellidos: TextInputEditText
    private lateinit var etCelular: TextInputEditText
    private lateinit var spGenero: Spinner
    private lateinit var btnContinuar2: Button

    // paso 3
    private lateinit var btnFinalizar: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var usuariosRef: DatabaseReference

    private var usuario = Usuario()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")

        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)
        layoutPaso3 = findViewById(R.id.layoutPaso3)

        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNumDoc = findViewById(R.id.etNumDoc)
        etFechaNac = findViewById(R.id.etFechaNac)
        cbTyC = findViewById(R.id.cbTyC)
        cbPromo = findViewById(R.id.cbPromo)
        btnContinuar = findViewById(R.id.btnContinuar)

        etNombres = findViewById(R.id.etNombres)
        etApellidos = findViewById(R.id.etApellidos)
        etCelular = findViewById(R.id.etCelular)
        etPeso = findViewById(R.id.etPeso)
        etTalla = findViewById(R.id.etTalla)
        spGenero = findViewById(R.id.spGenero)
        btnContinuar2 = findViewById(R.id.btnContinuar2)

        btnFinalizar = findViewById(R.id.btnFinalizar)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        layoutPaso1.visibility = View.VISIBLE
        layoutPaso2.visibility = View.GONE
        layoutPaso3.visibility = View.GONE

        ArrayAdapter.createFromResource(this, R.array.tipos_documento, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spTipoDoc.adapter = it
        }

        ArrayAdapter.createFromResource(this, R.array.generos, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spGenero.adapter = it
        }

        etFechaNac.setOnClickListener { showDatePicker() }
        btnContinuar.setOnClickListener { validarPaso1() }
        btnContinuar2.setOnClickListener { validarPaso2() }
        btnFinalizar.setOnClickListener { guardarEnFirebase() }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            etFechaNac.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validarPaso1() {
        val tipo = spTipoDoc.selectedItem?.toString() ?: ""
        val num = etNumDoc.text?.toString()?.trim() ?: ""
        val fnac = etFechaNac.text?.toString()?.trim() ?: ""

        if (num.isEmpty()) { mostrar("Ingresa el N° de documento"); return }
        if (tipo == "DNI" && num.length != 8) { mostrar("El DNI debe tener 8 dígitos"); return }
        if (fnac.isEmpty()) { mostrar("Selecciona tu fecha de nacimiento"); return }
        if (!cbTyC.isChecked) { mostrar("Debes aceptar los Términos y Condiciones"); return }

        usuario.tipoDoc = tipo
        usuario.numDoc = num
        usuario.fechaNac = fnac
        usuario.aceptaTerminos = cbTyC.isChecked
        usuario.aceptaPromociones = cbPromo.isChecked

        layoutPaso1.visibility = View.GONE
        layoutPaso2.visibility = View.VISIBLE
    }

    private fun validarPaso2() {
        val nombres = etNombres.text?.toString()?.trim() ?: ""
        val apellidos = etApellidos.text?.toString()?.trim() ?: ""
        val celular = etCelular.text?.toString()?.trim() ?: ""
        val genero = spGenero.selectedItem?.toString() ?: ""
        val peso = etPeso.text?.toString()?.trim() ?: ""
        val talla = etTalla.text?.toString()?.trim() ?: ""

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
        layoutPaso2.visibility = View.GONE
        layoutPaso3.visibility = View.VISIBLE
    }

    private fun guardarEnFirebase() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""
        val confirmPassword = etConfirmPassword.text?.toString()?.trim() ?: ""

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