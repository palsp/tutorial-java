/***********************************************************************

   SimpleWebServer.java


   This toy web server is used to illustrate security vulnerabilities.
   This web server only supports extremely simple HTTP GET and HTTP PUT
   requests.

 ***********************************************************************/

package com.learnsecurity;

import java.io.*;
import java.net.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;


public class SimpleWebServer {

  /* Run the HTTP server on this TCP port. */
  private static final int PORT = 8080;

  private static final String[] ALLOW_FILES = {"index.html"};

  /*
   * The socket used to process incoming connections from web clients
   */
  private static ServerSocket dServerSocket;

  public SimpleWebServer() throws Exception {
    dServerSocket = new ServerSocket(PORT);
  }

  public void run() throws Exception {
    while (true) {
      /* wait for a connection from a client */
      Socket s = dServerSocket.accept();

      /* then process the client's request */
      processRequest(s);
    }
  }

  /*
   * Reads the HTTP request from the client, and responds with the file the
   * user requested or a HTTP error code.
   */
  public void processRequest(Socket s) throws Exception {
    /* used to read data from the client */
    BufferedReader br = new BufferedReader(new InputStreamReader(
        s.getInputStream()));

    /* used to write data to the client */
    OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());

    /* read the HTTP request from the client */
    String request = br.readLine();

    String command = null;
    String pathname = null;

    /* parse the HTTP request */
    StringTokenizer st = new StringTokenizer(request, " ");

    command = st.nextToken();
    pathname = st.nextToken();



    if (command.equals("GET")) {
      /*
       * if the request is a GET try to respond with the file the user is
       * requesting
       */
      String userAgent = this.getUserAgentLine(br);
      serveFile(s, osw, pathname, userAgent);
    } else if (command.equals("PUT")) {
      /*
       * if the request is a PUT try to store the file where the user is
       * requesting
       */
      storeFile(br, osw, pathname);
    } else {
      /*
       * if the request is a NOT a GET, return an error saying this server
       * does not implement the requested command
       */
      osw.write("HTTP/1.0 501 Not Implemented\n\n");
    }

    /* close the connection to the client */
    osw.close();
  }

  public void serveFile(Socket s,OutputStreamWriter osw, String pathname, String userAgent)
      throws Exception {
    FileReader fr = null;
    int c = -1;
    StringBuffer sb = new StringBuffer();
    String command = "GET";

    /*
     * remove the initial slash at the beginning of the pathname in the
     * request
     */
    if (pathname.charAt(0) == '/')
      pathname = pathname.substring(1);

    /*
     * if there was no filename specified by the client, serve the
     * "index.html" file
     */
    if (pathname.equals(""))
    pathname = "index.html";

    if(!this.isAllowFile(pathname)){
      osw.write("HTTP/1.0 404 Not Found");
      this.logging(s, command, pathname, 404, userAgent);
      return; 
    }

    /* try to open file specified by pathnamename */
    try {
      fr = new FileReader(pathname);
      c = fr.read();
    } catch (Exception e) {
      /*
       * if the file is not found,return the appropriate HTTP response
       * code
       */
      osw.write("HTTP/1.0 404 Not Found\n\n");
      this.logging(s, command, pathname, 404, userAgent);
      return;
    }

    /*
     * if the requested file can be successfully opened and read, then
     * return an OK response code and send the contents of the file
     */
    osw.write("HTTP/1.0 200 OK\n\n");
    this.logging(s, command, pathname, 200, userAgent);
    while (c != -1) {
      sb.append((char) c);
      c = fr.read();
    }
    osw.write(sb.toString());
  }

  public void storeFile(BufferedReader br, OutputStreamWriter osw,
      String pathname) throws Exception {
    FileWriter fw = null;
    try {
      fw = new FileWriter(pathname);
      String s = br.readLine();
      while (s != null && s.length() > 0) {
        fw.write(s);
        s = br.readLine();
      }
      fw.close();
      osw.write("HTTP/1.0 201 Created");
    } catch (Exception e) {
      osw.write("HTTP/1.0 500 Internal Server Error");
    }
  }


  private String getUserAgentLine(BufferedReader br) throws Exception {
    String headerLine = null;
    String targetLine = ":";
    while ((headerLine = br.readLine()).length() != 0) {
      if (headerLine.toLowerCase().startsWith("user-agent")) {
        targetLine = headerLine;
      }
    }

    return targetLine;
  }

  private void logging(Socket s, String command, String path, Number statusCode, String userAgentLine) {
    try {
      // get time
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
      String t = dtf.format(LocalDateTime.now());

      // get user address
      String addr = s.getRemoteSocketAddress().toString().replace("/", "");

      // get user agent
      String userAgent = String.join("", new String[] { "\"", userAgentLine.split(":")[1].trim(), "\"" });

      // get endpoint
      String endpoint = String.join("", new String[] { "\"", command, " ", path, "\"" });

      // construct log line
      String log = String.join(" ",
          new String[] { addr, "- -", "[", t, "]", endpoint, statusCode.toString(), userAgent });

      BufferedWriter out = new BufferedWriter(
          new FileWriter("access.log", true));

      // Writing on output stream
      out.write(log + "\n");
      // Closing the connection
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean isAllowFile(String pathname){
    boolean isValid = false;

    for(int i = 0; i < ALLOW_FILES.length; i++){
      if(pathname.trim().equals(ALLOW_FILES[i])){
        isValid = true;
      }
    }

    return isValid;
  }
  /*
   * This method is called when the program is run from the command line.
   */
  public static void main(String argv[]) throws Exception {

    /* Create a SimpleWebServer object, and run it */
    SimpleWebServer sws = new SimpleWebServer();
    System.out.println("Listening on port: " + PORT);
    sws.run();
  }
}