package rioishii.uw.edu.news

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*


class ItemDetailFragment : Fragment() {

    private var article: NewsData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        article = arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                it.getParcelable(ARG_ITEM_ID)
            } else {
                null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.item_detail, container, false)

        article?.let {
            rootView.item_title.text = it.headline
            rootView.item_detail.text = it.description
            rootView.item_source.text = it.sourceName
        }

        return rootView
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"

        fun newInstance(article: NewsData) : ItemDetailFragment {
            val arguments = Bundle().apply {
                putParcelable(ARG_ITEM_ID, article)
            }

            val fragment = ItemDetailFragment().apply {
                setArguments(arguments)
            }
            return fragment
        }
    }
}
