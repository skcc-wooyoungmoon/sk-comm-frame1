# kotlinx.serialization 이 생성한 Serializer 를 보존한다.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.sk.autotrader.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.sk.autotrader.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit 인터페이스의 제네릭 시그니처 보존
-keepattributes Signature
-keep,allowobfuscation interface com.sk.autotrader.data.remote.KisApi
