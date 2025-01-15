package com.linkedin.venice.controllerapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;


public class LogResponse extends ControllerResponse {
  private List<KeyValuePair> logs = new ArrayList<>();

  public List<KeyValuePair> getLog() {
    return logs;
  }

  public void setLog(List<KeyValuePair> logs) {
    this.logs = logs;
  }

  public void addLog(KeyValuePair log) {
    this.logs.add(log);
  }

  public static class KeyValuePair {
    private String key;
    private String value;

    @JsonCreator
    public KeyValuePair(@JsonProperty("key") String key, @JsonProperty("value") String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
