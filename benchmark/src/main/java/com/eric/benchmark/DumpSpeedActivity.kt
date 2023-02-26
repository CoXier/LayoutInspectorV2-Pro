package com.eric.benchmark

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

class DumpSpeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dump_speed)

        val array = intArrayOf(3000, 4000, 5000, 6000)
        findViewById<ViewGroup>(R.id.root).apply {

            array.forEach {count->
                val button = Button(this@DumpSpeedActivity)
                addView(button, LinearLayout.LayoutParams(720, 120))
                button.text = "add $count views"
                button.setOnClickListener {
                    addView(count)
                }
            }

        }
    }

    private fun addView(count: Int) {
        findViewById<ViewGroup>(R.id.container).apply {
            this.removeAllViews()
            for (i in 0 until count) {
                addView(View(this@DumpSpeedActivity))
            }
        }
    }
}