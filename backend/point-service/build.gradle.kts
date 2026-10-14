import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 루트는 공통 설정만 가진다. 코드는 두 모듈에 있다.
// - point-api: 실행 모듈(컨트롤러·필터·바깥 호출 어댑터). bootJar 는 여기서만 나온다.
// - point-application: 라이브러리 모듈(유스케이스·서비스·엔티티·ro/rw 저장소·JPA 설정).
plugins {
    id("org.springframework.boot") version "4.0.8" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.spring") version "2.4.21" apply false
    kotlin("plugin.jpa") version "2.4.21" apply false
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // 실행 모듈(point-api)만 자기 build.gradle.kts 에서 org.springframework.boot 플러그인을 적용한다.
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

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            // Boot BOM 은 kotlin.version 을 자신이 빌드된 버전으로 고정한다. Boot 플러그인이 없는 라이브러리 모듈에서
            // 컴파일러와 stdlib 이 어긋나지 않도록 실제 적용된 코틀린 플러그인 버전으로 덮어쓴다.
            mavenBom(SpringBootPlugin.BOM_COORDINATES) {
                bomProperty("kotlin.version", getKotlinPluginVersion())
            }
            // Boot 4.0.x 와 짝인 릴리스 트레인(2025.1.3 의 spring-boot.version = 4.0.8).
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.3")
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
        // Boot 4 는 Jackson 3 을 쓴다(좌표가 tools.jackson 으로 바뀌었다).
        "implementation"("tools.jackson.module:jackson-module-kotlin")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
        // 코틀린 non-null 파라미터에 Mockito 매처(eq/any)를 쓰기 위해
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:6.4.0")
        "testRuntimeOnly"("com.h2database:h2")
        // Boot 플러그인이 없는 모듈은 JUnit Platform 런처가 자동으로 붙지 않는다.
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
