package com.karan.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_room_setup.*

lateinit var db : DatabaseReference
lateinit var auth : FirebaseAuth
private lateinit var listner : ValueEventListener
lateinit var roomid : String
lateinit var host : String
class RoomSetup : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_setup)
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("/bluffrooms")

        if(auth.uid == null) { auth.signInAnonymously() }
        listner = db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                btnjoin.setOnClickListener {

                    if(name.text.isEmpty()) {
                        Toast.makeText(this@RoomSetup, "Name Cannot be Blank, like your Brain :)", Toast.LENGTH_SHORT).show()
                    }else if(code.text.length!=4){
                        Toast.makeText(this@RoomSetup, "Are You DUMB or WHAT?", Toast.LENGTH_SHORT).show()
                    }else{
                        roomid = code.text.toString()
                        if (snapshot.child(roomid).exists() && snapshot.child(roomid).childrenCount > 1) {
                            if(snapshot.child("gamedata").child("inprogress").getValue().toString() =="1" ){
                                Toast.makeText(this@RoomSetup, "Game in Progress, wait for game to finish!", Toast.LENGTH_SHORT).show()
                            }else{
                                host =auth.uid!!
                                detachroomlistner()
                                db.child(roomid).child(auth.uid.toString()!!).child("name").setValue(name.text.toString())
                                db.child(roomid).child(auth.uid.toString()!!).child("state").setValue("0")
                                val intent = Intent(this@RoomSetup,Lobby::class.java)
                                startActivity(intent)
                            }
                        } else {
                            Toast.makeText(this@RoomSetup, "Room Not Found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                btncreate.setOnClickListener{
                    roomid = code.text.toString()//getrancar() + getrancar() + getrancar() + getrancar()
                    if(name.text.isEmpty())
                    {
                        Toast.makeText(this@RoomSetup, "Name Cannot be Blank, like your Brain :)", Toast.LENGTH_SHORT).show()
                    }else{
//                      while (snapshot.child(roomid).exists() && snapshot.child(roomid).childrenCount>1) {
//                      roomid = getrancar() + getrancar() + getrancar() + getrancar()
//                      }
                        if (snapshot.child(roomid).exists() && snapshot.child(roomid).childrenCount > 1) {
                            Toast.makeText(this@RoomSetup, "already exist", Toast.LENGTH_SHORT).show()
                        } else {
                            detachroomlistner()
                            db.child(roomid).removeValue()
                            db.child(roomid).child(auth.uid.toString()!!).child("name").setValue(name.text.toString())
                            db.child(roomid).child(auth.uid.toString()!!).child("state").setValue("2")
                            db.child(roomid).child("gamedata").child("deck").setValue(1)
                            db.child(roomid).child("gamedata").child("timeout").setValue(2)
                            db.child(roomid).child("gamedata").child("inprogress").setValue("0")
                            host = auth.uid.toString()
                            val intent = Intent(this@RoomSetup,Lobby::class.java)
                            intent.putExtra("roomid",roomid)
                            startActivity(intent)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RoomSetup, "error occured", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun detachroomlistner() {
        db.removeEventListener(listner)
    }

    private fun getrancar(): String {
        return (65..90).random().toChar().toString()
    }

}