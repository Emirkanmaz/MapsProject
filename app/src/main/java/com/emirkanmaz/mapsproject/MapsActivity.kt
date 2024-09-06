package com.emirkanmaz.mapsproject

import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.emirkanmaz.mapsproject.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var hasLocation: Boolean = false
    private var userLocationCircle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val newLocation = LatLng(location.latitude, location.longitude)
                updateLocation(newLocation)
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                accessLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(findViewById(android.R.id.content), "Need Location Permission.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission") {
                        requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
            }
            else -> {
                requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            accessLocation()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Location Permission Denied.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun accessLocation() {
        Snackbar.make(findViewById(android.R.id.content), "Accessing location.", Snackbar.LENGTH_SHORT).show()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                updateLocation(LatLng(it.latitude, it.longitude))
            }
        }
    }

    private fun updateLocation(location: LatLng) {
        if (!hasLocation) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
            hasLocation = true
        }

        userLocationCircle?.remove()
        userLocationCircle = mMap.addCircle(
            CircleOptions()
                .center(location)
                .radius(40.0)
                .strokeColor(Color.WHITE)
                .fillColor(Color.BLUE)
                .strokeWidth(10f)
        )
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()

        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                geocoder.getFromLocation(p0.latitude, p0.longitude, 1, Geocoder.GeocodeListener { addressList ->
                    val address = addressList.firstOrNull()
                    address?.let {
                        runOnUiThread {
                            mMap.addMarker(MarkerOptions().position(p0).title("${it.thoroughfare} ${it.subLocality} "))
                            println(it.countryName.toString())
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val addressList = geocoder.getFromLocation(p0.latitude, p0.longitude, 1)
                val address = addressList?.firstOrNull()
                address?.let {
                    mMap.addMarker(MarkerOptions().position(p0).title("${it.thoroughfare} ${it.subLocality} "))
                    println(it.countryName.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
