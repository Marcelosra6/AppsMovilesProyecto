package com.mchi.proyecto

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mchi.proyecto.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnProcessPayment.setOnClickListener { procesarPago() }
    }

    private fun procesarPago() {
        val cardNumber = binding.edtCardNumber.text.toString().trim()
        val expirationDate = binding.edtExpirationDate.text.toString().trim()
        val cvv = binding.edtCVV.text.toString().trim()

        if (cardNumber.isEmpty() || expirationDate.isEmpty() || cvv.isEmpty()) {
            binding.txtPaymentResult.text = "Por favor, complete todos los campos."
            return
        }
        if (validarTarjeta(cardNumber, expirationDate, cvv)) {
            binding.txtPaymentResult.text = "Pago procesado con éxito."
            binding.txtPaymentResult.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            Handler(Looper.getMainLooper()).postDelayed({ actualizarPlanEnFirebase() }, 2000)
        } else {
            binding.txtPaymentResult.text = "Datos de tarjeta inválidos."
            binding.txtPaymentResult.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        }
    }

    private fun validarTarjeta(cardNumber: String, expirationDate: String, cvv: String): Boolean {
        return cardNumber.length == 16 && expirationDate.length == 5 && cvv.length == 3
    }

    private fun actualizarPlanEnFirebase() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val database: DatabaseReference = FirebaseDatabase.getInstance()
                .getReference("usuarios").child(userId)
            database.child("plan").setValue("Estandar").addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Plan Estándar asignado con éxito!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al asignar el plan. Intenta nuevamente.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
