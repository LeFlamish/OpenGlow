package com.example.openglaw

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var guideTextView: TextView
    private var openedNotificationSettingsOnLaunch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        setContentView(createContentView())
        openNotificationAccessSettingsOnLaunchIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun createContentView(): LinearLayout {
        val density = resources.displayMetrics.density
        val horizontalPadding = (24 * density).toInt()
        val verticalPadding = (28 * density).toInt()

        statusTextView = TextView(this).apply {
            textSize = 18f
            setTextColor(getColor(android.R.color.black))
        }

        guideTextView = TextView(this).apply {
            textSize = 15f
            setTextColor(getColor(android.R.color.darker_gray))
            setLineSpacing(4f, 1f)
        }

        val titleTextView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setTextColor(getColor(android.R.color.black))
        }

        val settingButton = Button(this).apply {
            text = "알림 접근 권한 설정하기"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            setBackgroundColor(getColor(android.R.color.white))

            addView(titleTextView, matchWrapLayoutParams())
            addSpacer(18)
            addView(statusTextView, matchWrapLayoutParams())
            addSpacer(12)
            addView(guideTextView, matchWrapLayoutParams())
            addSpacer(22)
            addView(settingButton, matchWrapLayoutParams())
        }
    }

    private fun updatePermissionStatus() {
        val enabled = isNotificationListenerEnabled()

        statusTextView.text = if (enabled) {
            "권한 상태: 알림 접근 권한이 활성화되어 있습니다."
        } else {
            "권한 상태: 알림 접근 권한이 필요합니다."
        }

        guideTextView.text = if (enabled) {
            "알림 수신 대기 중입니다.\n문자, 카카오톡, 이메일 알림이 도착하면 화면에는 표시하지 않고 Logcat에만 출력합니다."
        } else {
            "이 앱은 문자, 카카오톡, 이메일 알림 구조를 로그로 확인하기 위해 알림 접근 권한이 필요합니다.\n알림 내용은 화면에 표시하지 않고 Logcat으로만 출력합니다."
        }
    }

    private fun openNotificationAccessSettingsOnLaunchIfNeeded() {
        if (openedNotificationSettingsOnLaunch || isNotificationListenerEnabled()) {
            return
        }

        openedNotificationSettingsOnLaunch = true
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(this, NotificationLogListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        ) ?: return false

        return enabledListeners.split(":").any { flatName ->
            val enabledComponent = ComponentName.unflattenFromString(flatName)
            enabledComponent == componentName ||
                TextUtils.equals(enabledComponent?.packageName, packageName)
        }
    }

    private fun LinearLayout.addSpacer(dp: Int) {
        val height = (dp * resources.displayMetrics.density).toInt()
        addView(
            TextView(context),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
            ),
        )
    }

    private fun matchWrapLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }
}
