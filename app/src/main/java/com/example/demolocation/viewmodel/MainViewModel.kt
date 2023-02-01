package com.example.demolocation.viewmodel

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
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.launch
import java.lang.StringBuilder

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
    lateinit var request:  FindAutocompletePredictionsRequest

    init {
        initClient()
    }

    fun initClient(){
        val token = AutocompleteSessionToken.newInstance()
        val rectangularBounds = RectangularBounds.newInstance(
            LatLng(-33.880490, 151.184363),
            LatLng(-33.858754, 151.229596)
        )

        request = FindAutocompletePredictionsRequest.builder()
            .setLocationBias(rectangularBounds)
            .setCountry("in")
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery(query)
            .build();
    }

    fun getNewComment(id: Int) {
       /* viewModelScope.launch {
            repository.getComment(id)
                .catch {
                    commentState.value =
                        CommentApiState.error(it.message.toString())
                }
                .collect {
                    commentState.value = CommentApiState.success(it.data)
                }
        }*/
    }

    fun initNew(){

    }
}