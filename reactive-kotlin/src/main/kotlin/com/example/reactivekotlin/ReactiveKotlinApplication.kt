package com.example.reactivekotlin

import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.factory.GatewayFilters.addResponseHeader
import org.springframework.cloud.gateway.handler.predicate.RoutePredicates.host
import org.springframework.cloud.gateway.handler.predicate.RoutePredicates.path
import org.springframework.cloud.gateway.route.Routes
import org.springframework.cloud.gateway.route.gateway
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toFlux

@SpringBootApplication
class ReactiveKotlinApplication {

    @Bean
    fun init(customerRepository: CustomerRepository,
             fmo: ReactiveFluentMongoOperations) = ApplicationRunner {

        customerRepository
                .deleteAll()
                .thenMany(
                        listOf("Sebastien", "Stephane", "Spencer", "Josh", "Madhura", "Cornelia")
                                .toFlux()
                                .flatMap { customerRepository.save(Customer(name = it)) })
                .thenMany(fmo.query<Customer>().all())
                .subscribe { println(it) }
    }

    @Bean
    fun routes(customerRepository: CustomerRepository) = router {
        GET("/customers") { ServerResponse.ok().body(customerRepository.findAll()) }
        GET("/customers/{id}") { ServerResponse.ok().body(customerRepository.findById(it.pathVariable("id"))) }
    }
}


@Configuration
class GatewayConfiguration {

    @Bean
    fun routeLocator() = gateway {

        route {
            id("test")
            uri("http://httpbin.org:80")
            predicate(host("**.abc.org") and path("/image/png"))
            add(addResponseHeader("X-TestHeader", "foobar"))
        }
        route {
            id("test2")
            uri("http://httpbin.org:80")
            predicate(path("/image/webp") or path("/image/anotherone"))
            add(addResponseHeader("X-AnotherHeader", "baz"))
            add(addResponseHeader("X-AnotherHeader-2", "baz-2"))
        }
    }
}

/**/

fun beans() = beans {
    /*   bean<UserHandler>()
       bean {
           Routes(ref(), ref()).router()
       }*/
//    profile("foo") {
    bean<Foo>()
//    }
}

class Foo : InitializingBean {
    override fun afterPropertiesSet() {
        println("hello world!")
    }
}

// See application.properties context.initializer.classes entry
//class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
//    override fun initialize(context: GenericApplicationContext) =
//            beans().initialize(context)
//
//}
/**/

fun main(args: Array<String>) {

   /* runApplication<ReactiveKotlinApplication>(*args) {
        initializers.add(beans())
    }*/

    val sab = SpringApplicationBuilder()
            .sources(ReactiveKotlinApplication::class.java)
            .initializers(beans())
            .run(*args)
    //SpringApplication.run(ReactiveKotlinApplication::class.java, *args)

}


interface CustomerRepository : ReactiveMongoRepository<Customer, String>

data class Customer(val name: String, var id: String? = null)