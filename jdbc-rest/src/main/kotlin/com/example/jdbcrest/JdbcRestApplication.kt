package com.example.jdbcrest

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.ui.ModelMap
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver

@SpringBootApplication
class JdbcRestApplication {

    @Bean
    fun init(cs: CustomerService) = ApplicationRunner {
        cs.save(Customer("Sebastien"))
        cs.save(Customer("Josh"))
        cs.byName("Sebastien").forEach { println(it) }
    }
}

@Configuration
class ScriptViewConfiguration {

    @Bean
    fun kotlinScriptConfigurer() =
            ScriptTemplateConfigurer().apply {
                engineName = "kotlin"
                setScripts("scripts/render.kts")
                renderFunction = "render"
                isSharedEngine = false
            }

    @Bean
    fun kotlinScriptViewResolver() =
            ScriptTemplateViewResolver().apply {
                setPrefix("templates/")
                setSuffix(".kts")
            }
}

fun main(args: Array<String>) {
    runApplication<JdbcRestApplication>(*args)
}

@Controller
class CustomerController(private val cs: CustomerService) {

//    @GetMapping("/customers/{name}.php")
//    fun customers(@PathVariable name: String) = ModelAndView("customers", mapOf("customers" to cs.byName(name)))

    @GetMapping("/customers-{name}.php")
    fun customers(model: Model, @PathVariable name: String): String {
        model["customers"] = cs.byName(name)
        return "customers"
    }

    @GetMapping("/customers.php")
    fun all(model: Model): String {
        model["customers"] = cs.all()
        return "customers"
    }
}


@Service
class CustomerService(private val jdbcOperations: JdbcOperations) {

    fun all() = jdbcOperations.query("SELECT  * FROM CUSTOMERS") { rs, i ->
        Customer(rs.getString("NAME"), rs.getLong("ID"))
    }

    fun save(c: Customer) =
            jdbcOperations.execute("INSERT INTO CUSTOMERS(NAME) VALUES(?)") { ps ->
                ps.setString(1, c.name)
                ps.execute()
            }

    fun byName(name: String) =
            jdbcOperations.query<Customer>("SELECT * FROM CUSTOMERS WHERE NAME = ?", name) { rs, i ->
                Customer(rs.getString("NAME"), rs.getLong("ID"))
            }
}

data class Customer(val name: String, val id: Long? = null)