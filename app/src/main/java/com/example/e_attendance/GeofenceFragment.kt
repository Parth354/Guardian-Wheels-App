package com.example.e_attendance

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Button // or any other UI element you use to trigger the navigation

class GeofenceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_geofence, container, false)

        val buttonNavigateToMap: Button = view.findViewById(R.id.button_navigate_to_map)
        buttonNavigateToMap.setOnClickListener {
            navigateToMapsActivity()
        }

        return view
    }

    private fun navigateToMapsActivity() {
        val intent = Intent(activity, MapsActivity2::class.java)
        startActivity(intent)
    }
}
