package com.mchi.proyecto

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.mchi.proyecto.databinding.FragmentAsistenciaBinding

class AsistenciaFragment : Fragment() {

    private var _binding: FragmentAsistenciaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private var especialistaSeleccionado = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsistenciaBinding.inflate(inflater, container, false)

        val especialistas = arrayOf("Selecciona un especialista", "ROBERT VERGARA", "LESLI ARIAS", "MARÍA SALAZAR")
        binding.spinnerEspecialistaChat.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            especialistas
        )

        adapter = ChatAdapter()
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        binding.spinnerEspecialistaChat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    especialistaSeleccionado = especialistas[position]
                    cargarMensajes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnSend.setOnClickListener { sendMessage() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getChatRef() = FirebaseDatabase.getInstance()
        .getReference("chats")
        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "anonimo")
        .child(especialistaSeleccionado)
        .child("messages")

    private fun cargarMensajes() {
        adapter.clearMessages()
        getChatRef().addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
                val msg = snapshot.getValue(Message::class.java)
                if (msg != null) adapter.addMessage(msg)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (TextUtils.isEmpty(text)) return
        if (especialistaSeleccionado.isEmpty()) {
            Toast.makeText(context, "Selecciona un especialista", Toast.LENGTH_SHORT).show()
            return
        }

        val msg = Message(text, "user", System.currentTimeMillis())
        getChatRef().push().setValue(msg)
        binding.etMessage.setText("")
    }
}
