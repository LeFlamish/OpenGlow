package com.example.openglow

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// 🌟 이 부분들이 누락되면 'by' 사용 시 컴파일 에러가 발생합니다.
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.openglow.ui.theme.OpenGlowTheme
import dagger.hilt.android.AndroidEntryPoint

// 💡 분리해 둔 화면들
import com.example.openglow.ui.screens.SplashScreen
import com.example.openglow.ui.screens.MainDashboardScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // compose state로 관리하여 값이 변하면 UI가 자동으로 재구성되도록 합니다.
    private var isNotificationAccessEnabled by mutableStateOf(false)
    private var openedNotificationSettingsOnLaunch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 초기 권한 상태 확인
        isNotificationAccessEnabled = isNotificationListenerEnabled()

        setContent {
            OpenGlowTheme {
                val navController = rememberNavController()

                // 권한 상태에 따라 시작 지점 결정
                val startDestination = if (isNotificationAccessEnabled) "splash" else "permission"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    // 1. 알림 권한 요청 화면
                    composable("permission") {
                        NotificationPermissionScreen(
                            isNotificationAccessEnabled = isNotificationAccessEnabled,
                            onOpenSettingsClick = ::openNotificationAccessSettings,
                            onGoToMainClick = {
                                navController.navigate("splash") {
                                    popUpTo("permission") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. 스플래시 화면
                    composable("splash") {
                        SplashScreen(navController = navController)
                    }

                    // 3. 메인 대시보드 화면
                    composable("main") {
                        MainDashboardScreen()
                    }
                }
            }
        }

        openNotificationAccessSettingsOnLaunchIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // 설정 화면에서 돌아왔을 때 권한 상태를 다시 체크하여 UI를 업데이트합니다.
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
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            // 일부 기기에서 해당 설정 페이지가 없을 경우를 대비
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        // NotificationLogListenerService::class.java가 빨간색으로 뜬다면
        // 해당 파일의 package com.example.openglow 선언이 맞는지 다시 확인해주세요.
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
    onGoToMainClick: () -> Unit,
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
                    "알림 수신 대기 중입니다.\n메시지가 도착하면 Logcat을 확인하세요."
                } else {
                    "앱의 기능을 사용하려면 알림 접근 권한이 필요합니다."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(22.dp))

            if (isNotificationAccessEnabled) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGoToMainClick,
                ) {
                    Text(text = "앱 시작하기")
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSettingsClick,
                ) {
                    Text(text = "알림 접근 권한 설정하기")
                }
            }
        }
    }
}