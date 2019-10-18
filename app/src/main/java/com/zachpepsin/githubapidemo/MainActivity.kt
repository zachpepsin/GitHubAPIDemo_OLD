package com.zachpepsin.githubapidemo

import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity(), OnRepoClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var tempDataset: ArrayList<String> = ArrayList()

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewManager = LinearLayoutManager(this)

        //val tempDataset = arrayOf("One", "Two", "Three")
        viewAdapter = RecyclerAdapter(tempDataset, this)

        recyclerView = findViewById<RecyclerView>(R.id.recycler_main).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

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

    inner class getData() : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {

            val response = params[0]
            val rootArray = JSONArray(response)

            //var repoNames:ArrayList<String> = ArrayList()

            for (i in 0 until rootArray.length()) {
                val jsonRepo = rootArray.getJSONObject(i)
                tempDataset.add(jsonRepo.getString("name"))
            }

            //tempDataset[0] = rootArray.get(0).toString()
            return "temp"
        }

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            viewAdapter.notifyDataSetChanged()
        }
    }

    // Handle an item in the recyclerView being clicked
    override fun onRepoClicked(dataItem: String) {
        Toast.makeText(this, dataItem, Toast.LENGTH_SHORT).show()
    }
}


