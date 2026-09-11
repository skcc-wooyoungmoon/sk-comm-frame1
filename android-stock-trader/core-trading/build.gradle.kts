plugins {
    alias(libs.plugins.kotlin.jvm)
}

// 매매 로직은 안드로이드에 의존하지 않는 순수 Kotlin 모듈로 둔다.
// JVM에서 바로 테스트할 수 있어 에뮬레이터 없이 전략/리스크 회귀 검증이 가능하다.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
