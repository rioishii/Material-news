package rioishii.uw.edu.news

import android.app.ActivityOptions
import android.app.DownloadManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */

const val NEWS_ARTICLE_TAG = "NewsArticle"

class ItemListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false
    private lateinit var newsAdapter: CustomAdapter

    private var articles = mutableListOf<NewsData>()

    private var searchQuery = ""
    private val QUERY_STRING : String = "search query"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        setSupportActionBar(toolbar)

        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(QUERY_STRING)!!
        }

        handleIntent(intent)

        fab.setOnClickListener {
            finish()
            startActivity(intent);
        }

        if (item_detail_container != null) {
            twoPane = true

            val fragment = WelcomeFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.item_detail_container, fragment)
                .commit()
        }

        initRecyclerView()
        fetchArticles(searchQuery)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        (menu!!.findItem(R.id.menu_search).actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun initRecyclerView() {
        item_list.apply {
            if (twoPane) {
                layoutManager = LinearLayoutManager(this@ItemListActivity)
            } else {
                layoutManager = GridLayoutManager(this@ItemListActivity, 2)
            }
            newsAdapter = CustomAdapter()
            adapter = newsAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(QUERY_STRING, searchQuery)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
        super.onNewIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                searchQuery = query
            }
        } else {
            searchQuery = ""
        }
        fetchArticles(searchQuery)
    }

    private fun updateView(stories:List<NewsData>){
        articles.clear()
        articles.addAll(stories)
        newsAdapter.notifyDataSetChanged()
    }

    private fun fetchArticles(query: String) {
        val url = ("https://newsapi.org/v2/"
                + (if (query == "") "top-headlines?country=us" else "everything?q=$query")
                + "&language=en"
                + "&apiKey=" + getString(R.string.api_key))
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                val articles = parseNewsAPI(response)
                newsAdapter.submitList(articles)
                updateView(articles)
            },
            Response.ErrorListener { error ->
                val errorMsg = "ERROR: %s".format(error.toString())
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        )
        RequestSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    fun showDetailsView(article: NewsData, selectedView: View ) {
        if (twoPane) {
            val fragment = ItemDetailFragment.newInstance(article)
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.item_detail_container, fragment)
                .addToBackStack(null)
                .commit()

            fab.isVisible = false

            fab_share.setOnClickListener { view ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "'${article.headline}' ${article.webUrl} ")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
            supportFragmentManager.addOnBackStackChangedListener {
                if(supportFragmentManager.backStackEntryCount == 0) {
                    fab_share.isVisible = false
                    fab.isVisible = true
                }
            }
            fab_share.isVisible = true

        } else {
            val intent = Intent(selectedView.context, ItemDetailActivity::class.java).apply {
                putExtra(ItemDetailFragment.ARG_ITEM_ID, article)
            }

            selectedView.context.startActivity(intent)
        }
    }

    inner class CustomAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<NewsData> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ViewHolder (
                LayoutInflater.from(parent.context).inflate(R.layout.item_list_content, parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]

            when(holder) {
                is ViewHolder -> {
                    holder.bind(item)
                }
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener({ v ->
                    val item = v.tag as NewsData
                    showDetailsView(item, v)
                })
            }
        }

        override fun getItemCount(): Int = items.size

        fun submitList(newsData: List<NewsData>) {
            this.items = newsData
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val image = itemView.card_image
            private val headline = itemView.card_headline

            fun bind(newsData: NewsData) {
                headline.text = newsData.headline
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)

                Glide.with(itemView.context)
                    .applyDefaultRequestOptions(requestOptions)
                    .load(newsData.imageUrl)
                    .into(image)
            }
        }

    }

}
