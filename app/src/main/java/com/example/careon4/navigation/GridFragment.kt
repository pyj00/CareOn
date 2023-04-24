package com.example.careon4.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.careon4.R
import com.example.careon4.databinding.FragmentGridBinding
import com.example.careon4.databinding.FragmentUserBinding
import com.example.careon4.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore

class GridFragment : Fragment(){
    lateinit var binding:FragmentGridBinding
    lateinit var binding2:FragmentUserBinding
    var firestore : FirebaseFirestore? = null
    var fragmentView : View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding= FragmentGridBinding.inflate(inflater)
        binding2 = FragmentUserBinding.inflate(inflater)
        firestore = FirebaseFirestore.getInstance()
        binding?.gridfragmentRecyclerview?.adapter = UserFragmentRecyclerViewAdapter(binding2)
        binding?.gridfragmentRecyclerview?.layoutManager = GridLayoutManager(activity, 3)
        return binding.root
    }
    inner class UserFragmentRecyclerViewAdapter(val binding: FragmentUserBinding) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot == null) return@addSnapshotListener

                //Get data
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageview = ImageView(p0.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var imageview = (p0 as CustomViewHolder).imageview
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}