package gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import java.util.regex.Pattern;

/** Render the bundled documents locally, without a browser or visible Markdown syntax. */
public final class LegalText {
    private LegalText() { }
    public static ScrollPane render(String source) {
        VBox body=new VBox(10);body.setPadding(new Insets(24));body.setId("legalDocument");
        for(String line:source.split("\\R")) {
            if(line.isBlank())continue;
            var heading=Pattern.compile("^(#{1,6})\\s+(.+)$").matcher(line);
            TextFlow flow=new TextFlow();flow.setLineSpacing(4);
            if(heading.matches()) {
                Text title=new Text(heading.group(2).replace("**",""));title.setStyle("-fx-font-weight:bold; -fx-font-size:"+(heading.group(1).length()==1?24:18)+"px;");flow.getChildren().add(title);
            } else {
                var bold=Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(line);int position=0;
                while(bold.find()){flow.getChildren().add(new Text(line.substring(position,bold.start())));Text text=new Text(bold.group(1));text.setStyle("-fx-font-weight:bold;");flow.getChildren().add(text);position=bold.end();}
                flow.getChildren().add(new Text(line.substring(position)));
            }
            body.getChildren().add(flow);
        }
        ScrollPane scroll=new ScrollPane(body);scroll.setFitToWidth(true);scroll.setPrefViewportWidth(740);return scroll;
    }
}
