package com.example.careon4.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.careon4.R
import com.example.careon4.databinding.ActivityAddPhotoBinding
import com.example.careon4.databinding.ActivityCommentBinding
import com.example.careon4.databinding.FragmentUserBinding
import com.example.careon4.databinding.ItemCommentBinding
import com.example.careon4.navigation.model.AlarmDTO
import com.example.careon4.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentBinding
    var contentUid : String? = null
    var destinationUid : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        binding.commentRecyclerview.adapter = CommentRecyclerviewAdapter(binding)
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        binding.commentBtnSend?.setOnClickListener {
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = binding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!,binding.commentEditMessage.text.toString())
            binding.commentEditMessage.setText("")
        }
        val view = binding.root
        setContentView(view)
    }
    fun commentAlarm(destinationUid : String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        //var msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " of " + message
        //FcmPush.instance.sendMessage(destinationUid,"Howlstagram",msg)
    }
    inner class CommentRecyclerviewAdapter(val binding: ActivityCommentBinding) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot == null)return@addSnapshotListener

                    for(snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            //var view = LayoutInflater.from(p0.context).inflate(R.layout.item_comment,p0,false)
            var binding2 = ItemCommentBinding.inflate(LayoutInflater.from(p0.context),p0,false)
            return CustomViewHolder(binding2)
        }

        private inner class CustomViewHolder(val binding2 : ItemCommentBinding) : RecyclerView.ViewHolder(binding2.root)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = (p0 as CustomViewHolder)
            view.binding2.commentviewitemTextviewComment.text = comments[p1].comment
            view.binding2.commentviewitemTextviewProfile.text = comments[p1].userId

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[p1].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        var url = task.result!!["image"]
                        Glide.with(p0.binding2.root.context).load(url).apply(RequestOptions().circleCrop()).into(view.binding2.commentviewitemImageviewProfile)
                    }
                }
        }

    }
}