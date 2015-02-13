package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.HttpRequestUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			/*
			 * INPUT 처리
			 */
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line = "", uri = "", httpVersion = "", acceptType= "", host = "";
			String[] tokens;
			String requestPath = "", params = "";
			
			// Get First Line Information
			if(!"".equals(line = br.readLine())) {
				tokens = line.split(" ");
				if(tokens[0].equals("GET")) {
					httpVersion = tokens[2];
					
					uri = tokens[1];
					int index = uri.indexOf("?");
					requestPath = uri.substring(0, index);
					params = uri.substring(index+1);
				}
			}
			
			// TODO 나중에 POST에 넣을 것
			if(requestPath.equals("/create")) {
				Map<String, String> paramsMap = HttpRequestUtils.parseQueryString(params);
				User userInfo = new User(paramsMap.get("userId"), paramsMap.get("password"), paramsMap.get("name"), paramsMap.get("email"));
			}
			
			while (!"".equals(line = br.readLine())) {
				tokens = line.split(" ");
				if(tokens.length > 0) {
					if(tokens[0].equals("Host:")) {
						host = tokens[1];
					} else if(tokens[0].equals("Accept:")) {
						acceptType = tokens[1];
					}
				}
			}
			
			byte[] body = Files.readAllBytes(new File("./webapp" + requestPath).toPath());

			DataOutputStream dos = new DataOutputStream(out);
			response200Header(dos, httpVersion, body.length);
			responseBody(dos, body);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void response200Header(DataOutputStream dos, String httpVersion, int lengthOfBodyContent) {
		try {
			dos.writeBytes(httpVersion + " 200 Document Follows \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
