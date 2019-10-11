package io.taucoin.android.ipfs

import android.app.Service
import android.os.Build.CPU_ABI
import org.slf4j.LoggerFactory
import java.lang.Runtime.getRuntime

class IPFSManager(private var service: Service) {
    private val logger = LoggerFactory.getLogger("ipfs")

    companion object{
        var daemon: Process? = null
        var logs: MutableList<String> = mutableListOf()
    }

    fun init() {
        install()
        start()
    }

    @Suppress("DEPRECATION")
    private fun install() {

        val type = CPU_ABI.let {
            when{
                it.startsWith("arm") -> "arm"
                it.startsWith("x86") -> "386"
                else ->  {
                    logger.error("Unsupported ABI $it")
                    throw Exception("Unsupported ABI $it")
                }
            }
        }

        service.bin.apply {
            delete()
            createNewFile()
        }

        service.store.apply {
            if(!exists()){
                mkdirs()
            }
        }

        val input = service.assets.open(type)
        val output = service.bin.outputStream()

        try {
            input.copyTo(output)
        } finally {
            input.close()
            output.close()
        }

        service.bin.setExecutable(true)
        logger.info("Installed binary")
    }

    fun start() {
        logs.clear()

        service.exec("init").apply {
            read{
                logger.info("init=$it")
                logs.add(it)
            }
            waitFor()
        }
        service.config {
            obj("API").obj("HTTPHeaders").apply {
                array("Access-Control-Allow-Origin").also { origins ->
                    val webui = json("https://sweetipfswebui.netlify.com")
                    val local = json("http://127.0.0.1:5001")
                    if (webui !in origins) origins.add(webui)
                    if (local !in origins) origins.add(local)
                }
                array("Access-Control-Allow-Methods").also { methods ->
                    val put = json("PUT")
                    val get = json("GET")
                    val post = json("POST")
                    if(put !in methods) methods.add(put)
                    if(get !in methods) methods.add(get)
                    if(post !in methods) methods.add(post)
                }
            }

        }

        val closeChildThread = object : Thread() {
            override fun run() {
                logger.info("ShutdownHook ipfs daemon")
                daemon?.destroy()
                daemon = null
            }
        }
        getRuntime().addShutdownHook(closeChildThread)

        service.exec("daemon --enable-pubsub-experiment").apply {
            daemon = this
            read{
                logger.info("daemon=$it")
                logs.add(it)
            }
        }
    }

    fun stop() {
        daemon?.destroy()
        daemon = null
    }
}