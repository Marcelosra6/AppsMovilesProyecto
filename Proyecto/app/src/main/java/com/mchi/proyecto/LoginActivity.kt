package com.mchi.proyecto

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mchi.proyecto.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var usuariosRef: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account: GoogleSignInAccount = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "Error Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        }

        auth = FirebaseAuth.getInstance()
        usuariosRef = FirebaseDatabase.getInstance().getReference("dbProy/idusuario")

        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        
        binding.btnLogin.setOnClickListener { loginNormal() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)



        val especialistas = arrayOf("Selecciona tu nombre", "ROBERT VERGARA", "LESLI ARIAS", "MARÍA SALAZAR")
        binding.spEspecialistaLogin.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            especialistas
        )

        binding.btnIngresarEspecialista.setOnClickListener {
            val seleccion = binding.spEspecialistaLogin.selectedItem?.toString() ?: ""
            if (seleccion.isEmpty() || seleccion == "Selecciona tu nombre") {
                Toast.makeText(this, "Selecciona tu nombre de especialista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getSharedPreferences("rol", Context.MODE_PRIVATE).edit()
                .putString("tipo", "especialista")
                .putString("nombre", seleccion)
                .apply()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun loginNormal() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    getSharedPreferences("rol", Context.MODE_PRIVATE).edit()
                        .putString("tipo", "paciente")
                        .apply()
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    usuariosRef.child(userId).apply {
                        child("email").setValue(acct.email)
                        child("nombre").setValue(acct.displayName)
                    }
                    getSharedPreferences("rol", Context.MODE_PRIVATE).edit()
                        .putString("tipo", "paciente")
                        .apply()
                    Toast.makeText(this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error en autenticación con Google", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
