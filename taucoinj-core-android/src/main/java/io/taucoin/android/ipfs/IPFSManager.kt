package io.taucoin.android.ipfs

import android.app.Service
import android.os.Build.CPU_ABI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Runtime.getRuntime

class IPFSManager(private var service: Service) {

    companion object{
        var nodeA: String = "/ip4/52.10.204.93/tcp/6001/ipfs/QmNu9vByGwjdnvRuyqTMi35FQvznEQ6qNLVnBFNxvJA2ip"
        var logger: Logger = LoggerFactory.getLogger("ipfs")
        var daemon: Process? = null

        fun stop(){
            logger.info("stop ipfs daemon")
            daemon?.destroy()
            daemon = null
            shutdown()
        }

        private fun shutdown() {
            logger.info("shutdown ipfs process")
            getRuntime().exec("ps").apply {
                read{
                    if(it.contains("goipfs") && it.length > 15){
                        val pid = it.substring(10, 15).trim()
                        getRuntime().exec("kill -2 $pid")
                        logger.info("shutdown cmd: kill -2 $pid")
                    }
                }
                waitFor()
            }
        }
    }

    fun init() {
        install()
        stop()
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

            val swarmFile = service.store["swarm.key"]

            if(!swarmFile.exists()){

            }
        }
        val swarmFile = service.store["swarm.key"]
        swarmFile.apply {
            delete()
            createNewFile()

            val keyInput = service.assets.open("swarm.key")
            copyFileTo(keyInput, this)
        }

        val cpuInput = service.assets.open(type)
        copyFileTo(cpuInput, service.bin)

        service.bin.setExecutable(true)
        logger.info("Installed binary")
    }

    fun start() {
        service.exec("init --profile=lowpower").apply {
            read{
                logger.info("init=$it")
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
            array("Bootstrap").also { methods ->
                val iterator = methods.iterator()
                while (iterator.hasNext()){
                    iterator.next()
                    iterator.remove()
                }
                val nodeA = json(nodeA)
                if(nodeA !in methods) methods.add(nodeA)
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

        service.exec("daemon --enable-pubsub-experiment --enable-gc").apply {
            daemon = this
            read{
                logger.info("daemon=$it")
            }
        }
    }

    fun restart() {
        stop()
        start()
    }
}