package com.zachpepsin.githubapidemo

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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

    /**
     * The repository content this fragment is presenting.
     */
    private var item: Repositories.RepositoryItem? = null

    private var tempDataset = Issues()

    private var repoName: String? = null

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            /*
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the repository content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item = tempDataset.ITEM_MAP[it.getString(ARG_ITEM_ID)]
                activity?.toolbar_layout?.title = item?.content
            }
             */
            if (it.containsKey(ARG_REPO_NAME) && !it.getString(ARG_REPO_NAME).isNullOrEmpty()) {
                // Load the repository content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                repoName = it.getString(ARG_REPO_NAME)
                activity?.toolbar_layout?.title = repoName
            } else {
                // TODO A repo name was not passed into the fragment
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

        setupRecyclerView(issues_list)

        // Execute HTTP Request to retrieve issues list
        run("https://api.github.com/repos/google/$repoName/issues?state=all")
    }

    private fun run(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            //override fun onResponse(call: Call, response: Response) = println(response.body()?.string())


            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                getData().execute(responseData)
                /*
                runOnUiThread{
                    try {
                        var json = JSONObject(responseData)
                        println("Request Successful!!")
                        println(json)
                        val responseObject = json.getJSONObject("response")
                        val docs = json.getJSONArray("docs")
                        this@MainActivity.fetchComplete()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                 */
            }
        })
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        //recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, DummyContent.ITEMS, twoPane)

        //Repositories.ITEMS.clear()
        recyclerView.adapter =
            SimpleItemRecyclerViewAdapter(tempDataset.ITEMS)
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

    inner class getData() : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {

            val response = params[0]
            val rootArray = JSONArray(response)

            //var repoNames:ArrayList<String> = ArrayList()

            for (i in 0 until rootArray.length()) {
                val jsonRepo = rootArray.getJSONObject(i)
                //tempDataset.add(jsonRepo.getString("name"))\
                tempDataset.addItem(
                    jsonRepo.getString("id"),
                    jsonRepo.getString("number"),
                    jsonRepo.getString("title"),
                    jsonRepo.getString("body"),
                    jsonRepo.getString("state")
                )
            }

            //tempDataset[0] = rootArray.get(0).toString()
            return "temp"
        }

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            // Check to make sure we still have this view, since the fragment could be destroyed
            if (issues_list != null)
                issues_list.adapter?.notifyDataSetChanged()
        }
    }

    fun setStateFilter(state:String) {
        Toast.makeText(context, state, Toast.LENGTH_SHORT).show()
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
