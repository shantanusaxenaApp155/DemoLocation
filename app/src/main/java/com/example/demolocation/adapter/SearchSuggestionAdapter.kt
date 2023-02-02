package com.example.demolocation.adapter

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.demolocation.databinding.RowSearchSuggestionBinding
import com.example.demolocation.diffutil.DemoDiffUtill
import com.google.android.libraries.places.api.model.AutocompletePrediction

class SearchSuggestionAdapter internal constructor(
    private var allsearchSuggestionList: ArrayList<AutocompletePrediction?>,
    val onDemoListener: OnDemoClick) : RecyclerView.Adapter<DemoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
        val binding = RowSearchSuggestionBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return DemoViewHolder(binding,onDemoListener)
    }

    override fun getItemCount(): Int {
        return allsearchSuggestionList.size
    }

    override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
        holder.bind(allsearchSuggestionList[position]!!,position)
    }

    fun setData(newList: List<AutocompletePrediction>) {
        val diffResult = DiffUtil.calculateDiff(
            DemoDiffUtill(allsearchSuggestionList, newList),
            true
        )
        allsearchSuggestionList = ArrayList(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    interface OnDemoClick{
        fun getSelectedItem(position: Int,title: String)
    }

}


class DemoViewHolder(private val binding: RowSearchSuggestionBinding,private val onDemoClick: SearchSuggestionAdapter.OnDemoClick) : RecyclerView.ViewHolder(binding.root) {

    fun bind(dataItem: AutocompletePrediction,position: Int) {
        binding.tvRowAddress.text=dataItem.getFullText(null)

        binding.root.setOnClickListener {
            onDemoClick.getSelectedItem(position,
                dataItem.getFullText(null).toString())
        }
    }
}