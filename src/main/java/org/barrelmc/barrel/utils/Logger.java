package org.barrelmc.barrel.utils;
import org.barrelmc.barrel.utils.nukkit.TextFormat;
public class Logger{
  private String prefix;
  Logger(String prefix){
    if(prefix != null){
    this.prefix = prefix;
    }else{
      this.prefix = "§6BarrelMC";
    }
  }
  public Logger getLogger(){
    return this;
  }
  public String getPrefix(){
    return this.prefix;
  }
  public void emergency(String message){
    System.out.println(TextFormat.colorize('§', "[§cEMERGENCY§r] ["+ this.prefix +"§r] "+message));
  }
  public void emergency(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§cEMERGENCY§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void info(String message){
    System.out.println(TextFormat.colorize('§', "[§aINFO§r] ["+ this.prefix +"§r] "+message));
  }
  public void info(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§aINFO§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void alert(String message){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
  }
  public void alert(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void critical(String message){
    System.out.println(TextFormat.colorize('§', "[§4FATAL§r] ["+ this.prefix +"§r] "+message));
  }
  public void critical(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§4FATAL§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void error(String message){
    System.out.println(TextFormat.colorize('§', "[§cERROR§r] ["+ this.prefix +"§r] "+message));
  }
  public void error(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§cERROR§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void warning(String message){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
  }
  public void warning(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void notice(String message){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
  }
  public void notice(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void debug(String message){
    System.out.println(TextFormat.colorize('§', "[DEBUG] ["+ this.prefix +"§r] "+message));
  }
  public void debug(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[DEBUG] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
  public void logException(Throwable er){
    er.printStackTrace();
  }
  public void log(String message){
    System.out.println(TextFormat.colorize('§', message));
  }
  public void log(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', message));
    er.printStackTrace();
  }
  public void warn(String message){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
  }
  public void warn(String message, Throwable er){
    System.out.println(TextFormat.colorize('§', "[§eWARN§r] ["+ this.prefix +"§r] "+message));
    er.printStackTrace();
  }
}
