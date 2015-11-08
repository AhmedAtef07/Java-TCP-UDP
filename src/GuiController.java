import com.guigarage.flatterfx.FlatterFX;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Random;
import java.util.Vector;

/**
 * Created by ahmedatef on 11/7/15.
 */
public class GuiController {

  private enum SelectedProtocol {
    TCP,
    UDP,
    RANDOM,
  }

  private Stage stage;
  private Server server;

  private int tcpPort;
  private int udpPort;

  private int width = 900;
  private int height = 600;

  private TextArea serverLogTA;
  private TextArea clientLogTA;

  private SelectedProtocol selectedProtocol = SelectedProtocol.RANDOM;

  private Vector<Control> inputControls;
  private Vector<Control> initControls;

  public GuiController(Stage primaryStage) {
    tcpPort = 7080;
    udpPort = 7090;
    inputControls = new Vector<>();
    initControls  = new Vector<>();
    startGUI(primaryStage);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() { if(server != null) server.shutdown(); }
    });
    primaryStage.setOnCloseRequest(t -> {
      Platform.exit();
      System.exit(0);
    });
  }

  private void startGUI(Stage primaryStage) {
    stage = primaryStage;

    GridPane grid = new GridPane();

    grid.setAlignment(Pos.CENTER);
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10));

    Button toggle = new Button("Start Server");
    Button create = new Button("Create Clients");
    Button clear  = new Button("Clear Log");
    inputControls.add(create);
    toggle.setMinWidth(250);

    Label tcpPortL      = new Label("TCP Port");
    TextField tcpPortTF = new TextField("7080");
    Label udpPortL      = new Label("UDP Port");
    TextField udpPortTF = new TextField("7090");
    tcpPortTF.setPromptText("TCP Port");
    tcpPortTF.setMaxWidth(120);
    tcpPortTF.setAlignment(Pos.CENTER);
    udpPortTF.setPromptText("UDP Port");
    udpPortTF.setMaxWidth(120);
    udpPortTF.setAlignment(Pos.CENTER);
    initControls.add(tcpPortTF);
    initControls.add(udpPortTF);


    Label parallelClientsL      = new Label("Parallel Clients: ");
    TextField parallelClientsTF = new TextField("1");
    ToggleGroup protocolTypeTG  = new ToggleGroup();
    RadioButton tcpRB           = new RadioButton("TCP");
    RadioButton udpRB           = new RadioButton("UDP");
    RadioButton randomRB        = new RadioButton("Random");
    parallelClientsTF.setMaxWidth(70);
    parallelClientsTF.alignmentProperty().set(Pos.CENTER);
    tcpRB.setToggleGroup(protocolTypeTG);
    udpRB.setToggleGroup(protocolTypeTG);
    randomRB.setToggleGroup(protocolTypeTG);
    randomRB.setSelected(true);
    inputControls.add(parallelClientsTF);
    inputControls.add(tcpRB);
    inputControls.add(udpRB);
    inputControls.add(randomRB);


    Label serverLogL = new Label("Server Log");
    Label clientLogL = new Label("Client Log");

    serverLogTA = new TextArea();
    clientLogTA = new TextArea();
    adjustLogTextArea(serverLogTA);
    adjustLogTextArea(clientLogTA);

    grid.add(toggle,            0, 0, 2, 1);
    grid.add(create,            2, 0, 2, 1);
    grid.add(clear,             4, 0, 2, 1);


    grid.add(tcpPortL,          0, 1, 2, 1);
    grid.add(tcpPortTF,         2, 1, 2, 1);
    grid.add(udpPortL,          4, 1, 2, 1);
    grid.add(udpPortTF,         6, 1, 2, 1);

    grid.add(parallelClientsL,  0, 2, 2, 1);
    grid.add(parallelClientsTF, 2, 2, 1, 1);
    grid.add(tcpRB,             4, 2, 1, 1);
    grid.add(udpRB,             5, 2, 1, 1);
    grid.add(randomRB,          6, 2, 1, 1);

    grid.add(serverLogL,        0, 4, 4, 1);
    grid.add(clientLogL,        4, 4, 4, 1);

    grid.add(serverLogTA,       0, 5, 4, 40);
    grid.add(clientLogTA,       4, 5, 4, 40);

    inputControls.forEach(control -> control.setDisable(true));

    toggle.setOnAction((ActionEvent event) -> {
      if(server == null || server.getState() != Server.State.LISTENING ) {
        toggle.setText("Shutdown Server");
        server = new Server(tcpPort, udpPort, this);
        while(server.getState() == Server.State.CONNECTING) {}
        if(server.getState() == Server.State.LISTENING) {
          //          new Client(Client.Protocol.UDP, udpPort, this, 1, 2);
          inputControls.forEach(control -> control.setDisable(false));
          initControls.forEach(control -> control.setDisable(true));
        }
      } else {
        server.shutdown();
        toggle.setText("Start Server");
        inputControls.forEach(control -> control.setDisable(true));
        initControls.forEach(control -> control.setDisable(false));
      }
    });

    create.setOnAction((ActionEvent event) -> {
      int parallelClients = Integer.parseInt(parallelClientsTF.getText());
      Random rand = new Random();
      for(int i = 0; i < parallelClients; ++i) {
        int n1 = (rand.nextInt(4) + 1) * (i + 1);
        int n2 = (rand.nextInt(4) + 1) * (i + 1);
        switch(selectedProtocol) {
          case TCP:
            new Client(Client.Protocol.TCP, tcpPort, this, n1, n2);
            break;
          case UDP:
            new Client(Client.Protocol.UDP, udpPort, this, n1, n2);
            break;
          case RANDOM:
            if(rand.nextBoolean()) new Client(Client.Protocol.TCP, tcpPort, this, n1, n2);
            else                   new Client(Client.Protocol.UDP, udpPort, this, n1, n2);
        }
      }
    });

    clear.setOnAction((ActionEvent event) -> {
      serverLogTA.setText("");
      clientLogTA.setText("");
    });

    parallelClientsTF.textProperty().addListener((observable, oldValue, newValue) -> {
      if(newValue.length() > 2 || !containsOnlyNumbers(newValue)) {
        parallelClientsTF.setText(oldValue);
      }
    });

    tcpPortTF.textProperty().addListener((observable, oldValue, newValue) -> {
      if(newValue.length() > 5 || !containsOnlyNumbers(newValue)) {
        tcpPortTF.setText(oldValue);
      } else {
        this.tcpPort = Integer.parseInt(tcpPortTF.getText());
      }
    });

    udpPortTF.textProperty().addListener((observable, oldValue, newValue) -> {
      if(newValue.length() > 5 || !containsOnlyNumbers(newValue)) {
        udpPortTF.setText(oldValue);
        this.udpPort = Integer.parseInt(udpPortTF.getText());
      }
    });

    protocolTypeTG.selectedToggleProperty().addListener((ov, t, t1) -> {
      RadioButton chk = (RadioButton)t1.getToggleGroup().getSelectedToggle();
      if(chk.getText() == "TCP") {
        selectedProtocol = SelectedProtocol.TCP;
      } else if (chk.getText() == "UDP") {
        selectedProtocol = SelectedProtocol.UDP;
      } else {
        selectedProtocol = SelectedProtocol.RANDOM;
      }
    });

    Scene scene = new Scene(grid, width, height);
    primaryStage.setScene(scene);
    primaryStage.setMaximized(true);

    stage.setTitle("TCP & UDP | Ahmed Atef");
    stage.setWidth(width);
    stage.setHeight(height);
    stage.centerOnScreen();

    stage.show();

    FlatterFX.style();
    grid.requestFocus();

  }

  private void adjustLogTextArea(TextArea textArea) {
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setFont(new Font("System", 14));
  }



  public void serverLog(String log) {
    serverLogTA.appendText(log + "\n");
  }

  public void clientLog(String log) {
    clientLogTA.appendText(log + "\n");
  }

  private boolean containsOnlyNumbers(String s) {
    for(int i = 0; i < s.length(); ++i) {
      if(!(s.charAt(i) >= '0' && s.charAt(i) <= '9')) return false;
    }
    return true;
  }
}
