package dialogs;
import gui.*;
public final class ConfirmDialog {
    public static boolean show(String title,String header,String content,Object... args) { return Ui.confirm(LocaleManager.get(content,args)); }
    public static boolean showSimple(String title,String content,Object... args) { return Ui.confirm(LocaleManager.get(content,args)); }
}
