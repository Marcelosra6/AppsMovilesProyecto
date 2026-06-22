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
import com.google.firebase.database.ValueEventListener
import com.mchi.proyecto.databinding.FragmentAsistenciaBinding

class AsistenciaFragment : Fragment() {

    private var _binding: FragmentAsistenciaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private var especialistaSeleccionado = ""
    private val especialistasList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsistenciaBinding.inflate(inflater, container, false)

        adapter = ChatAdapter()
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        cargarEspecialistas()

        binding.spinnerEspecialistaChat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    especialistaSeleccionado = especialistasList[position - 1]
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

    private fun cargarEspecialistas() {
        FirebaseDatabase.getInstance().getReference("especialistas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    especialistasList.clear()
                    for (child in snapshot.children) {
                        val especialista = child.getValue(Especialista::class.java)
                        if (especialista != null) {
                            val nombre = "${especialista.nombres} ${especialista.apellidos}".trim()
                            if (nombre.isNotEmpty()) especialistasList.add(nombre)
                        }
                    }
                    val items = mutableListOf("Selecciona un especialista")
                    items.addAll(especialistasList)
                    binding.spinnerEspecialistaChat.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        items
                    )
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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
