package com.usaip.gpt_guru
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.shrikanthravi.chatview.data.Message
import com.shrikanthravi.chatview.widget.ChatView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    val demoConversation:String = "User:Hey buddy!\n" +
            "AI: Hello good sir!\n" +
            "User: Heya! Should we go to a movie?\n" +
            "AI: Movie! omg am I your valentine?\n" +
            "User: No smh. You're my best friend.\n" +
            "AI: awww! Thank you!" + //you can update the responses to suit your needs

            "\n\n###\n\n" // add more examples with this as pad token
    private lateinit var volleyRequestQueue: RequestQueue
    private lateinit var mChatView: ChatView
    var conversation:String = "User:"
    fun getTextFromResponse(json:JSONObject):String{
        val arr:JSONArray = json.get("choices") as JSONArray
        val res = arr[0] as JSONObject
        var text = res.get("text") as String
        Log.d("AI convo",res.toString())
        text = text.replace("\n\n","\n")
        text = text.split("User:")[0].split("AI:")[0]
        if(text[text.length - 1] == '\n'){
            text = text.substring(0,text.length - 1)
        }
        return text
    }
    fun displayResponse(json: JSONObject){
        //uses the response text to create a chatview message and display it
        val text:String = getTextFromResponse(json)
        conversation += "$text\nUser:"
        val message = Message()
        message.type = Message.LeftSimpleMessage
        message.userName = "AI"
        message.body = text
        mChatView.addMessage(message)
    }
    fun conveyError(error:VolleyError){
        Toast.makeText(this,error.toString(),Toast.LENGTH_LONG).show()
    }
    fun communicate(text: String,engine: String = "davinci"){
        //sends request to OpenAI
        val json = JSONObject()
        json.put("prompt",demoConversation + text)
        json.put("max_tokens",30)
        json.put("temperature",0.9)
        json.put("presence_penalty",1.5)
        json.put("frequency_penalty",1.5)
        val url =  "https://api.openai.com/v1/engines/$engine/completions"
        val req = object: JsonObjectRequest(Method.POST,url,json,
            Response.Listener {
                response -> displayResponse(response)
            },
            Response.ErrorListener {
                error -> conveyError(error)
            }
        )
        {
            override fun getHeaders():Map<String,String>{
                val headers = HashMap<String,String>()
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val api_key = sharedPreferences.getString("openai_key","")
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $api_key"
                return headers
            }
        }
        volleyRequestQueue.add(req)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        volleyRequestQueue = Volley.newRequestQueue(this)
        volleyRequestQueue.start()
        mChatView = findViewById<View>(R.id.chat_view) as ChatView
        mChatView.setOnClickSendButtonListener {
            chat ->
            val message = Message()
                message.body = chat
                message.userName = "User"
                message.type = Message.RightSimpleMessage

            mChatView.addMessage(message)
            conversation += "$chat\nAI:"
            communicate(conversation)
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_settings -> {
                val intent = Intent(this,SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}