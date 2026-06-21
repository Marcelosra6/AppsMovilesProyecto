package com.mchi.proyecto

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.mchi.proyecto.databinding.FragmentEspecialistaBinding

class EspecialistaFragment : Fragment() {

    private var _binding: FragmentEspecialistaBinding? = null
    private val binding get() = _binding!!

    private var pacienteSeleccionado = ""
    private lateinit var adapter: ChatAdapter
    private val pacientes = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEspecialistaBinding.inflate(inflater, container, false)

        adapter = ChatAdapter()
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        cargarPacientes()

        binding.btnSend.setOnClickListener { sendMessage() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getNombreEspecialista(): String {
        return requireContext().getSharedPreferences("rol", Context.MODE_PRIVATE)
            .getString("nombre", "Especialista") ?: "Especialista"
    }

    private fun cargarPacientes() {
        FirebaseDatabase.getInstance().getReference("chats")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
                    val pacienteId = snapshot.key ?: return
                    if (!pacientes.contains(pacienteId)) {
                        pacientes.add(pacienteId)
                        agregarPacienteVista(pacienteId)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun agregarPacienteVista(pacienteId: String) {
        val tv = TextView(requireContext()).apply {
            text = "Paciente: $pacienteId"
            textSize = 16f
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
            setOnClickListener {
                pacienteSeleccionado = pacienteId
                binding.chatLayout.visibility = View.VISIBLE
                binding.recyclerPacientes.visibility = View.GONE
                binding.tvChatCon.text = "Chat con paciente: $pacienteId"
                cargarMensajesPaciente()
            }
        }
        binding.recyclerPacientes.addView(tv)
    }

    private fun cargarMensajesPaciente() {
        adapter.clearMessages()
        val especialista = getNombreEspecialista()
        FirebaseDatabase.getInstance()
            .getReference("chats").child(pacienteSeleccionado).child(especialista).child("messages")
            .addChildEventListener(object : ChildEventListener {
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
        if (pacienteSeleccionado.isEmpty()) return

        val msg = Message(text, getNombreEspecialista(), System.currentTimeMillis())
        val especialista = getNombreEspecialista()
        FirebaseDatabase.getInstance()
            .getReference("chats").child(pacienteSeleccionado).child(especialista).child("messages")
            .push().setValue(msg)
        binding.etMessage.setText("")
    }
}
