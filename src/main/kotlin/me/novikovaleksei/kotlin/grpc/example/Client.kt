package me.novikovaleksei.kotlin.grpc.example

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HelloWorldClient(val channel: ManagedChannel) : Closeable {
    private val stub: GreeterGrpcKt.GreeterCoroutineStub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    fun greet(name: String) = runBlocking {
        val request = HelloRequest.newBuilder().setName(name).build()
        try {
            val response = stub.sayHello(request)
            println("Greeter client received: ${response.message}")
        } catch (e: StatusException) {
            println("RPC failed: ${e.status}")
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

fun main(args: Array<String>) {
    val isRemote = args.size == 1

    Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        val builder = if (isRemote)
            ManagedChannelBuilder.forTarget(args[0].removePrefix("https://") + ":443").useTransportSecurity()
        else
            ManagedChannelBuilder.forTarget("localhost:50051").usePlaintext()

        HelloWorldClient(
            builder.executor(dispatcher.asExecutor()).build()
        ).use {
            val user = args.singleOrNull() ?: "world"
            it.greet(user)
        }
    }
}
