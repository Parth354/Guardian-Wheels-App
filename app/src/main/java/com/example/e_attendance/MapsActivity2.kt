package com.example.e_attendance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject

class MapsActivity2 : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private var mMap: GoogleMap? = null
    private var geofencingClient: GeofencingClient? = null
    private var geofenceHelper: GeofenceHelper? = null

    private var GEOFENCE_RADIUS = 200.0 // Adjustable based on user input
    private val GEOFENCE_ID = "1"

    private val FINE_LOCATION_ACCESS_REQUEST_CODE = 10001
    private val BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment: SupportMapFragment? = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val eiffel: LatLng = LatLng(48.8589, 2.29365)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel, 16F))

        enableUserLocation()
        mMap?.setOnMapLongClickListener(this)
    }

    private fun enableUserLocation() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true
        } else {
            val permissionsToRequest = mutableListOf<String>()
            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                FINE_LOCATION_ACCESS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_LOCATION_ACCESS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    mMap?.isMyLocationEnabled = true
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            BACKGROUND_LOCATION_ACCESS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Background location access granted.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Background location access is necessary for geofences.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        if (Build.VERSION.SDK_INT >= 29) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                handleMapLongClick(latLng)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_ACCESS_REQUEST_CODE
                )
            }
        } else {
            handleMapLongClick(latLng)
        }
    }

    private fun handleMapLongClick(latLng: LatLng) {
        mMap?.clear()
        addMarker(latLng)
        addCircle(latLng, GEOFENCE_RADIUS)
        addGeofence(latLng, GEOFENCE_RADIUS)
    }

    private fun addGeofence(latLng: LatLng, radius: Double) {
        val geofence = geofenceHelper?.getGeofence(
            GEOFENCE_ID,
            latLng,
            radius.toFloat(),
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT
        ) ?: return

        val geofencingRequest = geofenceHelper?.getGeofencingRequest(geofence) ?: return
        val pendingIntent = geofenceHelper?.getPendingIntent() ?: return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        geofencingClient?.addGeofences(geofencingRequest, pendingIntent)
            ?.addOnSuccessListener {
                sendGeofenceToBackend(latLng, radius)
                Log.d(TAG, "onSuccess: Geofence Added...")
            }
            ?.addOnFailureListener { e ->
                val errorMessage: String = geofenceHelper?.getErrorString(e) ?: "Unknown error"
                Log.d(TAG, "onFailure: $errorMessage")
            }
    }

    private fun addMarker(latLng: LatLng) {
        val markerOptions: MarkerOptions = MarkerOptions().position(latLng)
        mMap?.addMarker(markerOptions)
    }

    private fun addCircle(latLng: LatLng, radius: Double) {
        val circleOptions: CircleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(radius)
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0))
        circleOptions.fillColor(Color.argb(64, 255, 0, 0))
        circleOptions.strokeWidth(4F)
        mMap?.addCircle(circleOptions)
    }

    private fun sendGeofenceToBackend(latLng: LatLng, radius: Double) {
        val requestBody = JSONObject().apply {
            put("geofenceId", GEOFENCE_ID.toInt())
            put("geofence", JSONObject().apply {
                put("lat", latLng.latitude)
                put("lng", latLng.longitude)
                put("radius", radius)
            })
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "https://guardian-wheels.onrender.com/create-geofence",
            requestBody,
            { response ->
                Log.d(TAG, "Geofence sent to backend: $response")
            },
            { error ->
                Log.e(TAG, "Error sending geofence: ${error.message}")
                error.networkResponse?.let {
                    Log.e(TAG, "Response code: ${it.statusCode}")
                    Log.e(TAG, "Response data: ${String(it.data)}")
                }
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
    companion object {
        private const val TAG = "MapsActivity"
    }
}