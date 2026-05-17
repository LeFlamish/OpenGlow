package com.example.openglaw

/**
 * 알림 구조 확인 대상 앱 패키지 목록입니다.
 *
 * 기기 제조사, 기본 앱, 앱 버전에 따라 packageName이 달라질 수 있으므로
 * 새 대상이 필요하면 이 목록만 수정하면 됩니다.
 */
object NotificationTargetPackages {
    val targetPackages = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.kakao.talk",
        "com.google.android.gm",
        "com.samsung.android.email.provider",
        "com.microsoft.office.outlook",
    )
}
