import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * Created by ahmedatef on 11/7/15.
 */
public class Server {
  public enum State {
    DOWN,
    CONNECTING,
    FAILED_TO_CONNECT,
    LISTENING,
  }

  private ServerSocket tcpServer;
  private DatagramSocket udpServer;

  private Thread tcpThread;
  private Thread udpThread;

  private GuiController ui;

  private State state = State.DOWN;

  public Server(int tcpPort, int udpPort, GuiController guiController) {
    state = State.CONNECTING;

    this.ui = guiController;

    log(String.format("Attempting TCP socket on port %d", tcpPort));
    try {
      tcpServer = new ServerSocket(tcpPort);
    } catch(IOException e) {
      log(e.getMessage());
      state = State.FAILED_TO_CONNECT;
      return;
    }

    log(String.format("Attempting UDP socket on port %d", udpPort));
    try {
      udpServer = new DatagramSocket(udpPort);
    } catch(SocketException e) {
      log(e.getMessage());
      state = State.FAILED_TO_CONNECT;
      return;
    }

    boolean tcpConnected = initTcpThread();
    boolean udpConnected = initUdpThread();

    tcpThread.start();
    udpThread.start();

    state = tcpConnected && udpConnected ? State.LISTENING : State.FAILED_TO_CONNECT;
    if(tcpConnected && udpConnected) {
      state = State.LISTENING;
      log("Server is ready and listening...");
    } else {
      state = State.FAILED_TO_CONNECT;
      log("Server couldn't connect, change port numbers");
    }
  }

  private boolean initTcpThread() {
    tcpThread = new Thread() {
      public void run() {
        try {
          while(true) {
            Socket clientSocket = tcpServer.accept();

            // Resolving the client request on a detached thread.
            Thread resolve = new Thread() {
              public void run() {
                try {
                  resolveAndRespondToTcpClient(clientSocket);
                } catch(Exception e) {
                  log(e.getMessage());
                }
              }
            };
            resolve.start();
          }
        } catch(Exception e) {
          log(e.getMessage());
        }
      }
    };
    return true;
  }

  private boolean initUdpThread() {
    udpThread = new Thread() {
      public void run() {
        try {
          while(true) {
            byte[] dataAwaiting = new byte[8];
            DatagramPacket receivePacket = new DatagramPacket(dataAwaiting,
                    dataAwaiting.length);

            udpServer.receive(receivePacket);

            // Resolving the client request on a detached thread.
            Thread resolve = new Thread() {
              public void run() {
                try {
                  resolveAndRespondToUdpClient(receivePacket);
                } catch(Exception e) {
                  log(e.getMessage());
                }
              }
            };
            resolve.start();
          }
        } catch(IOException e) {
          log(e.getMessage());
        }
      }
    };
    return true;
  }


  private void resolveAndRespondToTcpClient(Socket clientSocket) throws IOException {
    if(state != State.LISTENING) return;

    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());

    // Read the first 2 int from the dataInputStream.
    int n1 = dataInputStream.readInt();
    int n2 = dataInputStream.readInt();

    log(String.format("TCP packet received from %s on port %d containing %d and %d",
            clientSocket.getRemoteSocketAddress().toString(),
            clientSocket.getPort(),
            n1,
            n2));

    int result = n1 + n2;
    DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
    dataOutputStream.writeInt(result);
    dataOutputStream.flush();

    log(String.format("TCP packet sent to %s on port %d containing %d",
            clientSocket.getRemoteSocketAddress().toString(),
            clientSocket.getPort(),
            result));
  }

  private void resolveAndRespondToUdpClient(DatagramPacket receivedPacket) throws IOException {
    if(state != State.LISTENING) return;

    ByteBuffer byteBuffer = ByteBuffer.wrap(receivedPacket.getData());
    // Read the first 2 int from the byteBuffer.
    int n1 = byteBuffer.getInt();
    int n2 = byteBuffer.getInt();
    int result = n1 + n2;

    log(String.format("UDP packet received from %s on port %d containing %d and %d",
            receivedPacket.getAddress().getHostAddress(),
            receivedPacket.getPort(),
            n1,
            n2));

    ByteBuffer requestedData = ByteBuffer.allocate(Integer.BYTES);
    requestedData.putInt(result);

    DatagramPacket sendPacket = new DatagramPacket(
            requestedData.array(),
            requestedData.array().length,
            receivedPacket.getAddress(),
            receivedPacket.getPort());

    udpServer.send(sendPacket);

    log(String.format("UDP packet sent to %s on port %d containing %d",
            receivedPacket.getAddress().getHostAddress(),
            receivedPacket.getPort(),
            result));
  }

  public State getState() {
    return state;
  }

  private void log(String log) {
    ui.serverLog("=> " + log);
  }

  public boolean shutdown() {
    log("Attempting to shutdown server");

    if(state != State.LISTENING) {
      log("Server was not up to shut it down");
      return false;
    }

    try {
      tcpServer.close();
    } catch(IOException e) {
      log("TCP Server: " + e.getMessage());
    }

    udpServer.close();

    boolean allClosed = tcpServer.isClosed() && udpServer.isClosed();
    if(allClosed) {
      state = State.DOWN;
      log("Server is shutdown");
    } else {
      log("Server failed to shutdown");
    }
    return allClosed;
  }
}