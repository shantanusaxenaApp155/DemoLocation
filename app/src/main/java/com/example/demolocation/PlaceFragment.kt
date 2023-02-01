package com.example.demolocation

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.demolocation.databinding.FragmentPlaceBinding
import com.example.demolocation.viewmodel.MainViewModel
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest.*
import com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import java.io.IOException
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit


class PlaceFragment: Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentPlaceBinding
    private var markerOptions: MarkerOptions? = null
    private var googleMap: GoogleMap? = null
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: MainViewModel
    val PERMISSION_ID = 200
    private var currentLocation: Location? = null
    private var mCurrLocationMarker: Marker?= null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaceBinding.inflate(layoutInflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        //initPlaceApi()
        setUpView()

        val apikey = getString(R.string.api_key)
        viewModel.apiKey = apikey

        Places.initialize(requireActivity().applicationContext, apikey)
        initPlaceApi()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getLastLocation()
    }


    private fun initPlaceApi(){
        //val apikey = getString(R.string.api_key)
        //val token = AutocompleteSessionToken.newInstance()
        Places.initialize(requireActivity().applicationContext, viewModel.apiKey)
        val placesClient = Places.createClient(this.requireActivity())
        viewModel.initClient()
        placesClient.findAutocompletePredictions(viewModel.request).addOnSuccessListener {
            var text = StringBuilder()
            for (pre in it.autocompletePredictions )
                text.append(" ").append(pre.getFullText(null))
            Log.v("ddtv","PF success: "+it.toString())
        }.addOnFailureListener {
            Log.v("ddtv","PF error: "+it.toString())
        }

       /* val rectangularBounds = RectangularBounds.newInstance(LatLng(-33.880490, 151.184363),
            LatLng(-33.858754, 151.229596))
        val request = FindAutocompletePredictionsRequest.builder()
            .setLocationBias(rectangularBounds)
            .setCountry("in")
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery("ind")
            .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener {
            var text = StringBuilder()
            for (pre in it.autocompletePredictions )
            text.append(" ").append(pre.getFullText(null))
            Log.v("ddtv","PF success: "+it.toString())
        }.addOnFailureListener {
            Log.v("ddtv","PF error: "+it.toString())
        }*/
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.getMapAsync(this)
        binding.mapView.onResume()
    }

    private fun setUpView(){
        binding.tvConfirmLocation.text = "Fetching Location"
        binding.btnConfirmLocation.setOnClickListener {
            //TODO move to next screen
        }

        binding.idSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.v("ddtv","on onQueryTextSubmit: "+query.toString())
                val location = binding.idSearchView.getQuery().toString()
                var addressList: List<Address>? = null
                if (location != null || location == "") {
                    val geocoder = Geocoder(requireActivity())
                    try {
                        addressList = geocoder.getFromLocationName(location, 1)
                        Log.v("ddtv","on onQueryTextSubmit 22: "+addressList.toString())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    
                    if (addressList!=null){
                    val address: Address = addressList!![0]
                    val latLng = LatLng(address.getLatitude(), address.getLongitude())

                        if (googleMap!=null){
                            googleMap?.addMarker(MarkerOptions().position(latLng).title(location))
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                        }
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("PotentialBehaviorOverride")
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onMapReady(mMap: GoogleMap) {
        googleMap = mMap
        googleMap!!.setOnMapClickListener {
            markerOptions= MarkerOptions()
            markerOptions?.position(it)
            markerOptions?.title(it.latitude.toString())
            googleMap!!.clear()
            googleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 10f))
            googleMap!!.addMarker(markerOptions!!)
        }
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        googleMap!!.isMyLocationEnabled = true
        googleMap!!.uiSettings.isMyLocationButtonEnabled = false
        //setMarkerOnCurrentLocation()
      /*]]  mMap.setOnMarkerClickListener { marker ->
           *//* anchorBehavior!!.state = (AnchorSheetBehavior.STATE_ANCHOR)
            mBusinessList!!.clear()
            mBusinessList!!.add(businessHashMap!![marker])
            mPlacesBusinessAdapter!!.notifyDataSetChanged()
            false*//*
        }
        mMap.setOnMapClickListener {
            mBusinessList!!.clear()
            mBusinessList!!.addAll((mPermanentBusinessList)!!)
            mPlacesBusinessAdapter!!.notifyDataSetChanged()
        }*/
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        Log.v("ddtv","on Success CompList: "+location.latitude.toString()+" , "+location.longitude.toString())
                        if (mCurrLocationMarker!=null){
                            mCurrLocationMarker!!.remove()
                        }

                        var latLong = LatLng(location.latitude,location.longitude)
                        markerOptions = MarkerOptions()
                        markerOptions?.position(latLong)
                        markerOptions?.title("Currrent Location: "+latLong)
                        markerOptions?.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        mCurrLocationMarker = googleMap?.addMarker(markerOptions!!);

                        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLong));
                        googleMap?.animateCamera(CameraUpdateFactory.zoomTo(11F));
                    }
                }
            } else {
                Toast.makeText(requireActivity(), "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation!!
            Log.v("ddtv","onLocRes: "+mLastLocation.toString())
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }
}