package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class Server {
	
	public static final List<String> validPaths = List.of("/index.html",
		"/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js",
		"/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
	
	public static void start(ExecutorService threadPool, int port) {
		try (final var serverSocket = new ServerSocket(port)) {
			while (true) {
				final var socket = serverSocket.accept();
				threadPool.submit(() -> handleConnection(socket));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void handleConnection(Socket socket) {
		try (
			final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			final var out = new BufferedOutputStream(socket.getOutputStream());
		)
		{
			
			final var requestLine = in.readLine();
			final var parts = requestLine.split(" ");
			
			if (parts.length != 3) {
				return;
			}
			
			final var path = parts[1];
			if (!validPaths.contains(path)) {
				out.write((
					"""
						HTTP/1.1 404 Not Found\r
						Content-Length: 0\r
						Connection: close\r
						\r
						"""
				).getBytes());
				out.flush();
				return;
			}
			
			final var filePath = Path.of(".", "public", path);
			final var mimeType = Files.probeContentType(filePath);
			
			// special case for classic
			if (path.equals("/classic.html")) {
				final var template = Files.readString(filePath);
				final var content = template.replace(
					"{time}",
					LocalDateTime.now().toString()
				).getBytes();
				out.write((
					"HTTP/1.1 200 OK\r\n" +
						"Content-Type: " + mimeType + "\r\n" +
						"Content-Length: " + content.length + "\r\n" +
						"Connection: close\r\n" +
						"\r\n"
				).getBytes());
				out.write(content);
				out.flush();
				return;
			}
			
			final var length = Files.size(filePath);
			out.write((
				"HTTP/1.1 200 OK\r\n" +
					"Content-Type: " + mimeType + "\r\n" +
					"Content-Length: " + length + "\r\n" +
					"Connection: close\r\n" +
					"\r\n"
			).getBytes());
			Files.copy(filePath, out);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
	
