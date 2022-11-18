import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class NetDraw extends Application {

	private final static String[] TOOLS = {  // The commands that appear in the Tools menu.
			"Rectangle", "Oval", "RoundRect", "Filled Rectangle", "Filled Oval",
			"Filled RoundRect", "Line", "Text"   // "Stamp" tool is handled separately!
		};
	
	private final static String[] TOOL_SHORTCUTS = { // For defining accelerator keys for commands in the Tools menu.
			"shortcut+R", "shortcut+V", "shortcut+D", "shortcut+shift+R", "shortcut+shift+V", 
			"shortcut+shift+D", "shortcut+L", "shortcut+T"
		};
	
	private final static String[] STAMP_FILE_NAMES = {  // Resource file names for available stamps, which must be in a folder names "stamps".
			"bell.png", "bomb.png", "camera.png", "check.png", "flower.png",
			"smiley.png", "star.png", "tux.png", "TV.png", "x.png"
		};
	
	private final static int WIDTH = 1000;  // Width of the Canvas that is used for drawing.
	private final static int HEIGHT = 600;  // Height of the Canvas that is used for drawing.

	private static GraphicsContext imageGraphics;    // For drawing on the actual image.
	private GraphicsContext overlayGraphics;  // For drawing on a transparent Canvas that overlays the image; used when drawing shapes.

	private String currentTool = "Stamp";  // The current tool, either "Stamp" if a stamp has been selected, or a command from the Tools menu.
	private Image[] stampImages;  // The images that are drawn when the current tool is "Stamp", one for each item in the "Stamp" menu.
	private Color currentColor = Color.BLACK;  // Used for all tools except "Stamp".
	private int currentStampNumber = 7;  // Used when the currentTool is "Stamp".
	
	private TextField textInput;   // Input for the text that will be drawn when the user clicks the canvas using the "Text" tool.
	private ComboBox<Integer> textSizeSelect;   // Pop-up menu for selecting the size of the font that is used for drawing the text.
	private ComboBox<Integer> lineWidthSelect;  // Pop-up menu for selecting the width of lines and outlines of shapes.
	
	private Label message;  // Message that appears below the canvas, for showing status information about the program.
	
	public static GraphicsContext getIGC() {
		return imageGraphics;
	}
	
	/**
	 * Set up the GUI and event handling.
	 */
	public void start(Stage stage) {
		BorderPane root = new BorderPane();

		Canvas picture = new Canvas(WIDTH,HEIGHT);
		imageGraphics = picture.getGraphicsContext2D();
		imageGraphics.setLineWidth(2);
		imageGraphics.setFill(Color.WHITE);
		imageGraphics.fillRect(0,0,WIDTH,HEIGHT);
		Canvas overlay = new Canvas(WIDTH,HEIGHT);
		overlayGraphics = overlay.getGraphicsContext2D();
		overlayGraphics.setLineWidth(2);
		StackPane canvasHolder = new StackPane(picture,overlay);
		root.setCenter(canvasHolder);

		MenuBar menubar = makeMenus();
		root.setTop(menubar);
		
		textInput = new TextField("Hello World");
		textInput.setPrefColumnCount(20);
		lineWidthSelect = new ComboBox<>();
		lineWidthSelect.getItems().addAll(0,1,2,3,4,5,7,10,12,15);
		lineWidthSelect.getSelectionModel().select(3);
		textSizeSelect = new ComboBox<>();
		textSizeSelect.getItems().addAll(12,18,24,30,36,48,60);
		textSizeSelect.getSelectionModel().select(3);
		message = new Label("Welcome to NetDraw! Current tool is Stamp (tux.png)");
		message.setMaxWidth(10000);
		message.setFont(Font.font(18));
		message.setStyle("-fx-padding:5px; -fx-border-color: black;"
				+ " -fx-background-color:white; -fx-border-width: 1 0 0 0");
		
		HBox controls = new HBox(20, new Label("Text:"), textInput, new Label("Text Size:"),
				textSizeSelect, new Label("Line Width:"), lineWidthSelect);
		controls.setStyle("-fx-background-color: lightgray; -fx-padding:5px");
		VBox bottom = new VBox(controls,message);
		bottom.setAlignment(Pos.CENTER);
		root.setBottom(bottom);
		bottom.setStyle("-fx-border-color:black");

		overlay.setOnMousePressed( this::mousePressed );
		overlay.setOnMouseDragged( this::mouseDragged );
		overlay.setOnMouseReleased( this::mouseReleased );

		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("NetDraw");
		stage.show();
	}
	
	
	public static void main(String[] args) {
		launch();
	}

	
	/**
	 * Create the menu bar for the window, containing "Control", "Color", "Tool" and "Stamp" menus.
	 * All the event handlers for responding to menu commands are also defined here.
	 */
	private MenuBar makeMenus() {
		
		Menu controlMenu = new Menu("Control");
		MenuItem clearItem = new MenuItem("Clear");
		MenuItem connect = new MenuItem("Connect");
		clearItem.setOnAction( e -> {
			imageGraphics.setFill(Color.WHITE);
			imageGraphics.fillRect(0, 0, WIDTH, HEIGHT);
		});
		connect.setOnAction(e -> {
			NetReader connection = new NetReader("localhost");
			connection.run();
		});
		clearItem.setAccelerator(KeyCombination.keyCombination("shortcut+N"));
		controlMenu.getItems().add(clearItem);
		controlMenu.getItems().add(connect);
		
		Menu colorMenu = new Menu("Color");
		MenuItem item;
		item = new MenuItem("Black");
		item.setOnAction( e -> currentColor = Color.BLACK );
		colorMenu.getItems().add(item);
		item = new MenuItem("Red");
		item.setOnAction( e -> currentColor = Color.RED );
		colorMenu.getItems().add(item);
		item = new MenuItem("Green");
		item.setOnAction( e -> currentColor = Color.GREEN );
		colorMenu.getItems().add(item);
		item = new MenuItem("Blue");
		item.setOnAction( e -> currentColor = Color.BLUE );
		colorMenu.getItems().add(item);
		item = new MenuItem("Custom Color...");
		item.setAccelerator(KeyCombination.keyCombination("shortcut+K"));
		colorMenu.getItems().add(item);
		item.setOnAction( e -> {
			Color c = SimpleDialogs.colorChooser(currentColor);
			if ( c != null )
				currentColor = c;
		});
		
		EventHandler<ActionEvent> toolListener = e -> {
			MenuItem target = (MenuItem)e.getTarget();
			currentTool = target.getText();
			message.setText("Current tool is " + currentTool);
		};
		Menu toolMenu = new Menu("Tool");
		for ( int i = 0; i < TOOLS.length; i++) {
			MenuItem toolItem = new MenuItem(TOOLS[i]);
			toolItem.setOnAction(toolListener);
			toolItem.setAccelerator(KeyCombination.keyCombination(TOOL_SHORTCUTS[i]));
			toolMenu.getItems().add(toolItem);
		}

		Menu stampMenu = new Menu("Stamp");
		stampImages = new Image[STAMP_FILE_NAMES.length];
		for (int i = 0; i < STAMP_FILE_NAMES.length; i++) {
			final int stampNumber = i;  // final, for use in lambda expression.
			stampImages[i] = new Image("stamps/" + STAMP_FILE_NAMES[i]);
			MenuItem stampItem = new MenuItem();
			stampItem.setGraphic( new ImageView(stampImages[i]) ); 
			stampItem.setOnAction( e -> {
				currentTool = "Stamp";
				currentStampNumber = stampNumber;
				message.setText("Current tool is Stamp (" + STAMP_FILE_NAMES[stampNumber] + ")");
			});
			stampMenu.getItems().add(stampItem);
			stampItem.setAccelerator(KeyCombination.keyCombination("shortcut+" + i));
		}
		
		MenuBar menubar = new MenuBar(controlMenu, colorMenu, toolMenu, stampMenu);
		return menubar;
		
	} // end makeMenus();
	

	/**
	 * Draws a shape in a specified graphics context.  This method is not used for text or stamps.
	 * @param shape  The shape to be drawn.  One of the strings in the TOOLS array, which also appear in the "Tool" menu. 
	 * @param color  The color for the shape.  For filled shapes, this is the fill color, and any outline for the shape is drawn in black.
	 * @param lineWidth  The stroke width for the "Line" tool, for unfilled shapes, and for the black outline around filled shapes.
	 *           For a filled shape, the outline is only drawn when lineWidth is greater than zero.
	 * @param g  The graphics context where where the shape will be drawn.  This will be overlayGraphics while the user is dragging
	 *           the mouse to create a shape.  It will be imageGraphics when the drag action ends and the shape is added to the actual image.
	 * @param x1 x-coordinate of one endpoint or corner for the shape
	 * @param y1 y-coordinate of one endpoint or corner for the shape
	 * @param x2 x-coordinate of the other endpoint or corner for the shape
	 * @param y2 y-coordinate of the other endpoint or corner for the shape
	 */
	public static void drawShape(String shape,  Color color, double lineWidth, GraphicsContext g, 
			double x1, double y1, double x2, double y2) {
		g.setStroke(color);
		g.setLineWidth( lineWidth );
		if (shape.equals("Line")) {
			g.strokeLine(x1,y1,x2,y2);
			return;
		}
		double left = Math.min(x1,x2);
		double top = Math.min(y1,y2);
		double width = Math.abs( x1 - x2 );
		double height = Math.abs( y1 - y2 );
		g.setFill(color);
		switch (shape) {
		case "Rectangle":
			g.strokeRect(left,top,width,height);
			break;
		case "RoundRect":
			g.strokeRoundRect(left,top,width,height,width/4,height/4);
			break;
		case "Oval":
			g.strokeOval(left,top,width,height);
			break;
		case "Filled Rectangle":
			g.fillRect(left,top,width,height);
			if (lineWidth > 0) {
				g.setStroke(Color.BLACK);
				g.strokeRect(left, top, width, height);
			}
			break;
		case "Filled RoundRect":
			g.fillRoundRect(left,top,width,height,width/4,height/4);
			if (lineWidth > 0) {
				g.setStroke(Color.BLACK);
				g.strokeRoundRect(left,top,width,height,width/4,height/4);
			}
			break;
		case "Filled Oval":
			g.fillOval(left,top,width,height);
			if (lineWidth > 0) {
				g.setStroke(Color.BLACK);
				g.strokeOval(left,top,width,height);
			}
			break;
		}
	}
	
	
	//--------------------------- Implementing mouse interaction ---------------------------------------------

	private double startX, startY;  // Records the point where a drag operation begins.
	private double endX, endY;  // Records the current mouse location during a drag operation.
	private boolean dragging;  // Set to true when a drag operation is in progress.  Not all mousePressed events begin a drag action.

	/**
	 * Respond when the user presses a mouse button while the mouse is over the canvas.  If the currentTool is "Stamp"
	 * or "Text", the image or text is drawn directly to imageGraphics.  If it is one of the other tools, a drag operation
	 * is started that allows the user to set the size of the shape by dragging the mouse.
	 */
	private void mousePressed(MouseEvent evt) {
		if (dragging) {  // This will happen if the user presses a second mouse button while dragging; ignore the second mouse press event.
			return;
		}
		if (currentTool.equals("Text")) {  // Draw the text to the actual image.
			String text = textInput.getText().trim();
			if (text.length() == 0)
				message.setText("PLEASE ENTER SOME TEXT IN THE INPUT BOX!!");
			else {
				Font f = Font.font( textSizeSelect.getValue() );
				imageGraphics.setFont(f);
				imageGraphics.setFill(currentColor);
				imageGraphics.fillText(text, evt.getX(), evt.getY());
				NetReader nr = new NetReader("localhost");
				String outgoing = "text " + evt.getX()+ " " + evt.getY()
				+ " " + textSizeSelect.getValue() + " " + currentColor.getRed() +
				" " + currentColor.getGreen() + " " + currentColor.getBlue() + " " + text.toString();
				nr.send(outgoing);
			}		
			dragging = false;
		}
		else if (currentTool.equals("Stamp")) {  // Draw the stamp to the actual image.
			Image stamp = stampImages[currentStampNumber];
			imageGraphics.drawImage( stamp, evt.getX() - stamp.getWidth()/2, evt.getY()-stamp.getHeight()/2 );
			dragging = false;
			NetReader nr = new NetReader("localhost");
			String outgoing = "stamp " + evt.getX()+ " " + evt.getY() + " " + STAMP_FILE_NAMES[currentStampNumber];
			nr.send(outgoing);
		}
		else {  // Start a drag operation.
			startX = endX = evt.getX();
			startY = endY = evt.getY();
			dragging = true;
		}
	}

	/**
	 *  Respond to a mouseDragged event.  If the user is drawing a shape by dragging, this method draws the
	 *  shape temporarily to the overlay canvas, so that the actual image is not affected while the drag is
	 *  in progress.  The two points that determine the shape are the mouse location where the drag action 
	 *  started, which is saved in the variables startX and startY, and the current mouse location.
	 */
	private void mouseDragged(MouseEvent evt) {
		if (!dragging) {  // Ignore a mouseDragged event that is not part of a drag operation in this program.
			return;
		}
		endX = evt.getX();
		endY = evt.getY();
		overlayGraphics.clearRect(0,0,WIDTH,HEIGHT);  // Clear any previous shape in the overlay canvas that was drawn while dragging.
		double lineWidth = lineWidthSelect.getValue();
		if (lineWidth == 0 && (currentTool.equals("Rectangle") || currentTool.equals("Oval") || currentTool.equals("RoundRect")) ) {
			lineWidth = 1;
		}
		drawShape(currentTool,currentColor,lineWidth,overlayGraphics,startX,startY,endX,endY);  // draw shape to overlay canvas
	}

	/**
	 * Respond to a mouseReleased event.  If this is the end of a drag action with which the user was drawing a 
	 * shape, the shape is now drawn permanently to the actual canvas, and the overlay canvas is cleared.
	 */
	private void mouseReleased(MouseEvent evt) {
		if (!dragging) {
			return;
		}
		dragging = false;
		overlayGraphics.clearRect(0,0,WIDTH,HEIGHT);
		double lineWidth = lineWidthSelect.getValue();
		if (lineWidth == 0 && (currentTool.equals("Rectangle") || currentTool.equals("Oval") || currentTool.equals("RoundRect")) ) {
			lineWidth = 1;
		}
		drawShape(currentTool,currentColor,lineWidth,imageGraphics,startX,startY,endX,endY); // draw shape to actual image
		String outgoing = "";
		NetReader nr = new NetReader("localhost");
		if (currentTool.equals("Rectangle")) {
			outgoing = "rect " + startX+ " " + startY + " " + (endX-startX)+ " " +(endY-startY)
					+ " " + currentColor.getRed() + " " + currentColor.getGreen() + " " + lineWidth;
			nr.send(outgoing);
		}
		else if (currentTool.equals("Oval")) {
			outgoing = "oval " + startX+ " " + startY + " " + (endX-startX)+ " " +(endY-startY)
					+ " " + currentColor.getRed() + " " + currentColor.getGreen() + " " + lineWidth;
			nr.send(outgoing);
		}
		else if (currentTool.equals("RoundRect")) {
			outgoing = "roundrect " + startX+ " " + startY + " " + (endX-startX)+ " " +(endY-startY)
					+ " " + currentColor.getRed() + " " + currentColor.getGreen() + " " + lineWidth;
			nr.send(outgoing);
		}
	}
	
	//----------------------------------------------------------------------------------------------------------------------

}

