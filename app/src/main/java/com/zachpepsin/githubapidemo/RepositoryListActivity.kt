package com.zachpepsin.githubapidemo

import android.content.Context
import android.content.Intent
import android.net.*
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_repository_list.*
import kotlinx.android.synthetic.main.repository_list.*
import kotlinx.android.synthetic.main.repository_list_content.view.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [RepositoryDetailActivity] representing
 * item body. On tablets, the activity presents the list of items and
 * item body side-by-side using two vertical panes.
 */
class RepositoryListActivity : AppCompatActivity() {

    // Whether or not the activity is in two-pane mode, i.e. running on a tablet device
    private var twoPane: Boolean = false

    private var isPageLoading = false // If a new page of items is currently being loaded
    private var isSearchPerformed = false
    private var encodedSearchText: String = ""

    // Number of items before the bottom we have to reach when scrolling to start loading next page
    private val visibleThreshold = 2

    // Number of repos to load per page (max of 100 per GitHub API)
    private val itemsPerPageLoad = 50

    private var pagesLoaded = 1

    var repositoriesDataset = Repositories()

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setOnClickListener {
            // Show dialog to enter search keywords
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_header_repository_search))

            val dialogView = layoutInflater.inflate(R.layout.dialog_repository_search, null)

            val categoryEditText = dialogView.findViewById(R.id.categoryEditText) as EditText

            builder.setView(dialogView)
                // Set up the search button
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    encodedSearchText = URLEncoder.encode(categoryEditText.text.toString(), "UTF-8")

                    if (encodedSearchText.isEmpty()) {
                        // Don't do anything if no text was submitted
                        dialog.dismiss()
                    }

                    isSearchPerformed = true
                    pagesLoaded = 1 // New search, so reset number of pages loaded

                    // Clear the recycler and prepare for a new list to populate it
                    val itemCount = repositoriesDataset.items.size
                    repositoriesDataset.items.clear()
                    recycler_repositories.adapter?.notifyItemRangeRemoved(0, itemCount)
                    text_repositories_recycler_empty.visibility = View.GONE

                    run(
                        "https://api.github.com/search/repositories?q=$encodedSearchText+user:google&page=$pagesLoaded&per_page=$itemsPerPageLoad"
                    )
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    // Cancel button
                    dialog.cancel()
                }

                .setNeutralButton(getString(R.string.reset)) { _, _ ->
                    // Reset button.  Perform non-search query
                    isSearchPerformed = false
                    setupRecyclerView(recycler_repositories)
                }

            // Create the alert dialog using builder
            val dialog: AlertDialog = builder.create()

            // Display the alert dialog
            dialog.show()
        }

        if (repository_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }


        // Use ConnectivityManager to check if we are connected to the
        // internet, and if so, what type of connection is in place
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        // This callback will be used if we don't have an initial connection and need to detect
        // when a network connection is established
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(
                    RepositoryDetailActivity::class.java.simpleName,
                    "Network connection now available"
                )

                // We are now connected and don't need this callback anymore
                // Note: If we were to check for disconnects and re-connects after initial
                // connection, we would keep this registered and use onLost
                connectivityManager.unregisterNetworkCallback(this)

                // We now have a network connection and can load the data
                // This has to be run on the UI thread because the callback is on a different thread
                runOnUiThread {
                    // Hide the 'no connection' message and re-display the recycler
                    // Reset the text of it back to the 'no items found' message in case the view is
                    // used again in the case that no repos are returned for the user
                    text_repositories_recycler_empty.visibility = View.GONE
                    text_repositories_recycler_empty.text =
                        getString(R.string.text_repositories_recycler_empty)

                    setupRecyclerView(recycler_repositories)
                }
            }
        }

        /**
         * For SDK 22+, we can use the new getNetworkCapabilities method from the ConnectionManager
         * For SDK <22, we have to use the deprecated activeNetworkInfo method, because its
         * replacement is only available on SDK22+
         */
        var networkAvailable = false
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT > 22) {
            // For devices with API >= 23
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            // NET_CAPABILITY_VALIDATED - Indicates that connectivity on this network was successfully validated.
            // NET_CAPABILITY_INTERNET - Indicates that this network should be able to reach the internet.
            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {

                if (networkCapabilities.hasTransport(TRANSPORT_WIFI)) {
                    networkAvailable = true
                } else if (networkCapabilities.hasTransport(TRANSPORT_CELLULAR)) {
                    networkAvailable = true
                }
            }
        } else {
            // For devices with API < 23
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            val isConnected: Boolean = activeNetwork?.isConnected == true
            if (isConnected) {
                networkAvailable = true
            }
        }

        if (networkAvailable) {
            // We have a network connection
            Log.d(
                RepositoryDetailActivity::class.java.simpleName,
                "Network connection available"
            )
            // Proceed to set up recycler and load data
            setupRecyclerView(recycler_repositories)
        } else {
            // We do not have a network connection
            Log.d(
                RepositoryDetailActivity::class.java.simpleName,
                "Network connection not available"
            )

            // Display a 'no connection' message
            recycler_repositories.visibility = View.GONE
            text_repositories_recycler_empty.text = getString(R.string.text_no_network_connection)
            text_repositories_recycler_empty.visibility = View.VISIBLE

            // Register a network callback so if we do get a network connection, we can proceed
            val builder: NetworkRequest.Builder = NetworkRequest.Builder()
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }
    }

    // Runs an API request.  Leave searchString null if not performing a search
    private fun run(url: String) {
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
                if (!isSearchPerformed) {
                    // We are not performing a search, just loading a page of repos
                    LoadRepositories().execute(responseData)
                } else {
                    // We did perform a search
                    LoadRepositoriesSearch().execute(responseData)
                }

                // Run view-related code back on the main thread
                runOnUiThread {
                    // Hide the main progress bar
                    progress_bar_repositories_center.visibility = View.GONE
                }
            }
        })
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter =
            RepositoryItemRecyclerViewAdapter(this, repositoriesDataset.items, twoPane)

        progress_bar_repositories_center.visibility = View.VISIBLE  // Display the main progress bar

        pagesLoaded = 1
        repositoriesDataset.items.clear()

        // Add divider for recycler
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Execute HTTP Request to load first batch of repos
        run("https://api.github.com/users/google/repos?page=$pagesLoaded&per_page=$itemsPerPageLoad")

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
                    isPageLoading = true
                    progress_bar_repositories_page.visibility = View.VISIBLE

                    // Iterate the pages loaded counter so we load the next page
                    pagesLoaded++

                    if (!isSearchPerformed) {
                        run("https://api.github.com/users/google/repos?page=$pagesLoaded&per_page=$itemsPerPageLoad")
                    } else {
                        // If we have less search results than however many we tried to load by now,
                        // Then we are at the end of the list of results
                        run("https://api.github.com/search/repositories?q=$encodedSearchText+user:google&page=$pagesLoaded&per_page=$itemsPerPageLoad")
                    }
                }

                // Hide FAB when scrolling down, show when scrolling up
                if (dy > 0) {
                    fab.hide()
                } else {
                    fab.show()
                }
            }
        })
    }

    class RepositoryItemRecyclerViewAdapter(
        private val parentActivity: RepositoryListActivity,
        private val values: List<Repositories.RepositoryItem>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<RepositoryItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Repositories.RepositoryItem
                if (twoPane) {
                    val fragment = RepositoryDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(RepositoryDetailFragment.ARG_ITEM_ID, item.id)
                            putString(RepositoryDetailFragment.ARG_REPO_NAME, item.name)
                            putString(
                                RepositoryDetailFragment.ARG_REPO_DESCRIPTION,
                                item.description
                            )
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.repository_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, RepositoryDetailActivity::class.java).apply {
                        putExtra(RepositoryDetailFragment.ARG_ITEM_ID, item.id)
                        putExtra(RepositoryDetailFragment.ARG_REPO_NAME, item.name)
                        putExtra(RepositoryDetailFragment.ARG_REPO_DESCRIPTION, item.description)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.repository_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.textName.text = item.name

            if (item.description != "null")
                holder.textDescription.text = item.description
            else {
                holder.textDescription.text = parentActivity.getString(R.string.no_description)
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textName: TextView = view.text_name
            val textDescription: TextView = view.text_description
        }
    }


    inner class LoadRepositories : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {
            val response = params[0]
            if (response.isEmpty()) {
                // We did not get a response
                Log.e(RepositoryDetailActivity::class.java.simpleName, "No response")
            }
            val rootArray = JSONArray(response)

            for (i in 0 until rootArray.length()) {
                val jsonRepo = rootArray.getJSONObject(i)
                repositoriesDataset.addItem(
                    jsonRepo.getString("id"),
                    jsonRepo.getString("name"),
                    jsonRepo.getString("description")
                )
            }
            return "temp"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            // Get the range of items added to notify the dataset how many items were added
            val firstItemAdded = (pagesLoaded - 1) * itemsPerPageLoad
            val lastItemAdded = ((pagesLoaded) * itemsPerPageLoad) - 1

            // Check to make sure we still have this view, since the activity could be destroyed
            if (recycler_repositories != null) {
                recycler_repositories.adapter?.notifyItemRangeInserted(
                    firstItemAdded,
                    lastItemAdded
                )
                progress_bar_repositories_page.visibility = View.INVISIBLE
            }

            if (repositoriesDataset.items.size <= 0) {
                // No repositories to display in list, show an empty list message
                recycler_repositories.visibility = View.GONE
                text_repositories_recycler_empty.visibility = View.VISIBLE
            } else if (recycler_repositories.visibility == View.GONE) {
                // If the recycler is hidden and we have items to display, make it visible
                recycler_repositories.visibility = View.VISIBLE
                text_repositories_recycler_empty.visibility = View.GONE
            }

            isPageLoading = false // We are done loading the page
        }
    }

    // Loads the repositories after performing a search
    inner class LoadRepositoriesSearch : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {
            val response = params[0]
            if (response.isEmpty()) {
                // We did not get a response
                Log.e(RepositoryDetailActivity::class.java.simpleName, "No response")
            }

            val rootObject = JSONObject(response)
            val repoJsonArray = rootObject.getJSONArray("items")

            for (i in 0 until repoJsonArray.length()) {
                val jsonRepo = repoJsonArray.getJSONObject(i)
                repositoriesDataset.addItem(
                    jsonRepo.getString("id"),
                    jsonRepo.getString("name"),
                    jsonRepo.getString("description")
                )
            }
            return "temp"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            // Get the range of items added to notify the dataset how many items were added
            val firstItemAdded = (pagesLoaded - 1) * itemsPerPageLoad
            //val lastItemAdded = min((pagesLoaded) * itemsPerPageLoad - 1, repositoriesDataset.items.size - 1)
            val lastItemAdded = ((pagesLoaded) * itemsPerPageLoad) - 1

            // Check to make sure we still have this view, since the activity could be destroyed
            if (recycler_repositories != null) {
                recycler_repositories.adapter?.notifyItemRangeInserted(
                    firstItemAdded,
                    lastItemAdded
                )
                progress_bar_repositories_page.visibility = View.INVISIBLE
            }

            if (repositoriesDataset.items.size <= 0) {
                // No repositories to display in list, show an empty list message
                recycler_repositories.visibility = View.GONE
                text_repositories_recycler_empty.visibility = View.VISIBLE
            } else if (recycler_repositories.visibility == View.GONE) {
                // If the recycler is hidden and we have items to display, make it visible
                recycler_repositories.visibility = View.VISIBLE
                text_repositories_recycler_empty.visibility = View.GONE
            }

            isPageLoading = false // We are done loading the page
        }
    }
}
