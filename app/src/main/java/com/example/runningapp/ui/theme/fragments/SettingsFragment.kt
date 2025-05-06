package com.example.runningapp.ui.theme.fragments

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentSettingsBinding
import com.example.runningapp.other.Constants.KEY_NAME
import com.example.runningapp.other.Constants.KEY_WEIGHT
import com.example.runningapp.other.Constants.SHARED_PREFERENCES_NAME
import com.example.runningapp.ui.theme.MainActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings){

    private lateinit var bindingSettings: FragmentSettingsBinding
    private lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingSettings = FragmentSettingsBinding.bind(view)
        sharedPref = requireContext().getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)

        loadFieldsFromSharedPref()
        bindingSettings.btnApplyChanges.setOnClickListener {
            val success = applyChangesToSharedPref()
            if (success) {
                Snackbar.make(view, "Saved changes!", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(view, "Please enter all the fields", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun loadFieldsFromSharedPref() {
        val name = sharedPref.getString(KEY_NAME, "")
        val weight = sharedPref.getFloat(KEY_WEIGHT, 65f)
        bindingSettings.etName.setText(name)
        bindingSettings.etWeight.setText(weight.toString())
    }

    private fun applyChangesToSharedPref(): Boolean {
        val nameText = bindingSettings.etName.text.toString()
        val weightText = bindingSettings.etWeight.text.toString()
        if (nameText.isEmpty() || weightText.isEmpty()) {
            return false
        }
        sharedPref.edit()
            .putString(KEY_NAME, nameText)
            .putFloat(KEY_WEIGHT, weightText.toFloat())
            .apply()
        val toolbarText = "Welcome, $nameText!"
        (requireActivity() as MainActivity).updateToolbarTitle(toolbarText)
        return true
    }
}