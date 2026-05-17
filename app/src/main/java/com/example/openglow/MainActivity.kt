package com.example.openglow

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.openglow.ui.theme.OpenGlowTheme

class MainActivity : ComponentActivity() {
    private var isNotificationAccessEnabled by mutableStateOf(false)
    private var openedNotificationSettingsOnLaunch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isNotificationAccessEnabled = isNotificationListenerEnabled()

        setContent {
            OpenGlowTheme {
                NotificationPermissionScreen(
                    isNotificationAccessEnabled = isNotificationAccessEnabled,
                    onOpenSettingsClick = ::openNotificationAccessSettings,
                )
            }
        }

        openNotificationAccessSettingsOnLaunchIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        isNotificationAccessEnabled = isNotificationListenerEnabled()
    }

    private fun openNotificationAccessSettingsOnLaunchIfNeeded() {
        if (openedNotificationSettingsOnLaunch || isNotificationListenerEnabled()) {
            return
        }

        openedNotificationSettingsOnLaunch = true
        openNotificationAccessSettings()
    }

    private fun openNotificationAccessSettings() {
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
}

@Composable
private fun NotificationPermissionScreen(
    isNotificationAccessEnabled: Boolean,
    onOpenSettingsClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "알림 로그 확인 앱",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (isNotificationAccessEnabled) {
                    "권한 상태: 알림 접근 권한이 활성화되어 있습니다."
                } else {
                    "권한 상태: 알림 접근 권한이 필요합니다."
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isNotificationAccessEnabled) {
                    "알림 수신 대기 중입니다.\n문자, 카카오톡, 이메일 알림이 도착하면 화면에는 표시하지 않고 Logcat에만 출력합니다."
                } else {
                    "이 앱은 문자, 카카오톡, 이메일 알림 구조를 로그로 확인하기 위해 알림 접근 권한이 필요합니다.\n알림 내용은 화면에 표시하지 않고 Logcat으로만 출력합니다."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSettingsClick,
            ) {
                Text(text = "알림 접근 권한 설정하기")
            }
        }
    }
}
