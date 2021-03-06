package se.selvidge.luben.weatherwidget


import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import se.selvidge.luben.weatherwidget.models.*
import java.lang.Appendable
import java.util.*
import kotlin.math.roundToInt

//import com.climesoft.designmaterial.R;


class WeatherCardAdapter(private val context: Context, private val list: MutableList<WeatherDestination>) : RecyclerView.Adapter<WeatherCardAdapter.ViewHolder>() {
    val geo = Geocoder(context)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.title) as TextView
        var countTextView: TextView = itemView.findViewById(R.id.data) as TextView
        var thumbImageView: ImageView = itemView.findViewById(R.id.thumbnail) as ImageView
        var wholeCard: CardView = itemView.findViewById(R.id.card_view) as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): WeatherCardAdapter.ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.card_view_item, parent, false)
        val card = view.findViewById(R.id.card_view) as CardView
        card.maxCardElevation = 2.0F
        card.radius = 5.0F
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherCardAdapter.ViewHolder, position: Int) {
        var weatherPositions = list[position]
        holder.titleTextView.text = geo.getFromLocation(weatherPositions.destination.lat, weatherPositions.destination.lon, 1).first().thoroughfare
        try {
            holder.countTextView.text = weatherPositions.weatherDatas.foldAndAvg(context)
        } catch (e: IllegalArgumentException) {
            //???
        }
        Picasso.get().load("file:///android_asset/cloud.png").into(holder.thumbImageView) //todo better graphics
        holder.wholeCard.setOnClickListener { showPopupMenu(holder.thumbImageView, position) }

    }

    private fun showPopupMenu(view: View, pos: Int) {
        // inflate menu
//        val popup = PopupMenu(context, view)
//        val inflater = popup.menuInflater
//        inflater.inflate(R.menu.menu_main, popup.menu)
//        popup.setOnMenuItemClickListener {
//            doAsync {
//                Log.d("ADAPTER", "adapter $it $view")
//                AppDatabase.getDatabase(context).destinationDao().delete(list[pos].destination)
//                list.removeAt(pos)
//                context.runOnUiThread {
//                    notifyItemRemoved(pos)
//                    notifyItemRangeChanged(pos, list.size)
//                }
//            }
//            true
//        }
//        popup.show()
        context.startActivity(Intent(context, PopoverComuteSelector::class.java).apply { putExtra("destinationId",list[pos].destination.id)})


    }

    override fun getItemCount(): Int {
        return list.size
    }


}
