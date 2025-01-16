package com.linkedin.venice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.venice.controllerapi.ControllerClient;
import com.linkedin.venice.controllerapi.LogResponse;
import com.linkedin.venice.meta.StoreInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.lang3.tuple.Pair;


public class DebugCollector {
  ObjectMapper objectMapper = new ObjectMapper();

  public void collectLogs(
      String store,
      String cluster,
      String fabric,
      int version,
      String parentDir,
      ControllerClient controllerClient) throws IOException {
    if (cluster == null) {
      cluster = controllerClient.discoverCluster(store).getCluster(); // check if works??
    }

    String logDir = parentDir + "/" + fabric;

    StoreInfo storeInfo = controllerClient.getStore(store).getStore();
    LogResponse logResponse = new LogResponse();

    controllerClient.getLogs(store, -1);
    logResponse.addLog(Pair.of("/venice/" + cluster + "/Stores/" + store, objectMapper.writeValueAsString(storeInfo)));
    storeLogToFile(logResponse, logDir);

    logResponse = controllerClient.getLogs(store, version);
    storeLogToFile(logResponse, logDir);
  }

  private void storeLogToFile(LogResponse logResponse, String parentDir) throws IOException {
    for (Pair<String, String> logs: logResponse.getLog()) {
      String outPath = parentDir + logs.getKey() + "/__data__";
      new File(outPath).getParentFile().mkdirs();
      try (FileWriter writer = new FileWriter(outPath + "/__data__")) {
        writer.write(logs.getValue());
      }
    }
  }
}
