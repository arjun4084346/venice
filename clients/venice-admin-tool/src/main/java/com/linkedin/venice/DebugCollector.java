package com.linkedin.venice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.linkedin.venice.controllerapi.ControllerClient;
import com.linkedin.venice.controllerapi.LogResponse;
import com.linkedin.venice.meta.StoreInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class DebugCollector {
  ObjectMapper objectMapper = new ObjectMapper();
  ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();

  public void collectLogs(
      String store,
      String cluster,
      int version,
      String parentDir,
      ControllerClient controllerClient) throws IOException {
    if (cluster == null) {
      cluster = controllerClient.discoverCluster(store).getCluster(); // check if works??
    }

    String logDir = parentDir + "/test";

    StoreInfo storeInfo = controllerClient.getStore(store).getStore();
    LogResponse logResponse = new LogResponse();

    controllerClient.getLogs(store, -1);
    logResponse.addLog(
        new LogResponse.KeyValuePair("/venice/" + cluster + "/Stores/" + store, writer.writeValueAsString(storeInfo)));
    storeLogToFile(logResponse, logDir);

    logResponse = controllerClient.getLogs(store, version);
    storeLogToFile(logResponse, logDir);
  }

  private void storeLogToFile(LogResponse logResponse, String parentDir) throws IOException {
    for (LogResponse.KeyValuePair logs: logResponse.getLog()) {
      String outPath = parentDir + logs.getKey() + "/__data__";
      new File(outPath).getParentFile().mkdirs();
      try (FileWriter writer = new FileWriter(outPath, true)) {
        writer.write(logs.getValue());
      }
    }
  }
}
