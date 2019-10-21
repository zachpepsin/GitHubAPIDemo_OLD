package com.zachpepsin.githubapidemo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_repository_detail.*

/**
 * An activity representing a single Repositories detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item body are presented side-by-side with a list of items
 * in a [RepositoryListActivity].
 */
class RepositoryDetailActivity : AppCompatActivity() {

    var selectedFilter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository_detail)
        setSupportActionBar(detail_toolbar)

        fab.setOnClickListener {
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.filter_issues))
                .setSingleChoiceItems(
                    resources.getStringArray(R.array.state_filter_options),
                    selectedFilter
                ) { dialog, which ->
                    val fragment: RepositoryDetailFragment =
                        supportFragmentManager.findFragmentByTag(RepositoryDetailFragment.ARG_REPO_NAME) as RepositoryDetailFragment
                    fragment.setStateFilter(resources.getStringArray(R.array.state_filter_options)[which])
                    selectedFilter = which

                    dialog.dismiss()
                }
            // Create the alert dialog using builder
            val dialog: AlertDialog = builder.create()

            // Display the alert dialog
            dialog.show()
        }

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = RepositoryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        RepositoryDetailFragment.ARG_ITEM_ID,
                        intent.getStringExtra(RepositoryDetailFragment.ARG_ITEM_ID)
                    )
                    putString(
                        RepositoryDetailFragment.ARG_REPO_NAME,
                        intent.getStringExtra(RepositoryDetailFragment.ARG_REPO_NAME)
                    )
                }
            }

            supportFragmentManager.beginTransaction()
                .add(
                    R.id.repository_detail_container,
                    fragment,
                    RepositoryDetailFragment.ARG_REPO_NAME
                )
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. For
                // more body, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back

                navigateUpTo(Intent(this, RepositoryListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
