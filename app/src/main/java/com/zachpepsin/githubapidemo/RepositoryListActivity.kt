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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_repository_list.*
import kotlinx.android.synthetic.main.repository_list.*
import kotlinx.android.synthetic.main.repository_list_content.view.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [RepositoryDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class RepositoryListActivity : AppCompatActivity() {

    // Whether or not the activity is in two-pane mode, i.e. running on a tablet device
    private var twoPane: Boolean = false

    private var isPageLoading = false // If a new page of items is currently being loaded

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

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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
                    setupRecyclerView(repository_list)
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
            setupRecyclerView(repository_list)
        } else {
            // We do not have a network connection
            Log.d(
                RepositoryDetailActivity::class.java.simpleName,
                "Network connection not available"
            )
            // Register a network callback so if we do get a network connection, we can proceed
            val builder: NetworkRequest.Builder = NetworkRequest.Builder()
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }
    }

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
                getData().execute(responseData)

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
            SimpleItemRecyclerViewAdapter(this, repositoriesDataset.ITEMS, twoPane)

        progress_bar_repositories_center.visibility = View.VISIBLE  // Display the main progress bar

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

                    run("https://api.github.com/users/google/repos?page=$pagesLoaded&per_page=$itemsPerPageLoad")
                }
            }
        })
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: RepositoryListActivity,
        private val values: List<Repositories.RepositoryItem>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Repositories.RepositoryItem
                if (twoPane) {
                    val fragment = RepositoryDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(RepositoryDetailFragment.ARG_REPO_NAME, item.content)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.repository_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, RepositoryDetailActivity::class.java).apply {
                        putExtra(RepositoryDetailFragment.ARG_REPO_NAME, item.content)
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
            holder.idView.text = item.id
            holder.contentView.text = item.content

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
                Log.d( RepositoryDetailActivity::class.java.simpleName, "No response")
                // TODO handle not getting a response
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
            val lastItemAdded = (pagesLoaded) * itemsPerPageLoad - 1

            // Check to make sure we still have this view, since the activity could be destroyed
            if (repository_list != null) {
                repository_list.adapter?.notifyItemRangeInserted(firstItemAdded, lastItemAdded)
                progress_bar_repositories_page.visibility = View.INVISIBLE
            }

            isPageLoading = false // We are done loading the page
        }
    }
}
