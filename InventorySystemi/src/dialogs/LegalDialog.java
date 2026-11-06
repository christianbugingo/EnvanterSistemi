package dialogs;
import gui.*;
import services.LegalService;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
public final class LegalDialog {
    private LegalDialog() { }
    public static void view(){
        Dialog<Void> d=new Dialog<>();d.setTitle(LocaleManager.get("legal.full"));Ui.style(d.getDialogPane());TabPane tabs=new TabPane();
        for(String file:new String[]{"TERMS.md","PRIVACY.md","THIRD-PARTY-NOTICES.txt"}){Tab tab=new Tab(LocaleManager.get(file.equals("TERMS.md")?"legal.terms":file.equals("PRIVACY.md")?"legal.privacy":"legal.notices"),LegalText.render(LegalService.text(file)));tab.setClosable(false);tabs.getTabs().add(tab);}
        tabs.setPrefSize(800,550);d.getDialogPane().setContent(tabs);d.getDialogPane().getButtonTypes().add(new ButtonType(LocaleManager.get("button.close"),ButtonBar.ButtonData.CANCEL_CLOSE));d.showAndWait();
    }
    public static boolean requireAcceptance(){
        if(LegalService.accepted())return true;
        Dialog<Boolean> d=new Dialog<>();d.setTitle(LocaleManager.get("legal.title"));Ui.style(d.getDialogPane());
        Label summary=new Label(LocaleManager.get("legal.summary"));summary.setWrapText(true);summary.setMaxWidth(620);
        CheckBox agree=new CheckBox(LocaleManager.get("legal.agree"));agree.setId("termsAgree");
        VBox content=new VBox(20,summary,Ui.button("legal.full",LegalDialog::view),agree);content.setPadding(new Insets(25));d.getDialogPane().setContent(content);
        ButtonType accept=new ButtonType(LocaleManager.get("legal.accept"),ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().addAll(accept,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        var button=d.getDialogPane().lookupButton(accept);button.setId("acceptTerms");button.disableProperty().bind(agree.selectedProperty().not());
        button.addEventFilter(javafx.event.ActionEvent.ACTION,e->{if(!agree.isSelected()){e.consume();return;}try{LegalService.accept();}catch(RuntimeException ex){e.consume();Ui.error(ex.getMessage());}});
        d.setResultConverter(b->b==accept);return d.showAndWait().orElse(false)&&LegalService.accepted();
    }
}
