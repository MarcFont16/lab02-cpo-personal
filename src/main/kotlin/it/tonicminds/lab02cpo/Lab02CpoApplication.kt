package it.tonicminds.lab02cpo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class Lab02CpoApplication

fun main(args: Array<String>) {
    runApplication<Lab02CpoApplication>(*args)
}
