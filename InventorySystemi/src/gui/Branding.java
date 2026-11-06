package gui;

import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import java.util.*;

/** Vector artwork at every UI size; Stage's Image-only API receives in-memory rasterizations. */
public final class Branding {
    private static List<Image> icons;
    private Branding() { }
    private static Group artwork(){
        Rectangle background=new Rectangle(128,128,Color.web("#163347"));background.setArcWidth(28);background.setArcHeight(28);
        SVGPath outline=new SVGPath();outline.setContent("M64 16 L105 40 L105 88 L64 112 L23 88 L23 40 Z");outline.setFill(Color.TRANSPARENT);outline.setStroke(Color.web("#339ad5"));outline.setStrokeWidth(6);outline.setStrokeLineJoin(StrokeLineJoin.ROUND);
        SVGPath mark=new SVGPath();mark.setContent("M42 90 L42 53 L64 66 L86 53 L86 90 L74 97 L74 74 L64 80 L54 74 L54 97 Z");mark.setFill(Color.web("#ff7928"));
        Circle person=new Circle(64,43,8,Color.web("#ff7928"));return new Group(background,outline,mark,person);
    }
    public static StackPane logo(int size){
        Group art=artwork();art.getTransforms().add(new Scale(size/128.0,size/128.0));StackPane view=new StackPane(new Group(art));view.setMinSize(size,size);view.setPrefSize(size,size);view.setMaxSize(size,size);view.setAccessibleText("Inventory System");return view;
    }
    public static void apply(Stage stage){
        if(icons==null){List<Image> result=new ArrayList<>();for(int size:new int[]{16,32,64,128,256}){var node=logo(size);new Scene(node,size,size);node.resize(size,size);node.applyCss();node.layout();SnapshotParameters p=new SnapshotParameters();p.setFill(Color.TRANSPARENT);result.add(node.snapshot(p,null));}icons=List.copyOf(result);}
        stage.getIcons().setAll(icons);
    }
}
