package com.example.careon4.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.careon4.LoginActivity
import com.example.careon4.R
import com.example.careon4.databinding.ActivityMainBinding
import com.example.careon4.databinding.FragmentUserBinding
import com.example.careon4.navigation.model.AlarmDTO
import com.example.careon4.navigation.model.ContentDTO
import com.example.careon4.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class UserFragment : Fragment() {
    lateinit var binding:FragmentUserBinding
    lateinit var binding2:ActivityMainBinding
    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUserUid : String? = null
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding= FragmentUserBinding.inflate(inflater)
        binding2= ActivityMainBinding.inflate(inflater)

        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid == currentUserUid){
            //MyPage
            binding?.accountBtnFollowSignout?.text = getString(R.string.signout)
            binding?.accountBtnFollowSignout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //OtherUserPage
            binding?.accountBtnFollowSignout?.text = getString(R.string.follow)
            binding2?.toolbarUsername?.text = arguments?.getString("userId")
            binding2?.toolbarBtnBack?.setOnClickListener {
                binding2.bottomNavigation.selectedItemId = R.id.action_home
            }
            binding2?.toolbarTitleImage?.visibility = View.GONE
            binding2?.toolbarUsername?.visibility = View.VISIBLE
            binding2?.toolbarBtnBack?.visibility = View.VISIBLE
            binding?.accountBtnFollowSignout?.setOnClickListener {
                requestFollow()
            }
        }
        binding?.accountReyclerview?.adapter = UserFragmentRecyclerViewAdapter(binding)
        binding?.accountReyclerview?.layoutManager = GridLayoutManager(activity, 3)

        binding?.accountIvProfile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return binding.root
    }
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                binding?.accountTvFollowingCount?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                binding?.accountTvFollowerCount?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!) == true){
                    binding?.accountBtnFollowSignout?.text = getString(R.string.follow_cancel)
                    binding?.accountBtnFollowSignout?.background
                        ?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray),
                            PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid){
                        binding?.accountBtnFollowSignout?.text = getString(R.string.follow)
                        binding?.accountBtnFollowSignout?.background?.colorFilter = null
                    }

                }
            }
        }
    }
    fun requestFollow(){
        //Save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true

                transaction.set(tsDocFollowing,followDTO!!)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                //It remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount!! - 1
                followDTO?.followings!!.remove(uid)
            }else{
                //It add following third person when a third person do not follow me
                followDTO?.followingCount = followDTO?.followingCount!! + 1
                followDTO?.followings!![uid!!] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }

        //Save data to third account

        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid!!)){
                //It cancel my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        //var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        //FcmPush.instance.sendMessage(destinationUid,"Howlstagram",message)
    }
    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(binding.root.context!!).load(url).apply(RequestOptions().circleCrop()).into(binding?.accountIvProfile!!)

            }
        }
    }
    inner class UserFragmentRecyclerViewAdapter(val binding: FragmentUserBinding) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot == null) return@addSnapshotListener

                //Get data
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                binding.accountTvPostCount?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageview = ImageView(p0.context)
            var binding = FragmentUserBinding.inflate(LayoutInflater.from(p0.context),p0,false)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)


            //val layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            binding.accountReyclerview.setLayoutParams(imageview.layoutParams)

            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(val binding: FragmentUserBinding) : RecyclerView.ViewHolder(binding.root) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var imageview = (p0 as CustomViewHolder)
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview.binding.accountIvProfile)
        }

    }
}
