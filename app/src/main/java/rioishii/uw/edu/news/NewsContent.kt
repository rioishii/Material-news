package rioishii.uw.edu.news

import android.os.Parcelable
import android.util.Log
import android.webkit.URLUtil
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat

@Parcelize
data class NewsData(
    val headline:String,
    val description:String,
    val publishedTime:Long,
    val webUrl:String,
    val imageUrl:String?,
    val sourceId:String,
    val sourceName: String
) : Parcelable

fun parseNewsAPI(response: JSONObject):List<NewsData> {

    val stories = mutableListOf<NewsData>()

    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    try {
        val jsonArticles = response.getJSONArray("articles")

        for (i in 0 until jsonArticles.length()) {
            val articleItemObj = jsonArticles.getJSONObject(i)

            //handle image url
            var imageUrl:String? = articleItemObj.getString("urlToImage")
            if (imageUrl == "null" || !URLUtil.isValidUrl(imageUrl)) {
                imageUrl = null //make actual null value
            }

            //handle date
            val publishedTime = try {
                val pubDateString = articleItemObj.getString("publishedAt")
                if(pubDateString != "null")
                    formatter.parse(pubDateString).time
                else
                    0L //return 0
            } catch (e: ParseException) {
                Log.e(NEWS_ARTICLE_TAG, "Error parsing date", e) //Android log the error
                0L //return 0
            }

            //access source
            val sourceObj = articleItemObj.getJSONObject("source")

            val story = NewsData(
                headline = articleItemObj.getString("title"),
                webUrl = articleItemObj.getString("url"),
                description = articleItemObj.getString("description"),
                imageUrl = imageUrl,
                publishedTime = publishedTime,
                sourceId = sourceObj.getString("id"),
                sourceName = sourceObj.getString("name")
            )

            stories.add(story)
        } //end for loop
    } catch (e: JSONException) {
        Log.e(NEWS_ARTICLE_TAG, "Error parsing json", e) //Android log the error
    }

    return stories
}

