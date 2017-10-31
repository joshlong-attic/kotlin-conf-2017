package com.example.exposed

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.annotation.PostConstruct
import javax.sql.DataSource

@SpringBootApplication
//@EnableAspectJAutoProxy(proxyTargetClass = true)
class ExposedApplication {

    @Bean
    fun springTransactionManager(ds: DataSource) = SpringTransactionManager(ds)

    @Bean
    fun initializer(cr: CityRepository) = InitializingBean {
        cr.begin()
    }
}

@RestController
@Transactional
@RequestMapping("/cities")
class CityRestController(private val cr: CityRepository) {

    // curl -H"content-type: application/json" -d'{"name":"Los Angeles"}' http://localhost:8080/cities
    @PostMapping
    fun write(@RequestBody city: City): ResponseEntity<Void> {
        val insert = cr.insert(City(name = city.name))
        val uri = URI.create("/cities/${insert}")
        return ResponseEntity.created(uri).build<Void>()
    }

    @GetMapping
    fun all() = cr.selectAll()

    @GetMapping("/{name}")
    fun cityByName(@PathVariable name: String) = cr.select(Cities.select { Cities.name.eq(name) })
}

@Repository
@Transactional
class CityRepository {

    private val cityTransform: (ResultRow) -> City = { City(it[Cities.id], it[Cities.name]) }

    //  @PostConstruct
    fun begin() {
        SchemaUtils.create(Cities)
    }

    fun select(q: Query): Iterable<City> = q.map(cityTransform)

    fun selectAll(): Iterable<City> = Cities.selectAll().map(cityTransform)

    fun insert(city: City): Int = Cities.insert { it[name] = city.name }.get(Cities.id)
}

class City(var id: Int? = null, var name: String? = null)

object Cities : Table() {
    val id = integer("id").autoIncrement().primaryKey() // Column<Int>
    val name = varchar("name", 50) // Column<String>
}

fun main(args: Array<String>) {
    SpringApplication.run(ExposedApplication::class.java, *args)
}