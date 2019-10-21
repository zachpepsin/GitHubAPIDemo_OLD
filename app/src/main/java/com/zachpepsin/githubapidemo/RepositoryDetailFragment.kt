package com.zachpepsin.githubapidemo

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_repository_detail.*
import kotlinx.android.synthetic.main.issues_list_content.view.*
import kotlinx.android.synthetic.main.repository_detail.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

/**
 * A fragment representing a single Repositories detail screen.
 * This fragment is either contained in a [RepositoryListActivity]
 * in two-pane mode (on tablets) or a [RepositoryDetailActivity]
 * on handsets.
 */
class RepositoryDetailFragment : Fragment() {

    // The repository content this fragment is presenting.
    private var item: Repositories.RepositoryItem? = null

    private var issuesDataset = Issues()
    private var repoName: String? = null
    private var stateFilter: String = "all"

    private var isPageLoading = false // If a new page of items is currently being loaded

    // Number of items before the bottom we have to reach when scrolling to start loading next page
    private val visibleThreshold = 2

    // Number of repos to load per page (max of 100 per GitHub API)
    private val itemsPerPageLoad = 20

    private var pagesLoaded = 1 // Count of number of pages already loaded

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_REPO_NAME) && !it.getString(ARG_REPO_NAME).isNullOrEmpty()) {
                // Load the repository content specified by the fragment arguments
                repoName = it.getString(ARG_REPO_NAME)
                activity?.toolbar_layout?.title = repoName
            } else {
                // A repo name was not passed into the fragment
                Log.w(RepositoryDetailActivity::class.java.simpleName, "Repo name not supplied")
                onDestroy()
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.repository_detail, container, false)

        // Show the repository content as text in a TextView.
        item?.let {
            //rootView.repository_detail.text = it.details
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(recycler_issues)
    }

    private fun run(url: String) {
        // If we are already making a call, don't run another simultaneously
        if (isPageLoading) return

        isPageLoading = true

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("Request Failed", e.message!!)
            }

            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                getData().execute(responseData)

                // Run view-related code back on the main thread
                activity?.runOnUiThread {
                    // Hide the main progress bar
                    progress_bar_issues_center.visibility = View.GONE
                }
            }
        })
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(issuesDataset.ITEMS)

        progress_bar_issues_center.visibility = View.VISIBLE  // Display the main progress bar

        // Execute HTTP Request to retrieve issues list
        run("https://api.github.com/repos/google/$repoName/issues?page=$pagesLoaded&per_page=$itemsPerPageLoad&state=$stateFilter")

        // Add scroll listener to detect when the end of the list has been reached
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // If we are within the threshold of the bottom of the list, and we are not
                // already loading a new page of items, then load the next page of items
                if (!isPageLoading
                    && totalItemCount <= (lastVisibleItem + visibleThreshold)
                ) {
                    // Load the next page of repos
                    progress_bar_issues_page.visibility = View.VISIBLE

                    // Iterate the pages loaded counter so we load the next page
                    pagesLoaded++

                    run("https://api.github.com/repos/google/$repoName/issues?page=$pagesLoaded&per_page=$itemsPerPageLoad&state=$stateFilter")
                }
            }
        })
    }

    class SimpleItemRecyclerViewAdapter(
        private val values: List<Issues.IssueItem>
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->

                // Handle clicking on an issue
                /*
                val item = v.tag as Repositories.RepositoryItem
                val intent = Intent(v.context, RepositoryDetailActivity::class.java).apply {
                    putExtra(ARG_ITEM_ID, item.id)
                }
                v.context.startActivity(intent)
                */
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.issues_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.number
            holder.contentView.text = item.content

            // Set the number text to green/red depending on if the issue state is open/closed
            when (item.state) {
                "open" -> holder.idView.setTextColor(
                    ContextCompat.getColor(
                        holder.idView.context,
                        R.color.stateOpen
                    )
                )
                "closed" -> holder.idView.setTextColor(
                    ContextCompat.getColor(
                        holder.idView.context,
                        R.color.stateClosed
                    )
                )
                else -> Log.wtf(
                    // Class state should always be either 'open' or 'closed', per GitHub API
                    RepositoryDetailFragment::class.java.simpleName,
                    "Issue state is neither \"open\" nor \"closed\""
                )
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }

    inner class getData : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {

            val response = params[0]
            if (response.isEmpty()) {
                // We did not get a response
                Log.d(RepositoryDetailFragment::class.java.simpleName, "No response")
                // TODO handle not getting a response
            }
            val rootArray = JSONArray(response)

            for (i in 0 until rootArray.length()) {
                val jsonRepo = rootArray.getJSONObject(i)
                issuesDataset.addItem(
                    jsonRepo.getString("id"),
                    jsonRepo.getString("number"),
                    jsonRepo.getString("title"),
                    jsonRepo.getString("body"),
                    jsonRepo.getString("state")
                )
            }

            return "temp"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            // Get the range of items added to notify the dataset how many items were added
            val firstItemAdded = (pagesLoaded - 1) * itemsPerPageLoad
            val lastItemAdded = (pagesLoaded) * itemsPerPageLoad - 1

            // Check to make sure we still have this view, since the fragment could be destroyed
            if (recycler_issues != null) {
                recycler_issues.adapter?.notifyItemRangeInserted(firstItemAdded, lastItemAdded)
                progress_bar_issues_page.visibility = View.INVISIBLE
            }

            if (issuesDataset.ITEMS.size <= 0) {
                // No issues to display in list, show an empty list message
                recycler_issues.visibility = View.GONE
                text_issues_recycler_empty.visibility = View.VISIBLE
            } else if (recycler_issues.visibility == View.GONE) {
                // If the recycler is hidden and we have items to display, make it visible
                recycler_issues.visibility = View.VISIBLE
                text_issues_recycler_empty.visibility = View.GONE
            }

            isPageLoading = false // We are done loading the page
        }
    }

    fun setStateFilter(state: String) {

        when (state) {
            resources.getStringArray(R.array.state_filter_options)[0] -> {// All
                // If selection is same as previous selection, we don't need to do anything
                if (stateFilter == "all") return else stateFilter = "all"
            }
            resources.getStringArray(R.array.state_filter_options)[1] -> { // Open
                // If selection is same as previous selection, we don't need to do anything
                if (stateFilter == "open") return else stateFilter = "open"
            }
            resources.getStringArray(R.array.state_filter_options)[2] -> { // Closed
                // If selection is same as previous selection, we don't need to do anything
                if (stateFilter == "closed") return else stateFilter = "closed"
            }
        }

        // Clear dataset and adapter before repopulating the recycler
        recycler_issues.adapter?.notifyItemRangeRemoved(0, issuesDataset.ITEMS.size)
        issuesDataset.ITEMS.clear()

        progress_bar_issues_center.visibility = View.VISIBLE  // Display the main progress bar

        // Re-execute HTTP Request to retrieve issues list with filter
        run("https://api.github.com/repos/google/$repoName/issues?state=$stateFilter")
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"
        const val ARG_REPO_NAME = "repo_name"
    }
}
