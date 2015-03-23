/*
 * Xibo - Digital Signage - http://www.xibo.org.uk
 * Copyright (C) 2015 Alex Harrington
 *
 * This file is part of Xibo.
 *
 * Xibo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version. 
 *
 * Xibo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Xibo.  If not, see <http://www.gnu.org/licenses/>.
 */ 

package displayclient;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Alex Harrington - Spring Signage Ltd
 */
public class DisplayClient extends Application {
    private final static Logger log = Logger.getLogger(DisplayClient.class.getName());

    /**
     * Properties object containing all app configuration
     */
    public final static Properties prop = new Properties();
    
    public DownloadManager DM;
    public XmdsxmdsBinding XMDS;
        
    @Override
    public void start(Stage primaryStage) {
        this.XMDS = new XmdsxmdsBinding(prop.getProperty("ServerUri") + "/xmds.php%3Fv=4");
        this.DM = new DownloadManager(this);
        this.DM.start();
        
        // Hide the Window Decorations
        primaryStage.initStyle(StageStyle.UNDECORATED);
        
        // Work out the size of the screen or area we're allocated
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getBounds();

        if (Integer.parseInt(prop.getProperty("SizeX")) == 0) {
            //set Stage boundaries to visible bounds of the main screen
            primaryStage.setX(primaryScreenBounds.getMinX());
            primaryStage.setY(primaryScreenBounds.getMinY());
            primaryStage.setWidth(primaryScreenBounds.getWidth());
            primaryStage.setHeight(primaryScreenBounds.getHeight());
        }
        else {
            // Size the primaryStage to the dimensions we're given
            primaryStage.setX(Integer.parseInt(prop.getProperty("OffsetX")));
            primaryStage.setY(Integer.parseInt(prop.getProperty("OffsetY")));
            primaryStage.setWidth(Integer.parseInt(prop.getProperty("SizeX")));
            primaryStage.setHeight(Integer.parseInt(prop.getProperty("SizeY")));
        }
        
        log.log( Level.INFO, "Screen Geometry ({0}x{1}) Offset ({2},{3})", new Object[]{primaryStage.getWidth(), primaryStage.getHeight(), primaryStage.getX(), primaryStage.getY()} );
        
        StackPane root = new StackPane();
        root.getChildren().add(
                        new ImagePane("/displayclient/assets/splash.jpg", "-fx-background-size: stretch; -fx-background-repeat: no-repeat;")
        );
        
        Scene scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());
        
        primaryStage.setTitle("Display Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //load a properties file from class path, inside static method
            prop.load(DisplayClient.class.getClassLoader().getResourceAsStream("site.properties"));
        } 
        catch (IOException ex) {
            ex.printStackTrace();
        }
        
        log.addHandler(new ConsoleHandler());
        log.setLevel(Level.ALL);
              
        launch(args);
    }
    
}
