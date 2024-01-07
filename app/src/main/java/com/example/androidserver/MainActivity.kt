package com.example.androidserver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.androidserver.databinding.ActivityMainBinding
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.Scanner
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var serverUp = false
    private var mHttpServer: HttpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val port = 5000

        binding.serverButton.setOnClickListener {
            serverUp = if (!serverUp) {
                startServer(port)
                true
            } else {
                stopServer()
                false
            }
        }
    }

    private fun streamToString(inputStream: InputStream): String {
        val s = Scanner(inputStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

    private fun sendResponse(httpExchange: HttpExchange, responseText: String, contentType: String) {
        httpExchange.responseHeaders.set("Content-Type", contentType)
        httpExchange.sendResponseHeaders(200, responseText.length.toLong())
        val os = httpExchange.responseBody
        os.write(responseText.toByteArray())
        os.close()
    }

    private fun readAssetFile(filePath: String): String {
        return try {
            val inputStream: InputStream = application.assets.open(filePath)
            val content = streamToString(inputStream)
            Log.d("readFromAsset", "Read content from $filePath: $content")
            content
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("readFromAsset", "Error reading content from $filePath: ${e.message}")
            ""
        }
    }

    private fun sendFileResponse(httpExchange: HttpExchange, filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val contentType = determineContentType(filePath)
                val content = file.readText()
                sendResponse(httpExchange, content, contentType)
            } else {
                sendErrorResponse(httpExchange, 404, "File not found: $filePath")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            sendErrorResponse(httpExchange, 500, "Internal Server Error")
        }
    }

    private fun determineContentType(filePath: String): String {
        return when {
            filePath.endsWith(".html") -> "text/html"
            filePath.endsWith(".css") -> "text/css"
            filePath.endsWith(".js") -> "application/javascript"
            filePath.endsWith(".svg") -> "image/svg+xml"
            else -> "text/plain"
        }
    }

    private fun sendErrorResponse(httpExchange: HttpExchange, code: Int, message: String) {
        try {
            httpExchange.sendResponseHeaders(code, message.length.toLong())
            val os: OutputStream = httpExchange.responseBody
            os.write(message.toByteArray())
            os.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startServer(port: Int) {
        try {
            mHttpServer = HttpServer.create(InetSocketAddress(port), 0)
            mHttpServer!!.executor = Executors.newCachedThreadPool()

            mHttpServer!!.createContext("/", rootHandler)
            mHttpServer!!.createContext("/index", rootHandler)
            // Add more contexts if needed

            mHttpServer!!.start()

            GlobalScope.launch(Dispatchers.Main) {
                binding.serverTextView.text = getString(R.string.server_running)
                binding.serverButton.text = getString(R.string.stop_server)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopServer() {
        if (mHttpServer != null) {
            mHttpServer!!.stop(0)
            binding.serverTextView.text = getString(R.string.server_down)
            binding.serverButton.text = getString(R.string.start_server)
        }
    }

    private val rootHandler = HttpHandler { exchange ->
        run {
            when (exchange!!.requestMethod) {
                "GET" -> {
                    val uri = exchange.requestURI.toString()
                    if (uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".svg")) {
                        val content = readAssetFile("build$uri")
                        val contentType = determineContentType("build$uri");
                        sendResponse(exchange, content, contentType);
                    } else {
                        val content = readAssetFile("build/index.html")
                        sendResponse(exchange, content, "text/html");
                    }
                }
            }
        }
    }
}