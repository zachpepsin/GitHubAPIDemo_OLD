package com.zachpepsin.githubapidemo

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
class RepositoryListActivity : AppCompatActivity(), OnRepoClickListener {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    private var tempDataset = Repositories()

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

        setupRecyclerView(repository_list)

        // Execute HTTP Request
        run("https://api.github.com/users/google/repos?page=1&per_page=100")
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

        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, tempDataset.ITEMS, twoPane)
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
                            //putString(RepositoryDetailFragment.ARG_ITEM_ID, item.id)
                            putString(RepositoryDetailFragment.ARG_REPO_NAME, item.content)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.repository_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, RepositoryDetailActivity::class.java).apply {
                        //putExtra(RepositoryDetailFragment.ARG_ITEM_ID, item.id)
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


    inner class getData() : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {

            val response = params[0]
            val rootArray = JSONArray(response)

            //var repoNames:ArrayList<String> = ArrayList()

            for (i in 0 until rootArray.length()) {
                val jsonRepo = rootArray.getJSONObject(i)
                //tempDataset.add(jsonRepo.getString("name"))
                tempDataset.addItem(
                    jsonRepo.getString("id"),
                    jsonRepo.getString("name"),
                    jsonRepo.getString("description")
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

            repository_list.adapter?.notifyDataSetChanged()
            //viewAdapter.notifyDataSetChanged()
        }
    }

    // Handle an item in the recyclerView being clicked
    // Example: https://github.com/ngengesenior/RecyclerViewClickListener/blob/master/app/src/main/java/com/example/ngenge/recyclerviewclicklistener/MainActivity.kt
    override fun onRepoClicked(dataItem: String) {
        Toast.makeText(this, dataItem, Toast.LENGTH_SHORT).show()
    }
}
