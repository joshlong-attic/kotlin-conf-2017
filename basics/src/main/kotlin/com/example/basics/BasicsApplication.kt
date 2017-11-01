package com.example.basics

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver
import javax.sql.DataSource

@SpringBootApplication
class BasicsApplication {

    @Profile("exposed")
    @Bean
    fun springTransactionManager(ds: DataSource) = SpringTransactionManager(ds)

    @Bean
    fun initializer(cs: CustomerService) = ApplicationRunner {

        cs.init()

        cs.insert(Customer(name = "En Lai"))
        cs.insert(Customer(name = "Tammie"))
        cs.insert(Customer(name = "Josh"))
        cs.insert(Customer(name = "Sebastien"))
        cs.all().forEach { println(it) }
    }
}

fun main(args: Array<String>) {
    runApplication<BasicsApplication>(*args)
}


@Configuration
class ScriptViewConfiguration {

    @Bean
    fun viewResolver() = ScriptTemplateViewResolver().apply {
        setSuffix(".kts")
        setPrefix("templates/")
    }

    @Bean
    fun viewTemplateConfigurer() = ScriptTemplateConfigurer().apply {
        this.setScripts("scripts/render.kts")
        this.isSharedEngine = false
        this.engineName = "kotlin"
        this.renderFunction = "render"
    }
}

data class Customer(val name: String, var id: Long? = null)

@Controller
class CustomerController(private val customerService: CustomerService) {

    @GetMapping("/customers.html")
    fun customers() = ModelAndView("customers", mapOf("customers" to this.customerService.all()))
}

interface CustomerService {

    fun init()

    fun all(): List<Customer>

    fun insert(c: Customer): Boolean

    fun byId(id: Long): Customer
}

object Customers : Table() {
    val id = long("id").autoIncrement().primaryKey()
    val name = varchar("name", 50)
}

@Profile("exposed", "default")
@Service
@Transactional
class ExposedCustomerService : CustomerService {

    override fun init() {
        SchemaUtils.create(Customers)
    }

    override fun all(): List<Customer> = Customers.selectAll().map { Customer(it[Customers.name], it[Customers.id]) }

    override fun insert(c: Customer): Boolean = Customers.insert { it[Customers.name] = c.name }.get(Customers.id) > 0

    override fun byId(id: Long): Customer = Customers.select { Customers.id.eq(id) }.map { Customer(it[Customers.name], it[Customers.id]) }.first()

}

@Profile("jdbc")
@Service
@Transactional
class JdbcCustomerService(private val jdbcOperations: JdbcOperations) : CustomerService {

    override fun init() {
    }

    override fun byId(id: Long) = this.jdbcOperations
            .queryForObject<Customer>("SELECT * FROM CUSTOMERS WHERE ID = ?") { rs, i ->
                Customer(rs.getString("NAME"), rs.getLong("ID"))
            }

    override fun all() = this.jdbcOperations
            .query("SELECT * FROM CUSTOMERS ") { rs, i ->
                Customer(rs.getString("NAME"), rs.getLong("ID"))
            }

    override fun insert(c: Customer) = this.jdbcOperations
            .execute("INSERT INTO CUSTOMERS(NAME) VALUES (?)") {
                it.setString(1, c.name)
                it.execute()
            }
}