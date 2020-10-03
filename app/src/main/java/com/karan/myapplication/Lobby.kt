package com.karan.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_lobby.*
import kotlinx.android.synthetic.main.player_rsr.view.*

private lateinit var datalistner : ValueEventListener
lateinit var gamedata : DatabaseReference

class Lobby : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        roomidview.text= roomid
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("/bluffrooms").child(roomid!!)
        db.child(auth.uid!!).onDisconnect().removeValue()
        gamedata = FirebaseDatabase.getInstance().getReference("/bluffrooms").child(roomid!!).child("gamedata")
        datalistner =
            db.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val adapter = GroupAdapter<GroupieViewHolder>()
                    var playercount  = snapshot.childrenCount - 1
                    var tim = snapshot.child("gamedata").child("timeout").value.toString().toLong()
                    timeoutview.text= tim!!.times(20).toString() +"s"
                    var dec = snapshot.child("gamedata").child("deck").value.toString().toLong()
                    deckview.text= dec.toString()
                    var allready : Boolean = true
                    var hostpresent = false
                    if(snapshot.child("gamedata").child("inprogress").getValue()=="1"){
                        playgame()
                    }
                    snapshot.children.forEach{
                        if(it.key != "gamedata"){
                            val name = it.child("name").getValue().toString()
                            when(val readystat = it.child("state").getValue().toString()){
                                "0"->{
                                    allready = false
                                    adapter.add(Player(name,readystat))
                                }
                                "1"->{
                                    adapter.add(Player(name,readystat))
                                }
                                "2"->{
                                    hostpresent = true
                                    host = it.key.toString()
                                    adapter.add(Player(name,readystat))
                                }
                            }
                        }
                    }
                    playerlist.adapter = adapter
                    if(!hostpresent){
                        db.child(auth.uid.toString()).child("state").setValue("2")
                    }
                    if(host==auth.uid) {
                        settingsvisible()
                    }else{
                        startgame.text = "Get Ready"
                        startgame.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                    }
                    startgame.setOnClickListener {
                        if(host==auth.uid) {
                            if(allready){
                                detachlistners()
                                gamedata.child("inprogress").setValue("1")
                                gamedata.child("dealt").setValue("0")
                                gamedata.child("turn").setValue(0)
                                val intent = Intent(this@Lobby,GameBluff::class.java)
                                startActivity(intent)
                                finish()
                            }else{
                                Toast.makeText(this@Lobby, "Wait for everyone to be ready", Toast.LENGTH_LONG).show()
                            }
                        }else{
                            if(startgame.text=="Get Ready"){
                                db.child(auth.uid.toString()!!).child("state").setValue("1")
                                startgame.text = "Not Ready"
                                startgame.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
                            }
                            else{
                                db.child(auth.uid.toString()!!).child("state").setValue("0")
                                startgame.text = "Get Ready"
                                startgame.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                            }
                        }
                    }
                    timinc.setOnClickListener {
                        if(tim<=5) {
                            gamedata.child("timeout").setValue(tim +1)
                        }
                    }
                    timdec.setOnClickListener {
                        if(tim!!>=3) {
                            gamedata.child("timeout").setValue(tim-1)
                        }
                    }
                    decinc.setOnClickListener {
                        if(dec!!<=3) {
                            gamedata.child("deck").setValue(dec+1)
                        }
                    }
                    decdec.setOnClickListener {
                        if(dec!!>=2) {
                            gamedata.child("deck").setValue(dec-1)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Lobby, "Something Went wrong with Players!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun playgame() {
        detachlistners()
        val intent = Intent(this@Lobby,GameBluff::class.java)
        startActivity(intent)
        finish()
    }

    private fun detachlistners() {
        val ref = FirebaseDatabase.getInstance().getReference("/bluffrooms").child(roomid!!)
        ref.removeEventListener(datalistner)
    }

    private fun settingsvisible() {
        decinc.visibility =View.VISIBLE
        decdec.visibility =View.VISIBLE
        timinc.visibility =View.VISIBLE
        timdec.visibility =View.VISIBLE
        startgame.text = "Start Game"
        startgame.visibility = View.VISIBLE
    }

    class Player(val name : String,val ready: String): Item<GroupieViewHolder>() {
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.name.text = name
            when(ready){
                "0"->{
                    viewHolder.itemView.ready0.visibility = View.VISIBLE
                    viewHolder.itemView.ready1.visibility = View.GONE
                    viewHolder.itemView.ready2.visibility = View.GONE
                }
                "1"->{
                    viewHolder.itemView.ready1.visibility = View.VISIBLE
                    viewHolder.itemView.ready0.visibility = View.GONE
                    viewHolder.itemView.ready2.visibility = View.GONE
                }
                "2"->{
                    viewHolder.itemView.ready2.visibility = View.VISIBLE
                    viewHolder.itemView.ready1.visibility = View.GONE
                    viewHolder.itemView.ready0.visibility = View.GONE
                }
            }
        }
        override fun getLayout(): Int {
            return R.layout.player_rsr
        }
    }

    override fun onBackPressed() {
        showdailog()
    }

    private fun showdailog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Leave Game?")
        builder.setMessage("Are you sure you want to leave the Room?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){ _, _ ->
            detachlistners()
            db.child(auth.uid!!).removeValue()
            val intent = Intent(this,RoomSetup::class.java)
            startActivity(intent)
        }
        builder.setNeutralButton("Cancel"){ _, _ ->
            Toast.makeText(this@Lobby,"Clicked cancel",Toast.LENGTH_LONG).show()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()

    }

}

