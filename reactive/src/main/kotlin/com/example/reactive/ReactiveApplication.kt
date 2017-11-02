package com.example.reactive

import org.reactivestreams.Publisher
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
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux

@SpringBootApplication
class ReactiveApplication

fun main(args: Array<String>) {

    SpringApplicationBuilder()
            .sources(ReactiveApplication::class.java)
            .initializers(beans {
                bean {
                    ApplicationRunner {

                        val customerService = ref<CustomerRepository>()

                        val customers: Flux<Customer> = arrayOf("A", "B", "C", "D")
                                .toFlux()
                                .flatMap { customerService.save(Customer(name = it)) }

                        customerService
                                .deleteAll()
                                .thenMany(customers)
                                .thenMany(customerService.findAll())
                                .subscribe { println(it) }
                    }
                }
                bean {
                    router {
                        val customerRepository = ref<CustomerRepository>()
                        GET("/customers/{id}") { ServerResponse.ok().body(customerRepository.findById(it.pathVariable("id"))) }
                        GET("/customers") { ServerResponse.ok().body(customerRepository.findAll()) }
                    }
                }
                bean {
                    gateway {
                        route {
                            id ("blog-atom")
                            predicate(path("/atom") or path("/blog.atom"))
                            uri("http://spring.io:80/blog.atom")
                        }
                    }
                }

            })
            .run(*args)
}

interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
data class Customer(@Id var id: String? = null, var name: String? = null)