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
import android.view.inputmethod.InputMethodManager
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
            startPointOfInterest(LatLng(37.4219983,-122.084),it)
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
                    //Log.i("ddtv", "autoResult: "+prediction.placeId+" , "+response.toString()+" ,,  "+prediction.placeTypes.toString())
                    //Log.i("ddtv", "autoResult 22: "+prediction.getPrimaryText(null).toString()+" , "+prediction.getFullText(null).toString())
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

    private fun startPointOfInterest(d: LatLng,pointOfInterest: PointOfInterest){
        Log.v("ddtv","startCMS: if case ")
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
        }.addOnFailureListener { }*/
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

        binding.searchBoxContainer.clearSearchQuery.setOnClickListener {
            binding.searchBoxContainer.searchEditText.setText(" ")
            binding.searchBoxContainer.clearSearchQuery.visibility=View.GONE
        }

        binding.searchBoxContainer.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                showPredictionList(p0.toString())
                binding.searchBoxContainer.clearSearchQuery.visibility=View.VISIBLE
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

        })

        binding.btnClose.setOnClickListener {
            binding.clBottomConfirmLocation.visibility= View.GONE
        }


        //for Geocoder
        /*
         val location = binding.idSearchView.getQuery().toString()
        var addressList: List<Address>? = null

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
        }*/
    }

    private fun getLatLngFromAddress(address:String): LatLng {
        val location = address
        var addressList: List<Address>? = null
        val geocoder = Geocoder(requireActivity())
        try {
            addressList = geocoder.getFromLocationName(location, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (addressList!=null){
            val address: Address = addressList!![0]
            val latLng = LatLng(address.getLatitude(), address.getLongitude())
            if (viewModel.googleMap!=null){
                viewModel.googleMap?.addMarker(MarkerOptions().position(latLng).title(location))
                viewModel.googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
            }
            return latLng
        }

        return viewModel.latlng!!
    }

    private fun showPredictionList(query: String){
        getSearchPlaces(query)
    }

    @SuppressLint("PotentialBehaviorOverride")
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onMapReady(mMap: GoogleMap) {
        Log.v("ddtv","onMapReady:  ")
        getLastLocation()
        viewModel.googleMap = mMap

       /* mMap.addMarker(
            MarkerOptions()
                .position(LatLng(viewModel.mLatitude, viewModel.mLongitude))
                .title("Marker")
        )*/

        viewModel.googleMap!!.setOnMapClickListener {
            binding.searchBoxContainer.searchEditText.clearFocus()
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
        if (viewModel.latlng!=null){
            viewModel.mLatitude = viewModel.latlng!!.latitude
            viewModel.mLongitude = viewModel.latlng!!.longitude
            val latlng = LatLng(20.593684,78.96288)
        var address= viewModel.getAddress(latlng,viewModel.geocoder)
        binding.tvConfirmLocation.text = address
        }else{
            val n = LatLng(20.593684,78.96288)
            var address= viewModel.getAddress(n,viewModel.geocoder)
            binding.tvConfirmLocation.text = address
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                viewModel.mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                        Log.v("ddtv","onLocationChanged: if case ")
                    } else {
                        Log.v("ddtv","onLocationChanged: else case ")
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

                        viewModel.googleMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(viewModel.mLatitude, viewModel.mLongitude))
                                .title("Marker")
                        )
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
            Log.v("ddtv","onLocRes: 555  "+mLastLocation.toString())
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
        //binding.searchBoxContainer.searchEditText.setText(title)
        viewModel.autoCompletePredication.clear()
        demoAdapter!!.notifyDataSetChanged()
        binding.tvConfirmLocation.text = title
        hideKeyboard()
        binding.searchBoxContainer.clearSearchQuery.visibility=View.GONE

        val latlng=getLatLngFromAddress(title)
        val mLatitude= latlng.latitude
        val mLongitude= latlng.longitude

        viewModel.googleMap?.addMarker(
            MarkerOptions()
                .position(latlng)
                .title("Marker")
        )

    }

    fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        binding.searchBoxContainer.searchEditText.clearFocus()
    }
}
