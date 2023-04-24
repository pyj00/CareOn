package com.example.careon4.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.careon4.R
import com.example.careon4.databinding.FragmentAlarmBinding
import com.example.careon4.databinding.ItemCommentBinding
import com.example.careon4.navigation.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AlarmFragment : Fragment(){
    lateinit var binding:FragmentAlarmBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding=FragmentAlarmBinding.inflate(inflater)
        binding.alarmfragmentRecyclerview.adapter = AlarmRecyclerviewAdapter(binding)
        binding.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }
    inner class AlarmRecyclerviewAdapter(val binding: FragmentAlarmBinding) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()
        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid",uid).addSnapshotListener{
                    querySnapshot, firebaseFirestoreException -> alarmDTOList.clear()
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            return CustomViewHolder(ItemCommentBinding.inflate(LayoutInflater.from(p0.context),p0,false))
        }
        inner class CustomViewHolder(val binding : ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        }
        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = (p0 as CustomViewHolder)


            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[p1].uid!!).get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]
                    Glide.with(view.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.binding.commentviewitemImageviewProfile)
                }
            }

            when(alarmDTOList[p1].kind){
                0 -> {
                    val str_0 = alarmDTOList[p1].userId + getString(R.string.alarm_favorite)
                    view.binding.commentviewitemTextviewProfile.text = str_0
                }
                1 -> {
                    val str_0 = alarmDTOList[p1].userId + " " + getString(R.string.alarm_comment) +" of " + alarmDTOList[p1].message
                    view.binding.commentviewitemTextviewProfile.text = str_0
                }
                2 -> {
                    val str_0 = alarmDTOList[p1].userId + " " + getString(R.string.alarm_follow)
                    view.binding.commentviewitemTextviewProfile.text = str_0
                }
            }
            view.binding.commentviewitemTextviewComment.visibility = View.INVISIBLE
        }

    }
}
