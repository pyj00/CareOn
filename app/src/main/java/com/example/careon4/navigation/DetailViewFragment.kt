package com.example.careon4.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.careon4.R
import com.example.careon4.databinding.FragmentDetailBinding
import com.example.careon4.databinding.ItemDetailBinding
import com.example.careon4.navigation.model.AlarmDTO
import com.example.careon4.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DetailViewFragment : Fragment(){
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    lateinit var binding:FragmentDetailBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding=FragmentDetailBinding.inflate(inflater)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter(binding)
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }


    inner class DetailViewRecyclerViewAdapter(val binding: FragmentDetailBinding) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {


            firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    if(querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var binding2 = ItemDetailBinding.inflate(LayoutInflater.from(p0.context), p0, false)
            return CustomViewHolder(binding2)
        }


        inner class CustomViewHolder(val binding2: ItemDetailBinding) : RecyclerView.ViewHolder(binding2.root) {

        }


        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            val viewholder = (p0 as CustomViewHolder)


            //UserId
            viewholder.binding2.detailviewitemProfileTextview.text = contentDTOs!![p1].userId

            //Image
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl)
                .into(viewholder.binding2.detailviewitemImageviewContent)

            //Explain of content
            viewholder.binding2.detailviewitemExplainTextview.text = contentDTOs!![p1].explain

            //likes
            viewholder.binding2.detailviewitemFavoritecounterTextview.text =
                "Likes " + contentDTOs!![p1].favoriteCount

            //This code is when the button is clicked
            viewholder.binding2.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(p1)
            }
            //This code is when the page is loaded
            if(contentDTOs!![p1].favorites.containsKey(uid)){
                //This is like status
                viewholder.binding2.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)

            }else{
                //This is unlike status
                viewholder.binding2.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            //This code is when the profile image is clicked
            viewholder.binding2.detailviewitemProfileImage.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[p1].uid)
                bundle.putString("userId",contentDTOs[p1].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            viewholder.binding2.detailviewitemCommentImageview.setOnClickListener { v ->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[p1])
                intent.putExtra("destinationUid",contentDTOs[p1].uid)
                startActivity(intent)
            }

        }
        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    //When the button is clicked
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! - 1
                    contentDTO?.favorites!!.remove(uid)
                }else{
                    //When the button is not clicked
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! + 1
                    contentDTO?.favorites!![uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)

                }
                transaction.set(tsDoc,contentDTO)
            }

        }
        fun favoriteAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            //var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            //FcmPush.instance.sendMessage(destinationUid,"Howlstagram",message)
        }



    }
}