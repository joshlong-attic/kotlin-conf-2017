package com.example.demo

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver
import javax.sql.DataSource

@SpringBootApplication
class DemoApplication {

    @Bean
    @Profile("exposed")
    fun platformTransactionManager(ds: DataSource) = SpringTransactionManager(ds)

    @Bean
    fun init(customerService: CustomerService) = ApplicationRunner {
        customerService.insert(Customer("A"))
        customerService.insert(Customer("B"))
        customerService.insert(Customer("C"))
        customerService.insert(Customer("D"))
        customerService.all().forEach { println(it) }
    }
}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

object Customers : Table() {
    val id = long("id").autoIncrement().primaryKey()
    val name = varchar("name", 255)
}

@Controller
class CustomerController(private val customerService: CustomerService) {

    @GetMapping("/customers.php")
    fun customers() = ModelAndView("customers", mapOf("customers" to this.customerService.all()))
}

@Configuration
class TemplateViewConfiguration {

    @Bean
    fun viewResolver() = ScriptTemplateViewResolver().apply {
        this.setSuffix(".kts")
        this.setPrefix("templates/")
    }

    @Bean
    fun viewConfigurer() = ScriptTemplateConfigurer().apply {
        this.setScripts("scripts/render.kts")
        this.engineName = "kotlin"
        this.renderFunction = "render"
        this.isSharedEngine = false
    }
}

@Service
@Transactional
@Profile("exposed")
class ExposedCustomerService(private val transactionTemplate: TransactionTemplate) : CustomerService, InitializingBean {

    override fun all(): Collection<Customer> = Customers.selectAll().map { Customer(it[Customers.name], it[Customers.id]) }

    override fun afterPropertiesSet() {
        this.transactionTemplate.execute {
            SchemaUtils.create(Customers)
        }
    }

    override fun insert(c: Customer) {
        Customers.insert { it[Customers.name] = c.name }
    }

    override fun byId(id: Long): Customer? = Customers.select { Customers.id.eq(id) }.map { Customer(it[Customers.name], it[Customers.id]) }.firstOrNull()
}

@Service
@Transactional
@Profile("jdbc")
class JdbcCustomerService(private val jdbcOperations: JdbcOperations) : CustomerService {

    override fun all(): Collection<Customer> =
            this.jdbcOperations.query("SELECT * FROM CUSTOMERS") { rs, i ->
                Customer(rs.getString("NAME"), rs.getLong("ID"))
            }

    override fun insert(c: Customer) {
        this.jdbcOperations.execute("INSERT INTO CUSTOMERS(NAME) VALUES(?)") {
            it.setString(1, c.name)
            it.execute()
        }
    }

    override fun byId(id: Long): Customer? =
            this.jdbcOperations.queryForObject("select * from customers where id=?", id) { rs, i ->
                Customer(rs.getString("NAME"), rs.getLong("ID"))
            }
}

interface CustomerService {

    fun all(): Collection<Customer>

    fun insert(c: Customer)

    fun byId(id: Long): Customer?
}

data class Customer(val name: String, var id: Long? = null)