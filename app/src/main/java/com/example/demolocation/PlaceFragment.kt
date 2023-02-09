package com.example.demolocation

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationRequest
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.demolocation.adapter.SearchSuggestionAdapter
import com.example.demolocation.databinding.FragmentPlaceBinding
import com.example.demolocation.viewmodel.MainViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest.*
import com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class PlaceFragment: Fragment(), OnMapReadyCallback,SearchSuggestionAdapter.OnDemoClick {
    private lateinit var binding: FragmentPlaceBinding
    private lateinit var viewModel: MainViewModel
    private var demoAdapter: SearchSuggestionAdapter? = null
    private lateinit var placesClient : PlacesClient
    private lateinit var token: AutocompleteSessionToken
    private lateinit var bounds: RectangularBounds

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
        setUpView()
        viewModel.geocoder = Geocoder(requireActivity(), Locale.getDefault())

        val apikey = getString(R.string.api_key)
        viewModel.apiKey = apikey

        initPlaceApi()
        viewModel.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        viewModel.googleMap?.setOnPoiClickListener {
            startCMS(LatLng(37.4219983,-122.084),it)
        }
        //getLastLocation()
    }

    private fun getSearchPlaces(query: String){
        val token = AutocompleteSessionToken.newInstance()
        val bounds = RectangularBounds.newInstance(
            LatLng(22.458744, 88.208162), LatLng(22.730671, 88.524896))
        val request =
            FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                //.setOrigin(LatLng(-33.8749937, 151.2041382))
                .setTypeFilter(TypeFilter.ADDRESS)
                .setTypeFilter(TypeFilter.GEOCODE)
                .setSessionToken(token)
                .setCountries("IN")
                .setQuery(query)
                .build()

        viewModel.autoCompletePredication.clear()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                for (prediction in response.autocompletePredictions) {
                    Log.i("ddtv", "autoResult: "+prediction.placeId+" , "+response.toString()+" ,,  "+prediction.placeTypes.toString())
                    Log.i("ddtv", "autoResult 22: "+prediction.getPrimaryText(null).toString()+" , "+prediction.getFullText(null).toString())
                    viewModel.autoCompletePredication.add(prediction)
                }
                if (demoAdapter != null) {
                    demoAdapter!!.notifyDataSetChanged()
                } else {
                    demoAdapter =
                        SearchSuggestionAdapter(viewModel.autoCompletePredication, this@PlaceFragment)
                    binding.searchList.adapter = demoAdapter
                }
            }.addOnFailureListener { exception: Exception? ->
                if (exception is ApiException) {
                    Log.e("ddtv", "Place not found: " + exception.statusCode+" , "+exception.message.toString())
                }
            }



    }

    private fun startCMS(d: LatLng,pointOfInterest: PointOfInterest){
        Places.initialize(requireActivity().applicationContext, viewModel.apiKey)
        val placesClient = Places.createClient(requireActivity())
        //viewModel.initClient()

       // val pointOfInterest= PointOfInterest(viewModel.latlng!!,"IN","india")
        val placeId = pointOfInterest.placeId
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG)

        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                Log.v("ddtv", "Place success: : "+place)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.v("ddtv", "Place not found: " + exception.message + ", " + "statusCode: " + statusCode)
                }
            }
    }

    private fun initPlaceApi(){
        Places.initialize(requireActivity().applicationContext, viewModel.apiKey)
        placesClient = Places.createClient(requireActivity())
        //viewModel.initClient()

        token = AutocompleteSessionToken.newInstance()
        bounds = RectangularBounds.newInstance(
            LatLng(37.4219983, -122.084 ),
            LatLng(37.4219983, -122.084 )
        )

       /* placesClient.findAutocompletePredictions(viewModel.request).addOnSuccessListener {
            var text = StringBuilder()
            for (prediction in it.autocompletePredictions ){
                text.append(" ").append(prediction.getFullText(null))
                viewModel.autoCompletePredication.add(prediction)
            }
        }.addOnFailureListener {

        }*/

        //heret
       /* val pointOfInterest= PointOfInterest(viewModel.latlng!!,"","")
        val placeId = pointOfInterest.placeId
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG)

        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place

            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode

                }
            }

        viewModel.googleMap?.setOnPoiClickListener {

        }*/
        //here
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

        binding.clBottomConfirmLocation.visibility= View.VISIBLE

        binding.searchBoxContainer.searchEditText.doOnTextChanged { text, start, before, count ->
            val query = text.toString().toLowerCase(Locale.getDefault())
           // filterWithQuery(query)
            //toggleImageView(query)
        }

        binding.searchBoxContainer.clearSearchQuery.setOnClickListener {
            binding.searchBoxContainer.searchEditText.setText(" ")
        }

        binding.searchBoxContainer.searchEditText.addTextChangedListener {

        }

        binding.searchBoxContainer.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                Log.v("ddtv","inPF afterTextChanged: "+p0.toString())
                shoePredictionList(p0.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

        })

        binding.btnClose.setOnClickListener {
            binding.clBottomConfirmLocation.visibility= View.GONE
        }

        /*binding.idSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.v("ddtv","on onQueryTextSubmit: "+query.toString())
                val location = binding.idSearchView.getQuery().toString()
                var addressList: List<Address>? = null

                //here
                getSearchPlaces(location)
                //here

                *//*if (location != null || location == "") {
                    val geocoder = Geocoder(requireActivity())
                    try {
                        addressList = geocoder.getFromLocationName(location, 3)
                        Log.v("ddtv","on onQueryTextSubmit 22: "+addressList.toString())
                        Log.v("ddtv","on onQueryTextSubmit 22: "+addressList?.size.toString())
                    } catch (e: IOException) {
                        Log.v("ddtv","in onQueryTxtSub: "+e.toString()+" , "+e.message.toString())
                        e.printStackTrace()
                    }
                    
                    if (addressList!=null){
                    val address: Address = addressList!![0]
                    val latLng = LatLng(address.getLatitude(), address.getLongitude())
                        Log.v("ddtv","in onQueryTxtSub addreesList: "+latLng)

                        if (viewModel.googleMap!=null){
                            Log.v("ddtv","in onQueryTxtSub addreesList 22 case: "+latLng.latitude+" , "+latLng.longitude)
                            viewModel.googleMap?.addMarker(MarkerOptions().position(latLng).title(location))
                            viewModel.googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                        }
                    }
                    if (demoAdapter != null) {
                        demoAdapter!!.notifyDataSetChanged()
                    } else {
                        demoAdapter =
                            SearchSuggestionAdapter(viewModel.autoCompletePredication, this@PlaceFragment)
                        //binding.rvDemo.adapter = demoAdapter
                    }
                }*//*

                return true
            }
        })*/
    }

    private fun shoePredictionList(query: String){
        getSearchPlaces(query)
    }


    //here
    private fun attachAdapter(list: ArrayList<AutocompletePrediction?>) {
        demoAdapter =
            SearchSuggestionAdapter(viewModel.autoCompletePredication, this@PlaceFragment)
        binding.searchList.adapter = demoAdapter

       /* val searchAdapter = SearchAdapter(list)
        recyclerView.adapter = searchAdapter*/
        /*if (demoAdapter != null) {
            demoAdapter!!.notifyDataSetChanged()
        } else {
            demoAdapter =
                SearchSuggestionAdapter(viewModel.autoCompletePredication, this@PlaceFragment)
            //binding.rvDemo.adapter = demoAdapter
        }*/
    }

    private fun filterWithQuery(query: String) {
        if (query.isNotEmpty()) {
            attachAdapter(viewModel.autoCompletePredication)
            toggleRecyclerView(viewModel.autoCompletePredication)
        } else if (query.isEmpty()) {
            attachAdapter(viewModel.autoCompletePredication)
        }
    }

   /* private fun onQueryChanged(filterQuery: String): List<AutocompletePrediction> {
       *//* val filteredList = ArrayList<AutocompletePrediction>()
        for (currentSport in sportsList) {
            if (currentSport.title.toLowerCase(Locale.getDefault()).contains(filterQuery)) {
                filteredList.add(currentSport)
            }
        }
        return filteredList*//*
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            binding.searchBoxContainer.searchEditText.setText(spokenText)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun toggleRecyclerView(sportsList: List<AutocompletePrediction?>) {
        if (sportsList.isEmpty()) {
            binding.searchList.visibility = View.INVISIBLE
            binding.noSearchResultsFoundText.visibility = View.VISIBLE
        } else {
            binding.searchList.visibility = View.VISIBLE
            binding.noSearchResultsFoundText.visibility = View.INVISIBLE
        }
    }

    private fun toggleImageView(query: String) {
        if (query.isNotEmpty()) {
            binding.searchBoxContainer.clearSearchQuery.visibility = View.VISIBLE
            //binding.searchBoxContainer.voiceSearchQuery.visibility = View.INVISIBLE
        } else if (query.isEmpty()) {
            binding.searchBoxContainer.clearSearchQuery.visibility = View.INVISIBLE
            //binding.searchBoxContainer.voiceSearchQuery.visibility = View.VISIBLE
        }
    }
    //here

    @SuppressLint("PotentialBehaviorOverride")
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onMapReady(mMap: GoogleMap) {
        getLastLocation()
        viewModel.googleMap = mMap

       /* mMap.addMarker(
            MarkerOptions()
                .position(LatLng(viewModel.mLatitude, viewModel.mLongitude))
                .title("Marker")
        )*/

        viewModel.googleMap!!.setOnMapClickListener {
            viewModel.markerOptions= MarkerOptions()
            viewModel.markerOptions?.position(it)
            viewModel.markerOptions?.title(it.latitude.toString())
            viewModel.googleMap!!.clear()
            viewModel.googleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 10f))
            viewModel.googleMap!!.addMarker(viewModel.markerOptions!!)
            viewModel.query = it.latitude.toString()+" , "+it.longitude.toString()
            viewModel.latlng=it
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
        viewModel.googleMap!!.isMyLocationEnabled = true
        viewModel.googleMap!!.uiSettings.isMyLocationButtonEnabled = true
        viewModel.googleMap!!.mapType= GoogleMap.MAP_TYPE_NORMAL
        Log.v("ddtv","onMapReady: 44 "+viewModel.latlng.toString()+" , ")
        if (viewModel.latlng!=null){
            viewModel.mLatitude = viewModel.latlng!!.latitude
            viewModel.mLongitude = viewModel.latlng!!.longitude

            val n = LatLng(20.593684,78.96288)
        var address= viewModel.getAddress(n,viewModel.geocoder)
        binding.tvConfirmLocation.text = address
            Log.v("ddtv","onMapReady: 55 "+address)
        }else{
            val n = LatLng(20.593684,78.96288)
            var address= viewModel.getAddress(n,viewModel.geocoder)
            binding.tvConfirmLocation.text = address
            Log.v("ddtv","onMapReady: end999  case: ")
           /* var address= viewModel.getAddress(viewModel.latlng!!,viewModel.geocoder)
            binding.tvConfirmLocation.text = address*/
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        Log.v("ddtv","in getLastLocation: 111")
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                viewModel.mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                        Log.v("ddtv","getLastLocation: if case")
                    } else {
                        Log.v("ddtv","on getLastLocation Success CompList: "+location.latitude.toString()+" , "+location.longitude.toString())
                        if (viewModel.mCurrLocationMarker!=null){
                            viewModel.mCurrLocationMarker!!.remove()
                        }

                        var latLong = LatLng(location.latitude,location.longitude)
                       /* viewModel.markerOptions = MarkerOptions()
                        viewModel.markerOptions?.position(latLong)
                        viewModel.markerOptions?.title("Currrent Location: "+latLong)
                        viewModel.markerOptions?.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        viewModel.mCurrLocationMarker = viewModel.googleMap?.addMarker(viewModel.markerOptions!!)

                        viewModel.googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLong))
                        viewModel.googleMap?.animateCamera(CameraUpdateFactory.zoomTo(11F))*/
                        viewModel.mLatitude = latLong.latitude
                        viewModel.mLongitude = latLong.longitude
                        var address= viewModel.getAddress(latLong,viewModel.geocoder)
                       // binding.tvConfirmLocation.text = address
                        Log.v("ddtv","getLastLocation: else case: "+address+" , "+latLong)

                       /* viewModel.mMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(viewModel.mLatitude, viewModel.mLongitude))
                                .title("Marker")
                        )*/
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

        viewModel.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        viewModel.mFusedLocationClient!!.requestLocationUpdates(
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
            viewModel.PERMISSION_ID
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == viewModel.PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    override fun getSelectedItem(position: Int, title: String) {
        binding.clBottomConfirmLocation.visibility= View.VISIBLE
        binding.searchBoxContainer.searchEditText.hint="Search Location"
        binding.searchBoxContainer.searchEditText.setText(title)
        viewModel.autoCompletePredication.clear()
        demoAdapter!!.notifyDataSetChanged()
        binding.tvConfirmLocation.text = title
    }
}