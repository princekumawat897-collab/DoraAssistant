package com.assistant.dora

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private lateinit var commandInput: EditText
    private lateinit var executeBtn: Button
    private lateinit var voiceBtn: Button
    private lateinit var permBtn: Button

    private val VOICE_REQ_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        statusText = findViewById(R.id.statusText)
        commandInput = findViewById(R.id.commandInput)
        executeBtn = findViewById(R.id.executeBtn)
        voiceBtn = findViewById(R.id.voiceBtn)
        permBtn = findViewById(R.id.permBtn)

        permBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Dora Service को ON करें", Toast.LENGTH_LONG).show()
        }

        executeBtn.setOnClickListener {
            val command = commandInput.text.toString().trim()
            if (command.isNotEmpty()) {
                processCommand(command)
            }
        }

        voiceBtn.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            try {
                startActivityForResult(intent, VOICE_REQ_CODE)
            } catch (e: Exception) {
                Toast.makeText(this, "Voice input support nahi mila", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQ_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val voiceText = result?.get(0) ?: ""
            commandInput.setText(voiceText)
            processCommand(voiceText)
        }
    }

    private fun processCommand(cmd: String) {
        val service = DoraAccessibilityService.instance
        if (service == null) {
            statusText.text = "Status: Accessibility Permission दीजिये!"
            speak("बॉस, पहले मुझे Accessibility Settings में जाकर परमिशन दें।")
            return
        }

        statusText.text = "Status: Dora काम कर रही है..."

        when {
            cmd.contains("click", ignoreCase = true) || cmd.contains("दबाओ", ignoreCase = true) -> {
                val target = cmd.replace("click", "", ignoreCase = true)
                                .replace("दबाओ", "", ignoreCase = true)
                                .replace("पर", "", ignoreCase = true).trim()
                val success = service.clickByText(target)
                finishTask(success)
            }

            cmd.contains("type", ignoreCase = true) || cmd.contains("लिखो", ignoreCase = true) -> {
                val textToType = cmd.replace("type", "", ignoreCase = true)
                                    .replace("लिखो", "", ignoreCase = true).trim()
                val success = service.typeTextInInput(textToType)
                finishTask(success)
            }

            else -> {
                val success = service.clickByText(cmd)
                finishTask(success)
            }
        }
    }

    private fun finishTask(isSuccess: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isSuccess) {
                statusText.text = "Status: टास्क पूरा हुआ"
                speak("बॉस, मैंने यह काम कर दिया है। क्या आपके लिए कोई और काम है?")
            } else {
                statusText.text = "Status: स्क्रीन पर एलिमेंट नहीं मिला"
                speak("माफ़ कीजियेगा बॉस, स्क्रीन पर यह ऑप्शन नहीं मिला।")
            }
        }, 1000)
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

