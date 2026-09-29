import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 루트는 공통 설정만 가진다. 코드는 두 모듈에 있다.
// - chat-api: 실행 모듈(컨트롤러·필터·Feign/Kafka 어댑터). bootJar 는 여기서만 나온다.
// - chat-application: 라이브러리 모듈(유스케이스·서비스·엔티티·ro/rw 저장소·JPA 설정).
plugins {
    id("org.springframework.boot") version "3.4.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    kotlin("plugin.jpa") version "2.1.10" apply false
    kotlin("kapt") version "2.1.10" apply false
}

// plugins 블록의 코틀린 버전과 같게 둔다.
val kotlinVersion = "2.1.10"

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // 실행 모듈(chat-api)만 자기 build.gradle.kts 에서 org.springframework.boot 플러그인을 적용한다.
    // 라이브러리 모듈은 Boot BOM 만 가져온다.
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Boot BOM 은 코틀린 1.9.25 를 관리한다. Boot 플러그인을 적용한 모듈은 코틀린 플러그인 버전에 자동으로 맞춰지지만
    // BOM 만 가져오는 라이브러리 모듈은 그렇지 않아, 컴파일러 도구·stdlib 가 1.9.25 로 끌려간다. 두 모듈 모두 여기서 맞춘다.
    extra["kotlin.version"] = kotlinVersion

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
        }
    }

    // JPA 엔티티는 Hibernate 프록시 생성을 위해 open 이어야 한다.
    configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
        // 코틀린 non-null 파라미터에 Mockito 매처(eq/any)를 쓰기 위해
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:5.4.0")
        "testRuntimeOnly"("com.h2database:h2")
        // Boot 플러그인이 없는 모듈은 JUnit Platform 런처가 자동으로 붙지 않는다.
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
