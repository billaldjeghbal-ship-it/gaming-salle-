package com.gamingsalle.app

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.util.*
import kotlin.math.roundToInt

data class Device(
    val id: Int,
    var name: String,
    var type: String,
    var pricePerHour: Int,
    var running: Boolean = false,
    var startedAt: Long = 0L,
    var revenue: Int = 0
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("gaming_salle", MODE_PRIVATE) }
    private val devices = mutableListOf<Device>()
    private var expenses = 0
    private var sessions = 0
    private var hallName = "Gaming Salle"
    private lateinit var content: LinearLayout
    private lateinit var title: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val bg = Color.rgb(8,11,18)
    private val panel = Color.rgb(17,24,39)
    private val text = Color.rgb(245,247,250)
    private val muted = Color.rgb(156,168,186)
    private val accent = Color.rgb(22,185,255)
    private val success = Color.rgb(34,197,94)
    private val danger = Color.rgb(255,77,103)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        if (!prefs.contains("setup_done")) showSetup() else showMain()
        handler.post(object : Runnable {
            override fun run() {
                if (prefs.contains("setup_done")) refreshDashboard()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun load() {
        hallName = prefs.getString("hallName", "Gaming Salle") ?: "Gaming Salle"
        expenses = prefs.getInt("expenses", 0)
        sessions = prefs.getInt("sessions", 0)
        val count = prefs.getInt("deviceCount", 0)
        val prices = prefs.getString("prices", "")!!.split(",")
        val types = prefs.getString("types", "")!!.split(",")
        val names = prefs.getString("names", "")!!.split(",")
        for (i in 0 until count) {
            val p = prices.getOrNull(i)?.toIntOrNull() ?: 100
            devices.add(Device(i+1, names.getOrNull(i)?.ifBlank { "Poste ${i+1}" } ?: "Poste ${i+1}", types.getOrNull(i)?.ifBlank { "PS5" } ?: "PS5", p))
        }
    }

    private fun save() {
        prefs.edit()
            .putString("hallName", hallName)
            .putInt("expenses", expenses)
            .putInt("sessions", sessions)
            .putInt("deviceCount", devices.size)
            .putString("prices", devices.joinToString(",") { it.pricePerHour.toString() })
            .putString("types", devices.joinToString(",") { it.type })
            .putString("names", devices.joinToString(",") { it.name })
            .apply()
    }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(bg)
        setPadding(16, 18, 16, 12)
    }

    private fun tv(s: String, size: Float, color: Int = text): TextView = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        setPadding(4, 4, 4, 4)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(14, 12, 14, 12)
        background = GradientDrawable().apply { cornerRadius = 22f; setColor(panel) }
    }

    private fun button(s: String, onClick: () -> Unit): Button = Button(this).apply {
        text = s
        setTextColor(text)
        textSize = 14f
        background = GradientDrawable().apply { cornerRadius = 18f; setColor(accent) }
        setOnClickListener { onClick() }
    }

    private fun showSetup() {
        val root = base()
        root.gravity = Gravity.CENTER_HORIZONTAL
        val logo = ImageView(this)
        val res = resources.getIdentifier("gaming_salle_logo", "drawable", packageName)
        if (res != 0) logo.setImageResource(res)
        root.addView(logo, LinearLayout.LayoutParams(-1, 260))
        root.addView(tv("Gaming Salle", 30f), LinearLayout.LayoutParams(-2, -2))
        root.addView(tv("Gestion professionnelle de salle de jeux", 15f, muted))
        val name = EditText(this).apply { hint = "اسم القاعة"; setTextColor(text); setHintTextColor(muted) }
        val count = EditText(this).apply { hint = "عدد الأجهزة"; inputType = 2; setTextColor(text); setHintTextColor(muted) }
        val price = EditText(this).apply { hint = "السعر بالساعة (دج)"; inputType = 2; setTextColor(text); setHintTextColor(muted) }
        root.addView(name, LinearLayout.LayoutParams(-1, 60).apply { topMargin = 18 })
        root.addView(count, LinearLayout.LayoutParams(-1, 60))
        root.addView(price, LinearLayout.LayoutParams(-1, 60))
        root.addView(button("ابدأ إعداد القاعة") {
            hallName = name.text.toString().ifBlank { "Gaming Salle" }
            val n = (count.text.toString().toIntOrNull() ?: 10).coerceIn(1,100)
            val p = (price.text.toString().toIntOrNull() ?: 100).coerceAtLeast(0)
            devices.clear()
            repeat(n) { devices.add(Device(it+1, "Poste ${it+1}", "PS5", p)) }
            prefs.edit().putBoolean("setup_done", true).apply()
            save()
            showMain()
        }, LinearLayout.LayoutParams(-1, 58).apply { topMargin = 18 })
        setContentView(root)
    }

    private fun showMain() {
        val root = base()
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        title = tv(hallName, 24f)
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("⚙") { showSettings() }, LinearLayout.LayoutParams(60, 52))
        root.addView(header)
        val nav = LinearLayout(this)
        nav.addView(button("الرئيسية") { showMain() }, LinearLayout.LayoutParams(0,52,1f))
        nav.addView(button("الأجهزة") { showDevices() }, LinearLayout.LayoutParams(0,52,1f))
        nav.addView(button("التقارير") { showReports() }, LinearLayout.LayoutParams(0,52,1f))
        root.addView(nav)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        refreshDashboard()
    }

    private fun refreshDashboard() {
        if (!::content.isInitialized) return
        content.removeAllViews()
        val today = devices.sumOf { it.revenue + currentCharge(it) }
        val active = devices.count { it.running }
        val hours = devices.sumOf { currentMinutes(it) } / 60.0
        val row = LinearLayout(this)
        val stats = listOf(
            "مداخيل اليوم" to "${today} دج",
            "الأجهزة شغالة" to "$active / ${devices.size}",
            "الساعات المباعة" to String.format(Locale.US,"%.1f", hours),
            "الحصص" to sessions.toString()
        )
        stats.forEach { (a,b) ->
            val c=card(); c.addView(tv(a,12f,muted)); c.addView(tv(b,18f))
            row.addView(c, LinearLayout.LayoutParams(0,100,1f).apply { marginEnd=6 })
        }
        content.addView(row)
        content.addView(tv("الأجهزة",20f).apply { setPadding(4,20,4,10) })
        devices.forEach { addDeviceCard(it) }
    }

    private fun addDeviceCard(d: Device) {
        val c=card()
        val head=LinearLayout(this)
        val state=if(d.running) "● شغال" else "○ فارغ"
        val stateColor=if(d.running) success else muted
        head.addView(tv("${d.name} • ${d.type}",17f), LinearLayout.LayoutParams(0,-2,1f))
        head.addView(tv(state,13f,stateColor))
        c.addView(head)
        val charge=currentCharge(d)
        val mins=currentMinutes(d)
        c.addView(tv(if(d.running) "الوقت: ${formatMinutes(mins)}   |   الحساب: $charge دج" else "السعر: ${d.pricePerHour} دج / ساعة",14f,muted))
        val actions=LinearLayout(this)
        if(d.running) actions.addView(button("إيقاف") { stopDevice(d) }, LinearLayout.LayoutParams(0,50,1f))
        else actions.addView(button("بدء الحصة") { startDevice(d) }, LinearLayout.LayoutParams(0,50,1f))
        actions.addView(button("تعديل") { editDevice(d) }, LinearLayout.LayoutParams(0,50,1f).apply{marginStart=8})
        c.addView(actions)
        content.addView(c, LinearLayout.LayoutParams(-1,0).apply { height=150; bottomMargin=10 })
    }

    private fun currentMinutes(d: Device): Int = if (!d.running || d.startedAt==0L) 0 else ((System.currentTimeMillis()-d.startedAt)/60000L).toInt().coerceAtLeast(0)
    private fun currentCharge(d: Device): Int = if (!d.running) 0 else ((currentMinutes(d)/60.0)*d.pricePerHour).roundToInt()
    private fun formatMinutes(m:Int) = "%02d:%02d".format(m/60,m%60)

    private fun startDevice(d:Device) { d.running=true; d.startedAt=System.currentTimeMillis(); sessions++; save(); refreshDashboard() }
    private fun stopDevice(d:Device) { d.revenue += currentCharge(d); d.running=false; d.startedAt=0L; save(); refreshDashboard() }

    private fun editDevice(d:Device) {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(30,10,30,5) }
        val name=EditText(this); name.setText(d.name); name.setTextColor(text); box.addView(name)
        val price=EditText(this); price.setText(d.pricePerHour.toString()); price.inputType=2; price.setTextColor(text); box.addView(price)
        val type=EditText(this); type.setText(d.type); type.setTextColor(text); box.addView(type)
        AlertDialog.Builder(this).setTitle("تعديل الجهاز").setView(box)
            .setPositiveButton("حفظ"){_,_-> d.name=name.text.toString().ifBlank{d.name}; d.type=type.text.toString().ifBlank{d.type}; d.pricePerHour=price.text.toString().toIntOrNull()?:d.pricePerHour; save(); refreshDashboard()}
            .setNegativeButton("إلغاء",null).show()
    }

    private fun showDevices() {
        showMain()
        content.removeAllViews()
        content.addView(tv("إدارة الأجهزة",24f))
        content.addView(tv("يمكنك تعديل الاسم والنوع والسعر لكل جهاز.",14f,muted))
        devices.forEach { addDeviceCard(it) }
        content.addView(button("+ إضافة جهاز"){
            val id=devices.size+1
            devices.add(Device(id,"Poste $id","PS5",100)); save(); showDevices()
        })
    }

    private fun showReports() {
        showMain()
        content.removeAllViews()
        val revenue=devices.sumOf{it.revenue+currentCharge(it)}
        content.addView(tv("التقارير",24f))
        val c=card()
        c.addView(tv("إجمالي المداخيل المسجلة",14f,muted))
        c.addView(tv("$revenue دج",28f))
        c.addView(tv("المصاريف: $expenses دج",15f,muted))
        c.addView(tv("الربح التقريبي: ${revenue-expenses} دج",19f, if(revenue-expenses>=0)success else danger))
        c.addView(tv("عدد الحصص: $sessions",15f,muted))
        content.addView(c)
        content.addView(tv("ملاحظة: النسخة الحالية تحفظ البيانات محلياً على الهاتف.",13f,muted).apply{setPadding(4,20,4,4)})
    }

    private fun showSettings() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(25,10,25,5) }
        val name=EditText(this); name.setText(hallName); name.setTextColor(text); box.addView(name)
        val exp=EditText(this); exp.setHint("المصاريف"); exp.setText(expenses.toString()); exp.inputType=2; exp.setTextColor(text); box.addView(exp)
        AlertDialog.Builder(this).setTitle("إعدادات Gaming Salle").setView(box)
            .setPositiveButton("حفظ"){_,_-> hallName=name.text.toString().ifBlank{"Gaming Salle"}; expenses=exp.text.toString().toIntOrNull()?:expenses; save(); showMain()}
            .setNeutralButton("مسح كل البيانات"){_,_-> prefs.edit().clear().apply(); devices.clear(); expenses=0; sessions=0; showSetup()}
            .setNegativeButton("إلغاء",null).show()
    }

    override fun onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null) }
}
