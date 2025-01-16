package com.linkedin.venice.controllerapi;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;


public class LogResponse extends ControllerResponse {
  private List<Pair<String, String>> logs = new ArrayList<>();

  public List<Pair<String, String>> getLog() {
    return logs;
  }

  public void setLog(List<Pair<String, String>> logs) {
    this.logs = logs;
  }

  public void addLog(Pair<String, String> log) {
    this.logs.add(log);
  }
}
