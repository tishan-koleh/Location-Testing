package com.vision.andorid.backgroundlocationyt

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.viewbinding.ViewBinding
import com.google.android.gms.location.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LocationService : Service() {

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var location:Location?=null

    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setIntervalMillis(500).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun createLocationRequest(){
        try {
            fusedLocationProviderClient?.requestLocationUpdates(locationRequest!!,locationCallback!!,null)
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun removeLocationUpdates(){
        locationCallback?.let { fusedLocationProviderClient?.removeLocationUpdates(it) }
        stopSelf()
    }

    private fun onNewLocation(locationResult: LocationResult) {
        location = locationResult.lastLocation
        val latitude = location?.latitude
        val longitude = location?.longitude
        EventBus.getDefault().post(LocationEvent(latitude, longitude))
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createLocationRequest()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeLocationUpdates()
    }



    companion object{
        fun showDialog(context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("GPS")
            builder.setMessage("Please turn on GPS.")
            builder.setPositiveButton("OK") { dialog, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        private fun isGPSEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        fun startLocationService(context: Context){
            if (isGPSEnabled(context)) {
                val intent = Intent(context, LocationService::class.java)
                context.startService(intent)
            }else{
                showDialog(context)
            }
        }
    }
}