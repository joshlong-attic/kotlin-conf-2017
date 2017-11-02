package com.example.reactivebasics

import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.gateway.handler.predicate.RoutePredicates.path
import org.springframework.cloud.gateway.route.gateway
import org.springframework.context.support.beans
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toFlux

@SpringBootApplication
class ReactiveBasicsApplication


fun main(args: Array<String>) {

    SpringApplicationBuilder()
            .sources(ReactiveBasicsApplication::class.java)
            .initializers(beans {
                bean {
                    InitializingBean {
                        val customerRepository = ref<CustomerRepository>()
                        customerRepository
                                .deleteAll()
                                .thenMany(arrayOf("Tammie", "Hadi", "Madhura", "Cornelia", "Andrey").toFlux().flatMap { customerRepository.save(Customer(name = it)) })
                                .thenMany(customerRepository.findAll())
                                .subscribe { println(it) }
                    }
                }
                bean {
                    val customerRepository = ref<CustomerRepository>()
                    router {
                        GET("/customers") { ServerResponse.ok().body<Customer>(customerRepository.findAll()) }
                        GET("/customers/{id}") { ServerResponse.ok().body<Customer>(customerRepository.findById(it.pathVariable("id"))) }
                    }
                }
                bean {
                    gateway {
                        route {
                            id("blog")
                            predicate(path("/blog"))
                            uri("http://spring.io:80/blog")
                        }
                    }
                }
            })
            .run(*args)
}


interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
data class Customer(@Id var id: String? = null, var name: String? = null)