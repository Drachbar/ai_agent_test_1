plugins {
    id("java")
}

group = "se.drachbar"
version = "1.0-SNAPSHOT"

val openAiKey: String by project

tasks.withType<JavaExec> {
    environment("OPENAI_API_KEY", openAiKey)
}


repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0-alpha1")
    implementation("dev.langchain4j:langchain4j:1.0.0-alpha1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}