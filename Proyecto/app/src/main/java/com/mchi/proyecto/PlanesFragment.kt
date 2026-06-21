package com.mchi.proyecto

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mchi.proyecto.databinding.FragmentPlanesBinding

class PlanesFragment : Fragment() {

    private var _binding: FragmentPlanesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanesBinding.inflate(inflater, container, false)

        binding.planEstandarInfo.visibility = View.VISIBLE

        binding.btnEstandar.setOnClickListener {
            val intent = Intent(activity, PaymentActivity::class.java)
            intent.putExtra("plan", "estandar")
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
