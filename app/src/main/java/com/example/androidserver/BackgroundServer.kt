package com.example.androidserver

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Scanner
import java.util.concurrent.Executors

class BackgroundServer : Service() {

    private var mHttpServer: HttpServer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        requestForegroundServicePermission()
        startServer(5000)
    }

    override fun onDestroy() {
        Log.d("server", "Server Stopper")
        // Stop your server when the service is destroyed
        stopServer()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification: Notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("Foreground Service")
            .setContentText("Server is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()

        startForeground(1, notification)

        return Service.START_NOT_STICKY
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "CHANNEL_ID",
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Use the Settings.ACTION_MANAGE_OVERLAY_PERMISSION action for Android 6.0+
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun startServer(port: Int): String {
        try {
            mHttpServer = HttpServer.create(InetSocketAddress(port), 0)
            mHttpServer!!.executor = Executors.newCachedThreadPool()

            mHttpServer!!.createContext("/", rootHandler)
            Log.d("server", "Server started")

            mHttpServer!!.start()

            return "${getLocalIpAddress()}:${port}"

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "N/A"
    }

    private fun getLocalIpAddress(): String {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            Log.d("NetworkInterfaces", networkInterfaces.toString());
            for (networkInterface in networkInterfaces) {
                val inetAddresses = Collections.list(networkInterface.inetAddresses)

                for (inetAddress in inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress as String
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "N/A"
    }

    private fun stopServer() {
        if (mHttpServer != null) {
            mHttpServer!!.stop(0)
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

    private fun streamToString(inputStream: InputStream): String {
        val s = Scanner(inputStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
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

    private fun sendResponse(httpExchange: HttpExchange, responseText: String, contentType: String) {
        httpExchange.responseHeaders.set("Content-Type", contentType)
        httpExchange.sendResponseHeaders(200, responseText.length.toLong())
        val os = httpExchange.responseBody
        os.write(responseText.toByteArray())
        os.close()
    }

}
