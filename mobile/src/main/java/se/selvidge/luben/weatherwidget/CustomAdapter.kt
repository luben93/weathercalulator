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
import se.selvidge.luben.weatherwidget.models.WeatherDestination

//import com.climesoft.designmaterial.R;



class CustomAdapter(private val context : Context, private val list : List<WeatherDestination>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>(){
    val geo = Geocoder(context)
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleTextView: TextView
        var countTextView: TextView
        var thumbImageView : ImageView
        var wholeCard : CardView


        init {
            titleTextView = itemView.findViewById(R.id.title) as TextView
            countTextView = itemView.findViewById(R.id.data) as TextView
            thumbImageView = itemView.findViewById(R.id.thumbnail) as ImageView
            wholeCard = itemView.findViewById(R.id.card_view) as CardView
        }
    }
    override fun onCreateViewHolder(parent : ViewGroup, type : Int) : CustomAdapter.ViewHolder{
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.card_view_item, parent, false);
        val card = view.findViewById(R.id.card_view) as CardView
        //   card.setCardBackgroundColor(Color.parseColor("#E6E6E6"));
        card.maxCardElevation = 2.0F;
        card.radius = 5.0F;
        return ViewHolder(view);
    }
    override fun onBindViewHolder(holder : CustomAdapter.ViewHolder, position : Int){
        var album = list.get(position)
        holder.titleTextView.text = geo.getFromLocation(album.destination.lat,album.destination.lon,1).first().thoroughfare
        holder.countTextView.text = album.weatherDatas.fold("",{acc,data->acc+data.getPrettyToString(context)})
//        holder.thumbImageView.setImageResource(R.drawable.cloud);
        Picasso.get().load("file:///android_asset/cloud.png").into(holder.thumbImageView)
//        holder.overflowImageView.setOnClickListener(object : View.OnClickListener{
//            override fun onClick(view: View) {
//                showPopupMenu(holder.overflowImageView)
//            }
//        });
        holder.wholeCard.setOnClickListener{showPopupMenu(holder.thumbImageView,position)};

    }
    private fun showPopupMenu(view: View,pos:Int) {
        // inflate menu
        val popup = PopupMenu(context, view)
        val inflater = popup.getMenuInflater()
        inflater.inflate(R.menu.menu_main, popup.getMenu())
        popup.setOnMenuItemClickListener{
            Log.d("ADAPTER","adapter $it $view")
            MyService.myself?.removeDestination(list[pos].destination)

            true

        }
        popup.show()
    }
    override fun getItemCount() : Int{
        return list.size;
    }


}