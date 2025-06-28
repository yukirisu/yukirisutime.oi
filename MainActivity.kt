package com.example.appinfo

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // レイアウトにWebViewだけを置いていると仮定
        webView = WebView(this)
        setContentView(webView)

        // 権限がなければ設定画面へ
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            val usageData = getUsageStats()
            val html = generateHtml(usageData)
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun getUsageStats(): List<UsageStats> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24 // 過去24時間
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
    }

    private fun generateHtml(usageStats: List<UsageStats>): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append(
            """
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: sans-serif; padding: 16px; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                </style>
            </head>
            <body>
                <h2>利用履歴 (過去24時間)</h2>
                <table>
                    <tr><th>アプリ</th><th>起動時間</th><th>最終使用</th></tr>
            """
        )

        for (stat in usageStats) {
            if (stat.totalTimeInForeground > 0) {
                sb.append("<tr>")
                sb.append("<td>${stat.packageName}</td>")
                sb.append("<td>${stat.totalTimeInForeground / 1000}s</td>")
                sb.append("<td>${dateFormat.format(Date(stat.lastTimeUsed))}</td>")
                sb.append("</tr>")
            }
        }

        sb.append("</table>")
        sb.append("<p>※健康情報取得サンプルは別途Google Fit API連携が必要</p>")
        sb.append("</body></html>")
        return sb.toString()
    }
}
