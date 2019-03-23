package com.testisim.activities.test

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.testisim.com.testisim.R
import kotlinx.android.synthetic.main.activity_test_decision.*
import com.google.gson.Gson
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.testisim.activities.MainActivity
import com.testisim.models.TestModel
import com.testisim.models.TestModelList
import com.testisim.singletons.TestisimKeyStore
import android.widget.Toast
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class TestDecisionActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private var fromHandFreeMode: Boolean = false
    private val CODE_SPEECH_INPUT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_decision)

        fromHandFreeMode = intent.getBooleanExtra(MainActivity.IS_COMES_FROM_VOICE_KEY, false)

        testDecisionBackButton.setOnClickListener {
            onBackPressed()
        }

        testDecisionNoButton.setOnClickListener {
            setDataToSharedPref(TestResultType.No)
            openTestResultActivity(TestResultType.No)
        }

        testDecisionYesButton.setOnClickListener {
            setDataToSharedPref(TestResultType.Yes)
            openTestResultActivity(TestResultType.Yes)
        }

        if (fromHandFreeMode) {
            initTTS()
        }
    }

    override fun onStart() {
        super.onStart()

        Handler().postDelayed({
            if (fromHandFreeMode) {
                speak()
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (fromHandFreeMode) {
            tts.stop()
            tts.shutdown()
        }
    }

    override fun onStop() {
        super.onStop()
        if (fromHandFreeMode) {
            tts.stop()
            tts.shutdown()
        }
    }

    private fun initTTS() {

        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {

            if (it == TextToSpeech.LANG_MISSING_DATA || it == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Error occurred while initializing Text-To-Speech engine", Toast.LENGTH_LONG).show()
            }
        })

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onError(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                startSTT()
            }

            override fun onStart(utteranceId: String?) {}
        })

        tts.language = Locale("tr", "TR")
    }

    private fun startSTT() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Hi speak something")
        try {
            startActivityForResult(intent, CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
        }
    }

    private fun speak() {

        val text = "${testDecisionText.text}"
        val textToSpeak = " $text Devam etmek için devam diyebilir yada sonlandır diyerek ana sayfaya dönebilirsin."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TEST")
        } else {
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                    if (result[0].toLowerCase().contains("evet")) {
                        setDataToSharedPref(TestResultType.Yes)
                        openTestResultActivity(TestResultType.Yes)

                    } else if (result[0].toLowerCase().contains("hayır")) {
                        setDataToSharedPref(TestResultType.No)
                        openTestResultActivity(TestResultType.No)
                    }
                }
            }
        }
    }

    private fun setDataToSharedPref(testResultType: TestResultType) {

        val mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        var listOfTests = TestModelList(arrayListOf())
        val isTestResultYes = testResultType == TestResultType.Yes

        val df = SimpleDateFormat("EEE, d MMM yyyy, HH:mm")
        val date = df.format(Calendar.getInstance().getTime())

        try {

            val json = mPrefs.getString(TestisimKeyStore.TESTS_KEY, "")
            val test = Gson().fromJson<TestModelList>(json, TestModelList::class.java)
            listOfTests = test.copy()

        } catch (e: Exception) {
            println("Exception")
        }

        listOfTests.listOfTest.add(TestModel(date = date, isSuccess = isTestResultYes))

        val prefsEditor = mPrefs.edit()
        val json = Gson().toJson(listOfTests)
        prefsEditor.putString(TestisimKeyStore.TESTS_KEY, json)
        prefsEditor.apply()
    }

    private fun openTestResultActivity(testResultType: TestResultType) {
        val intent = Intent(this, TestResultActivity::class.java)
        intent.putExtra(TEST_DECISION, testResultType.ordinal)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        finish()
    }

    companion object {
        const val TEST_DECISION = "testDecisionKey"
    }
}
