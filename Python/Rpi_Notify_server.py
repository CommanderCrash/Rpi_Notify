import os
import socket
import select
import threading
import queue
import time
import argparse
import stat
import sys
import signal
import psutil
from flask import Flask, render_template, jsonify, request 
from datetime import datetime

class NotificationServer:
    def __init__(self, tcp_port=5556, use_tcp=False, use_socket=True, socket_path="/mnt/ram/Rpi_notify"):
        self.tcp_port = tcp_port
        self.use_tcp = use_tcp
        self.use_socket = use_socket
        self.socket_path = socket_path
        self.message_queue = queue.Queue()
        self.clients = []
        self.running = True
        self.server_name = socket.gethostname()
        self.connected_devices = {}
        self.message_log = []
        self.start_time = datetime.now()
        
        # Initialize Flask app
        self.app = Flask(__name__)
        self.setup_routes()
        
    def setup_routes(self):
        self.app.add_url_rule('/', 'index', self.index)
        self.app.add_url_rule('/api/devices', 'get_devices', self.get_devices)
        self.app.add_url_rule('/api/logs', 'get_logs', self.get_logs)
        self.app.add_url_rule('/api/logs/clear', 'clear_logs', self.clear_logs, methods=['POST'])
        self.app.add_url_rule('/api/stats', 'get_stats', self.get_stats)
        self.app.add_url_rule('/api/test-message', 'test_message', self.test_message, methods=['POST'])

    def index(self):
        return render_template('index.html')
            
    def get_devices(self):
        return jsonify(self.connected_devices)
            
    def get_logs(self):
        return jsonify(self.message_log)

    def clear_logs(self):
        self.message_log.clear()
        return jsonify({'status': 'success'})

    def get_stats(self):
        uptime = datetime.now() - self.start_time
        return jsonify({
            'active_connections': len(self.clients),
            'messages_sent': len(self.message_log),
            'status': 'Online',
            'uptime': str(uptime).split('.')[0],  # Format as HH:MM:SS
            'server_name': self.server_name
        })

    def test_message(self):
        try:
            data = request.get_json()
            if data and 'message' in data:
                print(f"Received test message: {data['message']}")
                # Clear queue before adding new message
                while not self.message_queue.empty():
                    try:
                        self.message_queue.get_nowait()
                        self.message_queue.task_done()
                    except queue.Empty:
                        break
                # Add new message
                self.message_queue.put(data['message'])
                self.message_log.append({
                    'timestamp': datetime.now().isoformat(),
                    'message': data['message'],
                    'source': 'Web UI'
                })
                return jsonify({'status': 'success'})
        except Exception as e:
            print(f"Test message error: {e}")
            return jsonify({'status': 'error', 'message': str(e)}), 500
        return jsonify({'status': 'error', 'message': 'No message provided'}), 400
            
    def start(self):
        print(f"Starting server with name: {self.server_name}")
        print(f"TCP mode: {self.use_tcp}")
        print(f"Unix socket: {self.use_socket}")
        
        if self.use_socket:
            threading.Thread(target=self.run_unix_socket, daemon=True).start()
            
        if self.use_tcp:
            threading.Thread(target=self.run_tcp_socket, daemon=True).start()
            
        threading.Thread(target=self.process_message_queue, daemon=True).start()
        
        print(f"Starting web interface on port 5557")
        self.app.run(host='0.0.0.0', port=5557)
        
    def run_unix_socket(self):
        if os.path.exists(self.socket_path):
            os.remove(self.socket_path)
            
        unix_socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        unix_socket.bind(self.socket_path)
        os.chmod(self.socket_path, stat.S_IRWXU | stat.S_IRWXG | stat.S_IRWXO)
        unix_socket.listen(5)
        
        print(f"Unix socket listening at {self.socket_path}")
        
        while self.running:
            try:
                conn, _ = unix_socket.accept()
                data = conn.recv(1024).decode('utf-8')
                if data:
                    message = data.strip('|')
                    print(f"Received unix socket message: {message}")
                    self.message_queue.put(message)
                    self.message_log.append({
                        'timestamp': datetime.now().isoformat(),
                        'message': message
                    })
                conn.close()
            except Exception as e:
                print(f"Unix socket error: {e}")

    def run_tcp_socket(self):
        tcp_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tcp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        tcp_socket.bind(('0.0.0.0', self.tcp_port))
        tcp_socket.listen(5)
        
        print(f"TCP socket listening on port {self.tcp_port}")
        
        while self.running:
            try:
                conn, addr = tcp_socket.accept()
                print(f"New TCP connection from {addr}")
                self.clients.append(conn)
                self.connected_devices[addr[0]] = {
                    'name': f"Client-{len(self.clients)}",
                    'connected_since': datetime.now().isoformat(),
                    'last_ping': 0,
                    'status': 'Connected'
                }
                threading.Thread(target=self.handle_tcp_client, 
                               args=(conn, addr), 
                               daemon=True).start()
            except Exception as e:
                print(f"TCP socket error: {e}")

    def handle_tcp_client(self, conn, addr):
        print(f"Starting to handle client {addr}")
        while self.running:
            try:
                data = conn.recv(1024).decode('utf-8')
                if not data:
                    print(f"No data from {addr}, breaking connection")
                    break
                if data.strip() == "PING":
                    print(f"Received ping from {addr}")
                    conn.send("PONG\n".encode('utf-8'))
                    continue
                
                message = data.strip('|')
                print(f"Processing message from {addr}: {message}")
                self.message_queue.put(message)
                self.message_log.append({
                    'timestamp': datetime.now().isoformat(),
                    'message': message,
                    'source': addr[0]
                })
            except Exception as e:
                print(f"Client handler error for {addr}: {e}")
                break
        
        print(f"Client disconnected: {addr}")
        if conn in self.clients:
            self.clients.remove(conn)
        if addr[0] in self.connected_devices:
            self.connected_devices[addr[0]]['status'] = 'Disconnected'
        conn.close()

    def process_message_queue(self):
        last_message = None
        while self.running:
            try:
                if not self.message_queue.empty():
                    message = self.message_queue.get()
                    # Only process if it's a new message
                    if message != last_message:
                        print(f"Processing message from queue: {message}")
                        self.broadcast_message(message)
                        last_message = message
                        # Empty the queue of any duplicates
                        while not self.message_queue.empty():
                            self.message_queue.get()
                    self.message_queue.task_done()
                time.sleep(3)  # 3 second delay between messages
            except Exception as e:
                print(f"Message processing error: {e}")
                
    def broadcast_message(self, message):
        formatted_message = f"{self.server_name}|{message}\n"
        print(f"Broadcasting message: {formatted_message.strip()}")
        disconnected_clients = []
        for client in self.clients:
            try:
                client.send(formatted_message.encode('utf-8'))
                client.getpeername()
            except Exception as e:
                print(f"Failed to send to client: {e}")
                disconnected_clients.append(client)
        
        for client in disconnected_clients:
            if client in self.clients:
                self.clients.remove(client)

    def cleanup(self):
        print("\nCleaning up...")
        self.running = False
        if self.use_socket and os.path.exists(self.socket_path):
            os.remove(self.socket_path)
        for client in self.clients:
            try:
                client.close()
            except:
                pass
        self.clients.clear()
        print("Cleanup complete")

def signal_handler(signum, frame):
    print("\nReceived signal to shutdown...")
    if 'server' in globals():
        server.cleanup()
    sys.exit(0)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-n", "--name", help="Override server name")
    parser.add_argument("-t", "--tcp", action="store_true", help="Use TCP mode")
    args = parser.parse_args()
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    server = NotificationServer(use_tcp=args.tcp)
    if args.name:
        server.server_name = args.name
        
    try:
        server.start()
    except KeyboardInterrupt:
        server.cleanup()