package com.example.jdbcrest

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Service

@SpringBootApplication
class JdbcRestApplication {

    @Bean
    fun init(cs: CustomerService) = ApplicationRunner {
        cs.save(Customer("Sebastien"))
        cs.save(Customer("Josh"))
        cs.byName("Sebastien").forEach { println(it) }
    }
}

fun main(args: Array<String>) {
    runApplication<JdbcRestApplication>(*args)
}


@Service
class CustomerService(private val jdbcOperations: JdbcOperations) {

    fun save(c: Customer) = jdbcOperations
            .execute("INSERT INTO CUSTOMERS(NAME) VALUES(?)") { ps ->
                ps.setString(1, c.name)
                ps.execute()
            }

    fun byName(name: String) =
            jdbcOperations.query<Customer>("SELECT * FROM CUSTOMERS WHERE NAME = ?", name) { rs, i ->
                Customer(rs.getString("NAME"), rs.getLong("ID"))
            }
}

data class Customer(val name: String, val id: Long? = null)