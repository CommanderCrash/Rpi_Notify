# Rpi_Notify

**Rpi_Notify** is a Python-based notification server designed to handle notifications via both TCP and UNIX socket connections. It also provides a lightweight web interface for monitoring and managing connected devices, logs, and server statistics.

## Features
- **Multi-protocol support**: Supports both TCP and UNIX sockets for communication.
- **Web Interface**: Flask-powered UI for managing and monitoring the server.
- **Message Queue**: Uses a queue to process and broadcast messages efficiently.
- **Device Management**: Tracks connected devices with status and uptime.
- **Message Logs**: Maintains a log of all received messages with timestamps.
- **Server Statistics**: Provides information on active connections, uptime, and message count.
- **Test Messaging**: Allows sending test messages through the web interface.
