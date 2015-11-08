import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * Created by ahmedatef on 11/7/15.
 */
public class Client {
  private static int count = 0;

  public enum Protocol {
    TCP,
    UDP,
  }

  private GuiController ui;
  private int port;
  private int id;
  public Client(Protocol protocol, int port, GuiController guiController, int n1, int n2) {
    this.ui = guiController;
    this.port = port;
    this.id = count++;

    log("Client created");

    switch(protocol) {
      case TCP:
        tcpRequestAddition(n1, n2);
        break;
      case UDP:
        udpRequestAddition(n1, n2);
    }

    log("Client terminated");
  }

  private void log(String log) {
    ui.clientLog(String.format("%d => %s", id, log));;
  }
  private byte[] makeData(int n1, int n2) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 2);
    buffer.putInt(n1);
    buffer.putInt(n2);
    return buffer.array();
  }

  /**
   * Takes 2 int as parameters, send them to localhost on passed port, using TCP.
   * This method blocks while retrieving the response from the server.
   * @param n1 and n2 are numbers to be sent to the server.
   * @return the response of the server of the summation of the sent integers.
   */
  private int tcpRequestAddition(int n1, int n2) {
    byte[] data = makeData(n1, n2);

    Socket clientSocket = null;
    try {
      clientSocket = new Socket("localhost", port);
    } catch(IOException e) {
      log(e.getMessage());
    }

    // Sending data to the server.
    try {
      OutputStream outputStream = clientSocket.getOutputStream();
      outputStream.write(data);
      outputStream.flush();
    } catch(IOException e) {
      log(e.getMessage());
    }

    log(String.format("TCP request was sent to server to add %d and %d and waiting for " +
            "server response...", n1, n2));

    int result = -1;
    try {
      DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
      result = dataInputStream.readInt();

      log(String.format("TCP response received with value %d", result));

      clientSocket.close();
    } catch(IOException e) {
      log(e.getMessage());
    }
    return result;
  }

  /**
   * Takes 2 int as parameters, send them to localhost on passed port, using UDP.
   * This method blocks while retrieving the response from the server.
   * @param n1 and n2 are numbers to be sent to the server.
   * @return the response of the server of the summation of the sent integers.
   */
  private int udpRequestAddition(int n1, int n2) {
    byte[] data = makeData(n1, n2);

    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
    } catch(SocketException e) {
      log(e.getMessage());
    }

    InetAddress ipAddress = null;
    try {
      ipAddress = InetAddress.getByName("localhost");
    } catch(UnknownHostException e) {
      log(e.getMessage());
    }

    DatagramPacket sendPacket = new DatagramPacket(
            data,
            data.length,
            ipAddress,
            port);
    try {
      clientSocket.send(sendPacket);
    } catch(IOException e) {
      log(e.getMessage());
    }

    log(String.format("UDP request was sent to server to add %d and %d and waiting for " +
            "server response...", n1, n2));

    byte[] receiveData = new byte[Integer.BYTES];
    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

    // Waiting for response
    try {
      clientSocket.receive(receivePacket);
    } catch(IOException e) {
      log(e.getMessage());
    }

    ByteBuffer receivedData = ByteBuffer.wrap(receivePacket.getData());
    int result = receivedData.getInt();
    log(String.format("UDP response received with value %d", result));

    clientSocket.close();

    return result;
  }
}
