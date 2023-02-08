package com.example.demolocation.viewmodel

import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.provider.Settings.Global.getString
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demolocation.R
import com.example.demolocation.databinding.FragmentPlaceBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.flow.MutableSharedFlow

class MainViewModel : ViewModel() {
    lateinit var binding: FragmentPlaceBinding
    var markerOptions: MarkerOptions? = null
    var googleMap: GoogleMap? = null
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var viewModel: MainViewModel
    val PERMISSION_ID = 200
    var currentLocation: Location? = null
    var mCurrLocationMarker: Marker?= null
    lateinit var apiKey: String
    var query: String=""
    var latlng: LatLng?=null
    var mLatitude: Double=0.0
    var mLongitude: Double=0.0
    lateinit var request:  FindAutocompletePredictionsRequest
    lateinit var geocoder:Geocoder
    val autoCompletePredication = ArrayList<AutocompletePrediction?>()
    val addressList = MutableSharedFlow<List<Address>>(1)
    val autoCompletePredicationFlow = MutableSharedFlow<List<AutocompletePrediction>>(1)

    init {
        initClient()
    }

    fun initClient(){
        val token = AutocompleteSessionToken.newInstance()
        val rectangularBounds = RectangularBounds.newInstance(
            LatLng(mLatitude, mLongitude), LatLng(mLatitude, mLongitude)
        )

        request = FindAutocompletePredictionsRequest.builder()
            .setLocationBias(rectangularBounds)
            .setCountry("IN")
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery(query)
            .build();
    }


    fun getAddress(latLng: LatLng,geocoder: Geocoder): String {
        val addresses: List<Address>?
        val address: Address?
        var fulladdress = ""
        var fulAddress= StringBuffer()
        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

        if (addresses!!.isNotEmpty()) {
            address = addresses[0]
            fulladdress = address.getAddressLine(0)
            var city = address.getLocality()
            var state = address.getAdminArea()
            var country = address.getCountryName()
            var postalCode = address.getPostalCode()
            var knownName = address.getFeatureName()
            return fulAddress.append(fulAddress).append(" ").append(city).append(" ").append(state).append(" ")
                .append(country).append("\n").append(" ").append(postalCode).toString()
        } else{
             fulladdress = "Location not found"
            return fulAddress.append("Location not found").toString()
        }
        return latLng.toString()
    }
}