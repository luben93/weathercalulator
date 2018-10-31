package se.selvidge.luben.weatherwidget


import android.content.Context
import android.location.Geocoder
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.squareup.picasso.Picasso
import se.selvidge.luben.weatherwidget.models.WeatherData
import se.selvidge.luben.weatherwidget.models.WeatherDestination
import se.selvidge.luben.weatherwidget.models.WeatherView
import java.util.*
import kotlin.math.roundToInt

//import com.climesoft.designmaterial.R;


class WeatherCardAdapter(private val context: Context, private val list: List<WeatherDestination>) : RecyclerView.Adapter<WeatherCardAdapter.ViewHolder>() {
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

        holder.countTextView.text = weatherPositions.weatherDatas.foldAndAvg(context)
        Picasso.get().load("file:///android_asset/cloud.png").into(holder.thumbImageView) //todo better graphics
        holder.wholeCard.setOnClickListener { showPopupMenu(holder.thumbImageView, position) }

    }

    private fun showPopupMenu(view: View, pos: Int) {
        // inflate menu
        val popup = PopupMenu(context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_main, popup.menu)
        popup.setOnMenuItemClickListener {
            Log.d("ADAPTER", "adapter $it $view")
            MyService.myself?.removeDestination(list[pos].destination)
            true
        }
        popup.show()
    }

    override fun getItemCount(): Int {
        return list.size
    }


}
