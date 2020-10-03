package com.karan.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_game_bluff.*
import kotlinx.android.synthetic.main.activity_lobby.*
import kotlinx.android.synthetic.main.card_rsr.view.*
import kotlinx.android.synthetic.main.player_rsr.view.*
import java.util.*


private lateinit var gamedatalistner : ValueEventListener
private  var playercount= 0
private var deck =0
private var dis = ""
private var sel = ""




class GameBluff : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_bluff)


        db = FirebaseDatabase.getInstance().getReference("/bluffrooms").child(roomid)
        auth = FirebaseAuth.getInstance()

        val scard = resources.getStringArray(R.array.cards)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, scard)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                sel = scard[pos]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                sel = ""
            }
        }

        gamedatalistner =
            db.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val adapter = GroupAdapter<GroupieViewHolder>()
                    playercount = (snapshot.childrenCount - 1).toInt()



                    deck = snapshot.child("gamedata").child("deck").getValue().toString().toInt()

                    if (snapshot.child(auth.uid!!).child("state")
                            .getValue() == "2" && snapshot.child("gamedata/dealt").getValue().toString() == "0") {
                        db.child("gamedata").child("cards").setValue(shufle(deck))
                        val cards = snapshot.child("gamedata").child("cards").getValue().toString()
                        var f = 0
                        val t = cards.length / playercount
                        var rem = cards.length % playercount
                        var i = 0
                        db.child("gamedata").child("turn").setValue(0)
                        snapshot.children.forEach {
                            if (it.key != "gamedata") {
                                db.child(it.key!!).child("cards").setValue(cards.substring(f, f + t + if (rem > 0) { 1 } else { 0 }))
                                f += t + if (rem > 0) { 1 } else { 0 }
                                rem -= 1
                                db.child(it.key!!).child("turn").setValue(i++)
                            }
                        }
                        gamedata.child("dealt").setValue("1")
                        gamedata.child("cards").removeValue()
                    }
                    snapshot.children.forEach {
                        if (it.key != "gamedata") {
                            it.key
                        }
                    }

                    var car =snapshot.child(auth.uid!!).child("cards").getValue().toString()
                    showcards(car)

                    val myturn = snapshot.child("gamedata").child("turn")
                        .getValue() == snapshot.child(auth.uid!!).child("turn").getValue()
                    if (myturn) {
                        val turn = snapshot.child(auth.uid!!).child("turn").getValue().toString().toInt()
                        if(!snapshot.child(auth.uid!!).child("cards").exists() || car.length ==0){
                            pass.isPressed = true
                        }

                        discard.visibility = View.VISIBLE
                        pass.visibility = View.VISIBLE
                        check.visibility = View.VISIBLE

                        knowncardtype.text = snapshot.child("gamedata/lastcardtype").getValue().toString()

                        val stackcards = if(snapshot.child("gamedata/stack").exists()){snapshot.child("gamedata/stack").getValue().toString()}else{""}
                        val mohis = if(snapshot.child("gamedata/mohis").exists()){snapshot.child("gamedata/mohis").getValue().toString()}else{""}
                        val lst = snapshot.child("gamedata").child("lastcardtype").getValue().toString()
                        discard.setOnClickListener {
                            if(dis.length !=0){
                                car = snapshot.child(auth.uid!!).child("cards").getValue().toString()
                                car = substi(car,dis)
                                if(snapshot.child("gamedata").child("lastcardtype").exists()){
                                    gamedata.child("checkstat").setValue(checkstat(dis,lst))
                                }else{
                                    gamedata.child("lastcardtype").setValue(sel)
                                    gamedata.child("checkstat").setValue(checkstat(dis,sel))
                                }
                                db.child(auth.uid!!).child("cards").setValue(car)
                                gamedata.child("stack").setValue(stackcards+dis)
                                gamedata.child("mohis").setValue(mohis+"D")
                                dis = ""
                                db.child("gamedata/lp").setValue(auth.uid)
                                db.child("gamedata").child("turn").setValue(turn + 1)
                                db.child(auth.uid!!).child("turn").setValue(turn + playercount)

                            }else{
                                pass.isPressed = true
                            }
                        }
                        check.setOnClickListener {

                            val lp = snapshot.child("gamedata/lp").getValue().toString()
                            if(snapshot.child("gamedata/checkstat").getValue().toString() == "1"){
                                db.child(auth.uid!!).child("cards").setValue(car + stackcards)
                                db.child("gamedata/stack").setValue("")
                                gamedata.child("lastcardtype").setValue("")
                                // take care of turns
                                db.child("gamedata/turn").setValue(turn - 1)
                                db.child(lp).child("turn").setValue(turn- playercount)
                            }else{
                                val hiscards = snapshot.child(lp).child("cards").getValue().toString()
                                db.child(lp).child("cards").setValue( hiscards+ stackcards)
                                db.child("gamedata/stack").setValue("")
                                gamedata.child("lastcardtype").setValue("")
                            }
                        }
                        pass.setOnClickListener {

                            gamedata.child("mohis").setValue(mohis+"P")
                            if(mohis.takeLast(playercount) == "P".repeat(playercount)){
                                db.child("gamedata/abandoned").setValue(stackcards)
                                db.child("gamedata/stack").setValue("")
                            }
                            db.child("gamedata").child("turn").setValue(turn + 1)
                            db.child(auth.uid!!).child("turn").setValue(turn + playercount)
                        }
                    }
                    abandoned.text =
                        "Abandoned : " + (snapshot.child("gamedata/abandoned").getValue().toString().length).toString()
                    discarded.text =
                        "Discarded : " + (snapshot.child("gamedata/stack").getValue().toString().length).toString()
                    snapshot.children.forEach {
                        if (it.key != "gamedata") {
                            var name = it.child("name").getValue().toString()
                            val cardno = it.child("cards").getValue().toString().length.toString()
                            if (it.key == auth.uid) {
                                name = name + "(you)"
                            }
                            adapter.add(Player(name, cardno))
                            if (it.child("turn").getValue() == snapshot.child("gamedata").child("turn")
                                    .getValue()
                            ) {
                                turnshow.text = "Turn : " + name
                            }
                        }
                        players.adapter = adapter
                    }


                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

    }

    private fun checkstat(dis: String, type: String): String {
        var s = "1"
        for(i in dis.toCharArray()){
            if(i.toString() != type){s = "0"}
        }
        return s
    }


    class Player(val name: String, val cardsrem: String): Item<GroupieViewHolder>() {
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.name.text = name +" : " + cardsrem
        }

        override fun getLayout(): Int {
            return R.layout.player_rsr
        }
    }

    class Card(private val s: String):Item<GroupieViewHolder>(){
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.cardtext.text = s
        }
        override fun getLayout(): Int {
            return R.layout.card_rsr
        }

    }

    fun showcards(card: String) {
        val disadapter = GroupAdapter<GroupieViewHolder>()
        val adapter = GroupAdapter<GroupieViewHolder>()
        for (c in card.toCharArray().sorted()) {
            adapter.add(Card(c.toString()))
        }
        adapter.setOnItemClickListener { item, view ->
            adapter.remove(item)
            disadapter.add(item)
            dis += view.cardtext.text.toString()
            test.text  = dis
        }
        disadapter.setOnItemClickListener { item, view ->
            disadapter.remove(item)
            adapter.add(item)
            dis = substi(dis,view.cardtext.text.toString())
            test.text  = dis
        }
        mycardsrecycler.adapter = adapter
        discardsrecycler.adapter = disadapter
    }


    private fun shufle(d: Int) : String {
        val set = "A23456789TJQK"
        var pile= set.repeat(d * 4)
        val characters: MutableList<Char> = ArrayList()
        for (c in pile.toCharArray()) {
            characters.add(c)
        }
        val output = StringBuilder(pile.length)
        while (characters.size != 0) {
            val randPicker = (Math.random() * characters.size).toInt()
            output.append(characters.removeAt(randPicker))
        }
        return output.toString()
    }




    override fun onBackPressed() {
        showdailog()
    }

    fun substi(a:String, b:String) : String {
        val set = "A23456789TJQK"
        val map = mutableMapOf<Char,Int>()
        for(i in set.toCharArray()){
            map[i] = 0
        }
        for(i in a.toCharArray()){
            map[i]=map[i]!!+1
        }
        for(i in b.toCharArray()){
            map[i] = map[i]!!-1
        }
        var string = ""
        for(i in set.toCharArray()){
            string += (i.toString()).repeat(map[i]!!)
        }
        return string
    }

    private fun showdailog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Leave Game?")
        builder.setMessage("Are you sure you want to leave the game? It will remove you from the Room.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){ _, _ ->
            db.removeEventListener(gamedatalistner)
            db.child(auth.uid!!).removeValue()
            val intent = Intent(this, RoomSetup::class.java)
            startActivity(intent)
        }
        builder.setNeutralButton("Cancel"){ _, _ ->
            Toast.makeText(this@GameBluff, "Clicked cancel", Toast.LENGTH_LONG).show()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

}