class NetReader extends Thread {
    String host;
    PrintWriter out = new PrintWriter(System.out);
    public final String HWS_IP = "172.21.7.12";
    NetReader(String host) {
        this.host = host;
        setDaemon(true);
    }
    public PrintWriter getPW() {
    	return out;
    }
    
    public void send(String send) {
    	Socket socket = new Socket();
		SocketAddress address = new InetSocketAddress(HWS_IP, 35053);
		Scanner in;
		try {
			socket.connect(address);
			in = new Scanner(socket.getInputStream());
			System.out.println("sub test 2");
			System.out.println("TEST 4");
			OutputStream os = socket.getOutputStream();
			PrintWriter out = new PrintWriter(os, true);
			out.println("NetDraw");
			out.flush();
			if (in.nextLine().equals("NetDraw")) {
				out.println(send);
				out.flush();
			}
			socket.close();
			in.close();
			
		} catch (IOException e) {
			System.out.println("Error");
			e.printStackTrace();
		}
    }
    
    public void run() {
    	System.out.println("TEST 1");
    	Scanner in = new Scanner(System.in);
    	String server = SimpleDialogs.prompt("Enter a server to connect to.");
    	if (server.equalsIgnoreCase("hws")) {server = HWS_IP;}
    	System.out.println("TEST 2");
    	try {
    		while (true) {
    			System.out.println("TEST 3");
    			System.out.println("sub test 1");
    			Socket socket = new Socket();
    			SocketAddress address = new InetSocketAddress(server, 35053);
    			socket.connect(address);
    			System.out.println("sub test 2");
    			System.out.println("TEST 4");
    			OutputStream os = socket.getOutputStream();
    			PrintWriter out = new PrintWriter(os, true);
    			out.println("NetDraw");
    			out.flush();
    			System.out.println("TEST 5");
    			//172.21.7.12
    			in = new Scanner(socket.getInputStream());
    			String protocolIn;
    			if (in.hasNext()) {
    				protocolIn = in.nextLine();
    				System.out.println(protocolIn);
    				if (protocolIn.equals("NetDraw")) {
    					while (true) {
    						System.out.println("--1");
    						if (in.hasNext()) {
    							System.out.println("--2");
    							in.nextLine();
    							if (in.hasNext("text")) {
    								in.next();
    								System.out.println("--3 Text");
    								double xCoor = in.nextDouble();
    								double yCoor = in.nextDouble();
    								double size = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								String textLine = in.next();
    								Font font = Font.font(size);
    								Text text = new Text(textLine);
    						        text.setFont(font);
    						        text.setX(xCoor);
    						        text.setY(yCoor);
    						        text.setFill(Color.rgb((int)red*100, (int)green*100, (int)blue*100));
    						        NetDraw.getIGC().fillText(textLine, xCoor, yCoor);
    							}
    							else if (in.hasNext("stamp")) {
    								in.next();
    								System.out.println("--3 Stamp");
    								double xCoor = in.nextDouble();
    								double yCoor = in.nextDouble();
    								String filename = in.next();
    								System.out.println(filename);
    								Image stamp = new Image(filename);
    								NetDraw.getIGC().drawImage( stamp, xCoor, yCoor );
    							}
    							else if (in.hasNext("line")) {
    								in.next();
    								System.out.println("--3 Line");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double xCoor2 = in.nextDouble();
    								double yCoor2 = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								Line line = new Line();
    								line.setEndX(xCoor2);
    								line.setEndY(yCoor2);
    								line.setStartX(xCoor1);
    								line.setStartY(yCoor1);
    								line.setFill(Color.rgb((int)red*100, (int)green*100, (int)blue*100));
    						        NetDraw.getIGC().lineTo(xCoor2, yCoor2);
    							}
    							else if (in.hasNext("rect")) {
    								in.next();
    								System.out.println("--3 rect");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double width = in.nextDouble();
    								double height = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								double lineWidth = in.nextDouble();
    						        NetDraw.drawShape("Rectangle", Color.rgb((int)red*100, (int)green*100, (int)blue*100), 
    						        		lineWidth, NetDraw.getIGC(), xCoor1, yCoor1, xCoor1+width, yCoor1+height);
    							}
    							else if (in.hasNext("oval")) {
    								in.next();
    								System.out.println("--3 oval");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double width = in.nextDouble();
    								double height = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								double lineWidth = in.nextDouble();
    						        NetDraw.drawShape("Oval", Color.rgb((int)red*100, (int)green*100, (int)blue*100), 
    						        		lineWidth, NetDraw.getIGC(), xCoor1, yCoor1, xCoor1+width, yCoor1+height);
    							}
    							else if (in.hasNext("roundrect")) {
    								in.next();
    								System.out.println("--3 roundrect");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double width = in.nextDouble();
    								double height = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								double lineWidth = in.nextDouble();
    						        NetDraw.drawShape("RoundRect", Color.rgb((int)red*100, (int)green*100, (int)blue*100), 
    						        		lineWidth, NetDraw.getIGC(), xCoor1, yCoor1, xCoor1+width, yCoor1+height);
    							}
    							else if (in.hasNext("filledoval")) {
    								in.next();
    								System.out.println("--3 filledoval");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double width = in.nextDouble();
    								double height = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								double lineWidth = in.nextDouble();
    						        NetDraw.drawShape("Filled Oval", Color.rgb((int)red*100, (int)green*100, (int)blue*100), 
    						        		lineWidth, NetDraw.getIGC(), xCoor1, yCoor1, xCoor1+width, yCoor1+height);
    							}
    							else if (in.hasNext("filledroundrect")) {
    								in.next();
    								System.out.println("--3 rect");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double width = in.nextDouble();
    								double height = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								double lineWidth = in.nextDouble();
    						        NetDraw.drawShape("Filled RoundRect", Color.rgb((int)red*100, (int)green*100, (int)blue*100), 
    						        		lineWidth, NetDraw.getIGC(), xCoor1, yCoor1, xCoor1+width, yCoor1+height);
    							}
    							else if (in.hasNext("filledrect")) {
    								in.next();
    								System.out.println("--3 filledrect");
    								double xCoor1 = in.nextDouble();
    								double yCoor1 = in.nextDouble();
    								double width = in.nextDouble();
    								double height = in.nextDouble();
    								double red = in.nextDouble();
    								double green = in.nextDouble();
    								double blue = in.nextDouble();
    								double lineWidth = in.nextDouble();
    						        NetDraw.drawShape("Filled Rectangle", Color.rgb((int)red*100, (int)green*100, (int)blue*100), 
    						        		lineWidth, NetDraw.getIGC(), xCoor1, yCoor1, xCoor1+width, yCoor1+height);
    							}
    						}
    					}
    				}
    			}
    			else {
    				protocolIn = "Could not Connect";
    				socket.close();
    				in.close();
    				break;
    			}
    			System.out.println("TEST 6");
    			System.out.println(protocolIn);
    			out.close();
    			socket.close();
    		}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    
    
}


