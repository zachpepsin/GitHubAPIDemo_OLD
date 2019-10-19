package com.zachpepsin.githubapidemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_recycler.view.*

class RecyclerAdapter(
    private val myDataset: ArrayList<String>,
    val itemClickListener: OnRepoClickListener
) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    interface OnRepoClickListener {
        fun onRepoClicked(dataItem: String)
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        // Holds the TextView that will set for each item
        private val textHeader = view.text_header

        fun bind(dataItem: String, clickListener: OnRepoClickListener) {
            textHeader.text = dataItem

            itemView.setOnClickListener {
                clickListener.onRepoClicked(dataItem)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycler, parent, false) as View
        // set the view's size, margins, paddings and layout parameters
        // ...
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        //holder.textHeader.text = myDataset[position]

        holder.bind(myDataset[position], itemClickListener)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}

