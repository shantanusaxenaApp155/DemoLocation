package com.example.demolocation.diffutil

import androidx.recyclerview.widget.DiffUtil
import com.google.android.libraries.places.api.model.AutocompletePrediction

class DemoDiffUtill constructor(
    private val oldList: List<AutocompletePrediction?>,
    private val newList: List<AutocompletePrediction?>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]?.getFullText(null) == newList[newItemPosition]?.getFullText(null)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].toString() == newList[newItemPosition].toString()
    }
}
