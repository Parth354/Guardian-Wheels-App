package com.example.e_attendance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if(geofencingEvent == null){
            showToast(context, "Geofence not Available...")
        }

        if (geofencingEvent?.hasError() == true) {
            Log.d(TAG, "onReceive: Error receiving geofence event...")
            return
        }

        val geofenceList = geofencingEvent?.triggeringGeofences
        if (geofenceList != null) {
            for (geofence in geofenceList) {
                Log.d(TAG, "onReceive: Geofence triggered: ${geofence.requestId}")
            }

            val transitionType = geofencingEvent.geofenceTransition
            handleGeofenceTransition(context, transitionType, notificationHelper)
        }
    }

    private fun handleGeofenceTransition(
        context: Context,
        transitionType: Int,
        notificationHelper: NotificationHelper
    ) {
        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                notificationHelper.sendHighPriorityNotification(
                    "Car entered the Geofence", "",
                    MapsActivity::class.java
                )
            }

//            Geofence.GEOFENCE_TRANSITION_DWELL -> {
//                showToast(context, "GEOFENCE_TRANSITION_DWELL")
//                notificationHelper.sendHighPriorityNotification(
//                    "GEOFENCE_TRANSITION_DWELL", "",
//                    MapsActivity::class.java
//                )
//            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                notificationHelper.sendHighPriorityNotification(
                    "Car exited the Geofence", "",
                    MapsActivity::class.java
                )
            }

            else -> {
                Log.d(TAG, "onReceive: Unknown transition type: $transitionType")
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }
}
